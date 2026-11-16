package com.app.aparatdownloader.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.app.aparatdownloader.data.api.ApiClient
import com.app.aparatdownloader.data.api.resolveVideoUrl
import com.app.aparatdownloader.security.KeyStoreManager
import com.app.aparatdownloader.security.VideoCrypto
import com.app.aparatdownloader.ui.common.LoadState
import com.app.aparatdownloader.ui.common.apiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import javax.crypto.Cipher
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import java.io.RandomAccessFile


fun enqueueDownload(ctx: Context, id: String, title: String, course: String? = null, idx: Int = 0) {
    val vd = File(ctx.filesDir, "videos").apply { mkdirs() }
    if (course != null) File(vd, "$id.course").writeText("$idx\n$course")
    val req = OneTimeWorkRequestBuilder<VideoDownloadWorker>()
        .setInputData(workDataOf("video_id" to id, "title" to title))
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.LINEAR, 10, java.util.concurrent.TimeUnit.SECONDS)
        .addTag("dl").addTag("vid:$id").addTag("title:$title").build()
    WorkManager.getInstance(ctx).enqueueUniqueWork("dl_$id", ExistingWorkPolicy.KEEP, req)
}

/** توقف: فایل نیمه‌کاره حفظ می‌شود و با ادامه از همان نقطه پیش می‌رود */
fun pauseDownload(ctx: Context, id: String) { WorkManager.getInstance(ctx).cancelUniqueWork("dl_$id") }

fun cancelDownload(ctx: Context, id: String) {
    WorkManager.getInstance(ctx).cancelUniqueWork("dl_$id")
    val dir = File(ctx.filesDir, "videos")
    File(dir, "$id.part").delete()
    if (!File(dir, "$id.enc").exists()) { File(dir, "$id.title").delete(); File(dir, "$id.course").delete() }
}

class VideoDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object { private val gate = Semaphore(1) }

    private fun fgInfo(title: String, pct: Int): ForegroundInfo {
        val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) nm.createNotificationChannel(NotificationChannel("dl", "دانلود", NotificationManager.IMPORTANCE_LOW))
        val n = NotificationCompat.Builder(applicationContext, "dl")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(title).setContentText("$pct%")
            .setProgress(100, pct, false).setOngoing(true).setOnlyAlertOnce(true).build()
        val nid = id.hashCode()
        return if (Build.VERSION.SDK_INT >= 29) ForegroundInfo(nid, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        else ForegroundInfo(nid, n)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val vid = inputData.getString("video_id") ?: return@withContext Result.failure()
        val title = inputData.getString("title") ?: vid
        val dir = File(applicationContext.filesDir, "videos").apply { mkdirs() }
        val part = File(dir, "$vid.part")
        try {
            gate.withPermit { download(vid, title, dir, part) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (runAttemptCount < 30) Result.retry() else Result.failure(workDataOf("err" to "دانلود ناموفق بود"))
        }
    }

    private suspend fun download(vid: String, title: String, dir: File, part: File): Result {
        val store = KeyStoreManager(applicationContext)
        val api = ApiClient.create { store.getToken() }
        val resumed = part.exists() && part.length() >= 16
        File(dir, "$vid.title").writeText(title)
        try { setForeground(fgInfo(title, 0)) } catch (_: Exception) {}

        if (!resumed) {
            // هر کاربر فقط یک بار اجازه دانلود هر ویدیو را دارد
            when (val g = apiCall { api.downloadStart(vid) }) {
                is LoadState.Error -> return if (g.network) Result.retry()
                    else Result.failure(workDataOf("err" to g.message))
                else -> {}
            }
        }
        val key = (apiCall { api.getVideoKey(vid) } as? LoadState.Success)?.data?.key
            ?: return Result.retry()
        val url = (resolveVideoUrl(api, vid) as? LoadState.Success)?.data
            ?: return Result.retry()

        val iv = ByteArray(16)
        var offset = 0L
        if (resumed) {
            RandomAccessFile(part, "r").use { it.readFully(iv) }
            offset = part.length() - 16
        } else SecureRandom().nextBytes(iv)

        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 20000; conn.readTimeout = 30000
        conn.setRequestProperty("Referer", "https://www.aparat.com/")
        conn.setRequestProperty("User-Agent", "Mozilla/5.0")
        if (offset > 0) conn.setRequestProperty("Range", "bytes=$offset-")
        val code = conn.responseCode
        if (code !in 200..299) throw java.io.IOException("HTTP $code")
        val cl = conn.contentLengthLong
        val total = if (code == 206) offset + cl else cl
        val cipher = VideoCrypto.cipherAt(VideoCrypto.keyBytes(key), iv, offset, Cipher.ENCRYPT_MODE)

        var done = offset
        var lastT = System.currentTimeMillis()
        var lastBytes = done
        conn.inputStream.use { input ->
            if (offset > 0 && code == 200) { var sk = offset; while (sk > 0) { val k = input.skip(sk); if (k <= 0) break; sk -= k } }
            FileOutputStream(part, true).use { fos ->
                if (!resumed) fos.write(iv)
                val buf = ByteArray(64 * 1024)
                while (true) {
                    if (isStopped) return Result.failure()
                    val n = input.read(buf)
                    if (n < 0) break
                    cipher.update(buf, 0, n, buf, 0)
                    fos.write(buf, 0, n)
                    done += n
                    val now = System.currentTimeMillis()
                    if (now - lastT >= 700) {
                        val speed = (done - lastBytes) * 1000 / (now - lastT)
                        lastT = now; lastBytes = done
                        val pct = if (total > 0) (done * 100 / total).toInt() else 0
                        setProgress(workDataOf("p" to pct, "done" to done, "total" to total, "speed" to speed))
                        try { setForeground(fgInfo(title, pct)) } catch (_: Exception) {}
                    }
                }
            }
        }
        val out = File(dir, "$vid.enc")
        out.delete()
        part.renameTo(out)
        store.saveVideoKey(vid, key)
        for (i in 0 until 3) { if (apiCall { api.downloadDone(vid) } is LoadState.Success) break }
        return Result.success()
    }
}

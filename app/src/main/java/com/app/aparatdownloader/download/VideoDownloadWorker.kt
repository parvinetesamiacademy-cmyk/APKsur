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

class VideoDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val id = inputData.getString("video_id") ?: return@withContext Result.failure()
        val title = inputData.getString("title") ?: id
        val dir = File(applicationContext.filesDir, "videos").apply { mkdirs() }
        val tmp = File(dir, "$id.part")
        try {
            val store = KeyStoreManager(applicationContext)
            val api = ApiClient.create { store.getToken() }

            val key = (apiCall { api.getVideoKey(id) } as? LoadState.Success)?.data?.key
                ?: return@withContext Result.failure()
            val url = (resolveVideoUrl(api, id) as? LoadState.Success)?.data
                ?: return@withContext Result.failure()

            val conn = URL(url).openConnection() as HttpURLConnection
            conn.connectTimeout = 20000; conn.readTimeout = 30000
            conn.setRequestProperty("User-Agent", "Mozilla/5.0")
            val total = conn.contentLengthLong

            val iv = ByteArray(16).also { SecureRandom().nextBytes(it) }
            val cipher = VideoCrypto.cipherAt(VideoCrypto.keyBytes(key), iv, 0, Cipher.ENCRYPT_MODE)

            var done = 0L
            var lastPct = -1
            conn.inputStream.use { input ->
                FileOutputStream(tmp).use { fos ->
                    fos.write(iv)
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        if (isStopped) { tmp.delete(); return@withContext Result.failure() }
                        val n = input.read(buf)
                        if (n < 0) break
                        cipher.update(buf, 0, n, buf, 0)
                        fos.write(buf, 0, n)
                        done += n
                        if (total > 0) {
                            val pct = (done * 100 / total).toInt()
                            if (pct != lastPct) {
                                lastPct = pct
                                setProgress(workDataOf("p" to pct, "title" to title))
                            }
                        }
                    }
                }
            }
            val out = File(dir, "$id.enc")
            out.delete()
            tmp.renameTo(out)
            File(dir, "$id.title").writeText(title)
            store.saveVideoKey(id, key)
            Result.success()
        } catch (e: Exception) {
            tmp.delete()
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }
}

package com.app.aparatdownloader.download

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

class VideoDownloadWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val videoUrl = inputData.getString("video_url") ?: return@withContext Result.failure()
            val videoId = inputData.getString("video_id") ?: return@withContext Result.failure()
            val encryptionKeyHex = inputData.getString("encryption_key") ?: return@withContext Result.failure()

            val dir = File(applicationContext.filesDir, "videos")
            if (!dir.exists()) dir.mkdirs()
            val outputFile = File(dir, "$videoId.enc")

            val url = URL(videoUrl)
            val connection = url.openConnection()
            val inputStream = connection.getInputStream()

            // Simple AES encryption stream or chunk encryption
            val keyBytes = hexToBytes(encryptionKeyHex)
            val secretKey = SecretKeySpec(keyBytes, "AES")
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)

            val iv = cipher.iv

            FileOutputStream(outputFile).use { fos ->
                // Write IV first (12 bytes)
                fos.write(iv)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    val encrypted = cipher.update(buffer, 0, bytesRead)
                    if (encrypted != null) {
                        fos.write(encrypted)
                    }
                }
                val finalBytes = cipher.doFinal()
                if (finalBytes != null) {
                    fos.write(finalBytes)
                }
            }

            inputStream.close()
            Result.success()
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure()
        }
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}

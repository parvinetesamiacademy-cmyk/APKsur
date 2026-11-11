package com.app.aparatdownloader.security

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.common.util.UnstableApi
import java.io.File
import java.io.RandomAccessFile
import java.math.BigInteger
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/** فایل: ۱۶ بایت IV + داده رمزشده با AES-CTR (قابل seek) */
object VideoCrypto {
    fun keyBytes(k: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(k.toByteArray())

    fun cipherAt(key: ByteArray, iv: ByteArray, offset: Long, mode: Int): Cipher {
        val ctr = BigInteger(1, iv).add(BigInteger.valueOf(offset / 16))
        val raw = ctr.toByteArray()
        val out = ByteArray(16)
        val n = minOf(16, raw.size)
        System.arraycopy(raw, raw.size - n, out, 16 - n, n)
        val c = Cipher.getInstance("AES/CTR/NoPadding")
        c.init(mode, SecretKeySpec(key, "AES"), IvParameterSpec(out))
        val rem = (offset % 16).toInt()
        if (rem > 0) c.update(ByteArray(rem))
        return c
    }
}

@UnstableApi
class CtrFileDataSource(private val key: ByteArray) : DataSource {
    private var raf: RandomAccessFile? = null
    private var cipher: Cipher? = null
    private var uri: Uri? = null
    private var remaining = 0L

    override fun addTransferListener(transferListener: TransferListener) {}

    override fun open(dataSpec: DataSpec): Long {
        uri = dataSpec.uri
        val f = RandomAccessFile(File(dataSpec.uri.path!!), "r")
        raf = f
        val iv = ByteArray(16)
        f.readFully(iv)
        val plainLen = f.length() - 16
        f.seek(16 + dataSpec.position)
        cipher = VideoCrypto.cipherAt(key, iv, dataSpec.position, Cipher.DECRYPT_MODE)
        remaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) plainLen - dataSpec.position else dataSpec.length
        return remaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (remaining <= 0L) return C.RESULT_END_OF_INPUT
        val n = raf!!.read(buffer, offset, minOf(length.toLong(), remaining).toInt())
        if (n < 0) return C.RESULT_END_OF_INPUT
        cipher!!.update(buffer, offset, n, buffer, offset)
        remaining -= n
        return n
    }

    override fun getUri(): Uri? = uri
    override fun getResponseHeaders(): Map<String, List<String>> = emptyMap()
    override fun close() { raf?.close(); raf = null }
}

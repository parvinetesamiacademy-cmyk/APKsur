package com.app.aparatdownloader.data.api

import android.content.Context
import com.app.aparatdownloader.ui.common.LoadState
import com.app.aparatdownloader.ui.common.apiCall
import com.app.aparatdownloader.ui.common.cacheDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

object AparatResolver {
    private val posters = ConcurrentHashMap<String, String>()

    private fun attrs(hash: String): JSONObject? {
        repeat(2) {
            try {
                val c = URL("https://www.aparat.com/api/fa/v1/video/video/show/videohash/$hash")
                    .openConnection() as HttpURLConnection
                c.connectTimeout = 15000; c.readTimeout = 20000
                c.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                c.setRequestProperty("Accept", "application/json")
                if (c.responseCode in 200..299) {
                    val txt = c.inputStream.bufferedReader().use { it.readText() }
                    return JSONObject(txt).getJSONObject("data").getJSONObject("attributes")
                }
            } catch (e: Exception) { }
        }
        return null
    }

    /** لینک مستقیم mp4 با بهترین کیفیت (m3u8 رد می‌شود چون قابل دانلود نیست) */
    suspend fun resolve(hash: String): String? = withContext(Dispatchers.IO) {
        val a = attrs(hash) ?: return@withContext null
        var best: String? = null
        var bestH = -1
        val arr = a.optJSONArray("file_link_all")
        if (arr != null) for (i in 0 until arr.length()) {
            val o = arr.optJSONObject(i) ?: continue
            val h = o.optString("profile").filter { it.isDigit() }.toIntOrNull() ?: 0
            val urls = o.optJSONArray("urls") ?: continue
            var u: String? = null
            for (j in 0 until urls.length()) {
                val x = urls.optString(j)
                if (x.isNotBlank() && !x.contains(".m3u8")) { u = x; break }
            }
            if (u != null && h > bestH) { best = u; bestH = h }
        }
        best ?: a.optString("file_link").takeIf { it.isNotBlank() && !it.contains(".m3u8") }
    }

    /** عکس اولین ویدیو برای کاور دوره (کش‌شده) */
    suspend fun poster(ctx: Context, hash: String): String? = withContext(Dispatchers.IO) {
        posters[hash]?.let { return@withContext it }
        val f = File(cacheDir(ctx), "poster_$hash.txt")
        if (f.exists()) f.readText().takeIf { it.isNotBlank() }?.let { posters[hash] = it; return@withContext it }
        val a = attrs(hash) ?: return@withContext null
        val u = listOf("big_poster", "medium_poster", "small_poster", "thumbnail")
            .map { a.optString(it) }.firstOrNull { it.startsWith("http") } ?: return@withContext null
        posters[hash] = u
        try { f.writeText(u) } catch (_: Exception) {}
        u
    }
}

/** آدرس قابل پخش/دانلود ویدیو: از سرور، و در صورت نبودن لینک مستقیم، از آپارات */
suspend fun resolveVideoUrl(api: ApiService, videoId: String): LoadState<String> {
    return when (val r = apiCall { api.getVideoSource(videoId) }) {
        is LoadState.Success -> {
            val s = r.data
            if (s.url.isNotBlank()) LoadState.Success(s.url)
            else {
                val u = s.aparat_hash?.let { AparatResolver.resolve(it) }
                if (u != null) LoadState.Success(u)
                else LoadState.Error("لینک ویدیو از آپارات گرفته نشد")
            }
        }
        is LoadState.Error -> r
        else -> LoadState.Error("خطای نامشخص")
    }
}

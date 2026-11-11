package com.app.aparatdownloader.data.api

import com.app.aparatdownloader.ui.common.LoadState
import com.app.aparatdownloader.ui.common.apiCall
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AparatResolver {
    /** لینک مستقیم mp4 با بهترین کیفیت را از API آپارات می‌گیرد */
    suspend fun resolve(hash: String): String? = withContext(Dispatchers.IO) {
        try {
            val c = URL("https://www.aparat.com/api/fa/v1/video/video/show/videohash/$hash")
                .openConnection() as HttpURLConnection
            c.connectTimeout = 15000; c.readTimeout = 20000
            c.setRequestProperty("User-Agent", "Mozilla/5.0")
            val txt = c.inputStream.bufferedReader().use { it.readText() }
            val attrs = JSONObject(txt).getJSONObject("data").getJSONObject("attributes")
            var best: String? = null
            var bestH = -1
            val arr = attrs.optJSONArray("file_link_all")
            if (arr != null) for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val h = o.optString("profile").filter { it.isDigit() }.toIntOrNull() ?: 0
                val u = o.optJSONArray("urls")?.optString(0)
                if (!u.isNullOrBlank() && h > bestH) { best = u; bestH = h }
            }
            best ?: attrs.optString("file_link").ifEmpty { null }
        } catch (e: Exception) { null }
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

package com.app.aparatdownloader.data

import android.content.Context

/** پیشرفت تماشای هر ویدیو (فقط روی خود دستگاه ذخیره می‌شود) */
object WatchProgress {
    private fun p(ctx: Context) = ctx.getSharedPreferences("watch_progress", Context.MODE_PRIVATE)

    /** موقعیت فعلی پخش را ذخیره می‌کند؛ بیشترین نقطهٔ دیده‌شده جداگانه نگه داشته می‌شود */
    fun save(ctx: Context, id: String, posMs: Long, durMs: Long) {
        if (durMs <= 0 || posMs < 0) return
        val sp = p(ctx)
        val ended = posMs >= durMs - 1500
        val max = maxOf(sp.getLong("max_$id", 0L), if (ended) durMs else posMs)
        sp.edit()
            .putLong("pos_$id", if (ended) 0L else posMs)
            .putLong("max_$id", minOf(max, durMs))
            .putLong("dur_$id", durMs)
            .apply()
    }

    /** نقطه‌ای که باید پخش از آن ادامه پیدا کند */
    fun resumeAt(ctx: Context, id: String): Long = p(ctx).getLong("pos_$id", 0L)

    /** نسبت دیده‌شده بین ۰ تا ۱ (اگر مدت ویدیو هنوز معلوم نیست از مدت ثبت‌شده در سرور استفاده می‌شود) */
    fun fraction(ctx: Context, id: String, fallbackDurSec: Int = 0): Float {
        val sp = p(ctx)
        val max = sp.getLong("max_$id", 0L)
        val dur = sp.getLong("dur_$id", 0L).takeIf { it > 0 } ?: (fallbackDurSec * 1000L)
        if (max <= 0 || dur <= 0) return 0f
        val f = (max.toFloat() / dur).coerceIn(0f, 1f)
        return if (f >= 0.97f) 1f else f
    }

    fun clear(ctx: Context, id: String) {
        p(ctx).edit().remove("pos_$id").remove("max_$id").remove("dur_$id").apply()
    }
}

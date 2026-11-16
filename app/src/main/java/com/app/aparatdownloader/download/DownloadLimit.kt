package com.app.aparatdownloader.download

import android.content.Context

/**
 * محدودیت «هر ویدیو فقط یک‌بار دانلود» فقط سمت اپ است و سرور دخالتی ندارد.
 * بعد از پایان موفق دانلود ثبت می‌شود و با حذف ویدیو از دستگاه هم پاک نمی‌شود.
 */
object DownloadLimit {
    private fun p(ctx: Context) = ctx.getSharedPreferences("download_limit", Context.MODE_PRIVATE)
    fun isUsed(ctx: Context, id: String): Boolean = p(ctx).getBoolean("used_$id", false)
    fun markUsed(ctx: Context, id: String) { p(ctx).edit().putBoolean("used_$id", true).apply() }
}

package com.app.aparatdownloader.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Surface,
    primaryContainer = PrimaryLight,
    onPrimaryContainer = PrimaryDark,
    secondary = Accent,
    background = Background,
    surface = Surface,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun LuminaTheme(content: @Composable () -> Unit) {
    // اپ فارسی است؛ کل رابط راست‌چین می‌شود
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = LightColorScheme,
            content = content
        )
    }
}

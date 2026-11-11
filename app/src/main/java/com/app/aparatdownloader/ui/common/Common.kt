package com.app.aparatdownloader.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.aparatdownloader.data.api.ApiClient
import com.app.aparatdownloader.data.api.ApiService
import com.app.aparatdownloader.security.KeyStoreManager
import com.app.aparatdownloader.ui.theme.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import java.util.Locale
import kotlin.math.roundToInt

// ───────── وضعیت بارگذاری ─────────

sealed interface LoadState<out T> {
    object Loading : LoadState<Nothing>
    data class Success<T>(val data: T) : LoadState<T>
    data class Error(val message: String, val unauthorized: Boolean = false) : LoadState<Nothing>
}

private fun messageFor(code: String?): String = when (code) {
    "BANNED" -> "حساب شما مسدود شده است"
    "EXPIRED" -> "اعتبار حساب شما به پایان رسیده است"
    "DEVICE_MISMATCH" -> "این حساب روی دستگاه دیگری فعال شده است"
    "NOT_FOUND" -> "مورد درخواستی پیدا نشد"
    else -> "دریافت اطلاعات انجام نشد؛ دوباره تلاش کنید"
}

/** یک درخواست API را اجرا می‌کند و نتیجه را به LoadState تبدیل می‌کند */
suspend fun <T> apiCall(block: suspend () -> Response<T>): LoadState<T> =
    try {
        val res = block()
        val body = res.body()
        if (res.isSuccessful && body != null) {
            LoadState.Success(body)
        } else if (res.code() == 401) {
            LoadState.Error("نشست شما معتبر نیست؛ دوباره وارد شوید", unauthorized = true)
        } else {
            val raw = withContext(Dispatchers.IO) { res.errorBody()?.string() }
            LoadState.Error(messageFor(ApiClient.errorCodeOf(raw)))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        LoadState.Error("اتصال به سرور برقرار نشد؛ اینترنت خود را بررسی کنید")
    }

@Composable
fun rememberApi(): ApiService {
    val ctx = LocalContext.current.applicationContext
    return remember {
        val store = KeyStoreManager(ctx)
        ApiClient.create { store.getToken() }
    }
}

// ───────── قالب‌بندی فارسی ─────────

fun String.toFa(): String =
    map { if (it in '0'..'9') '۰' + (it - '0') else it }.joinToString("")

fun formatDuration(sec: Int?): String? {
    if (sec == null || sec <= 0) return null
    val minutes = (sec + 59) / 60
    return "$minutes دقیقه".toFa()
}

fun formatSize(bytes: Long?): String? {
    if (bytes == null || bytes <= 0) return null
    val mb = bytes / 1048576.0
    return if (mb >= 1024) {
        String.format(Locale.US, "%.1f گیگابایت", mb / 1024).toFa()
    } else {
        "${mb.roundToInt().coerceAtLeast(1)} مگابایت".toFa()
    }
}

// ───────── کامپوننت‌های مشترک ─────────

@Composable
fun LoadingState(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Primary)
    }
}

@Composable
fun ErrorState(
    state: LoadState.Error,
    onRetry: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CloudOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(state.message, color = TextSecondary, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(20.dp))
        if (state.unauthorized) {
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) { Text("ورود دوباره") }
        } else {
            Button(
                onClick = onRetry,
                colors = ButtonDefaults.buttonColors(containerColor = Primary),
                shape = RoundedCornerShape(12.dp)
            ) { Text("تلاش دوباره") }
        }
    }
}

@Composable
fun ComingSoonScreen(title: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Construction, contentDescription = null, tint = TextMuted, modifier = Modifier.size(48.dp))
        Spacer(Modifier.height(12.dp))
        Text(title, color = TextPrimary, fontSize = 16.sp)
        Spacer(Modifier.height(4.dp))
        Text("به‌زودی", color = TextSecondary, fontSize = 13.sp)
    }
}

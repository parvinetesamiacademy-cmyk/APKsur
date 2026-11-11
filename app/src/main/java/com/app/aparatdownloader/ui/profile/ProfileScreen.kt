package com.app.aparatdownloader.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.aparatdownloader.ui.theme.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.app.aparatdownloader.data.api.MeResponse
import com.app.aparatdownloader.ui.common.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(onLogout: () -> Unit) {
    val api = rememberApi()
    val ctx = LocalContext.current
    var state by remember { mutableStateOf<LoadState<MeResponse>>(LoadState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }
    var usedBytes by remember { mutableLongStateOf(0L) }
    LaunchedEffect(reloadKey) {
        state = LoadState.Loading
        state = apiCall { api.getMe() }
        usedBytes = File(ctx.filesDir, "videos").listFiles()?.sumOf { it.length() } ?: 0L
    }
    when (val st = state) {
        is LoadState.Loading -> { LoadingState(); return }
        is LoadState.Error -> { ErrorState(st, onRetry = { reloadKey++ }, onLogout = onLogout); return }
        is LoadState.Success -> {}
    }
    val me = (state as LoadState.Success).data
    val nowSec = me.server_time ?: (System.currentTimeMillis() / 1000)
    val daysLeft = (((me.expires_at ?: nowSec) - nowSec) / 86400).coerceAtLeast(0)
    val offlineDays = minOf(me.offline_days ?: 0, daysLeft.toInt())
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("پروفایل و لایسنس دانشجویی", color = TextPrimary, fontSize = 16.sp) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Student Info Card
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text((me.full_name ?: "-"), fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text((me.phone ?: "").toFa(), fontSize = 12.sp, color = TextSecondary)
                            }
                            Surface(color = PrimaryLight, shape = RoundedCornerShape(8.dp)) {
                                Text("دانشجوی رسمی", color = PrimaryDark, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("شماره موبایل", fontSize = 11.sp, color = TextMuted)
                                Text((me.phone ?: "-").toFa(), fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column {
                                Text("کد ملی ثبت‌شده", fontSize = 11.sp, color = TextMuted)
                                Text((me.national_id_mask ?: "-"), fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                        }
                    }
                }
            }

            // Offline License & DRM Status Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("وضعیت لایسنس و DRM آفلاین", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            Surface(color = PrimaryLight, shape = RoundedCornerShape(6.dp)) {
                                Text("امن و فعال", color = PrimaryDark, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("اعتبار بدون نیاز به اینترنت", fontSize = 12.sp, color = TextSecondary)
                                Text("می‌توانید تا $offlineDays روز دیگر ویدیوهای رمزشده را آفلاین ببینید.".toFa(), fontSize = 11.sp, color = TextMuted)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("• اشتراک: $daysLeft روز باقی‌مانده".toFa(), fontSize = 10.sp, color = Primary)
                            }
                            // Circular Days badge mock
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = PrimaryLight,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("$offlineDays روز".toFa(), fontWeight = FontWeight.Bold, color = PrimaryDark, fontSize = 12.sp)
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Divider(color = SurfaceContainerBorder)
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("دستگاه ثبت‌شده: ${me.device_model ?: "-"}", fontSize = 11.sp, color = TextSecondary)
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Primary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { reloadKey++ },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("استعلام و همگام‌سازی آنلاین مجوز ترم")
                        }
                    }
                }
            }

            // Storage Management Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("مدیریت حافظه ویدیوها", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                            Text((formatSize(usedBytes) ?: "۰ مگابایت"), fontSize = 12.sp, color = Primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { (usedBytes / 5_000_000_000f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Primary,
                            trackColor = SurfaceContainerLow
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = { File(ctx.filesDir, "videos").deleteRecursively(); reloadKey++ },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Danger, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حذف همه ویدیوهای دانلود‌شده", color = Danger, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onLogout, modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Danger),
                    shape = RoundedCornerShape(12.dp)) { Text("خروج از حساب") }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

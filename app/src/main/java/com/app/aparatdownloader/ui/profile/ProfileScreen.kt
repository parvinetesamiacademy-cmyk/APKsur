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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen() {
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
                                Text("سهراب رادمنش", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = TextPrimary)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("ترم جامع آیلتس و مکالمه پیشرفته (C1)", fontSize = 12.sp, color = TextSecondary)
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
                                Text("شماره زبان‌آموزی", fontSize = 11.sp, color = TextMuted)
                                Text("EN-84920", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column {
                                Text("کد ملی ثبت‌شده", fontSize = 11.sp, color = TextMuted)
                                Text("941000..2", fontWeight = FontWeight.Bold, color = TextPrimary)
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
                                Text("شما می‌توانید به مدت ۲۶ روز دیگر تمامی ویدیوهای رمز شده را کاملاً آفلاین مرور کنید.", fontSize = 11.sp, color = TextMuted)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text("• امضای معتبر Ed25519 تا ۲۹ اسفند", fontSize = 10.sp, color = Primary)
                            }
                            // Circular Days badge mock
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = PrimaryLight,
                                modifier = Modifier.size(60.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text("۲۶ روز", fontWeight = FontWeight.Bold, color = PrimaryDark, fontSize = 12.sp)
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
                            Text("دستگاه تایید شده اختصاصی: Samsung Galaxy S23 Ultra", fontSize = 11.sp, color = TextSecondary)
                            Icon(Icons.Default.PhoneAndroid, contentDescription = null, tint = Primary)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {},
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
                            Text("۴.۱ گیگابایت", fontSize = 12.sp, color = Primary)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { 0.7f },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                            color = Primary,
                            trackColor = SurfaceContainerLow
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("• آیلتس (2.4 GB)", fontSize = 11.sp, color = TextSecondary)
                            Text("• مکالمه (1.2 GB)", fontSize = 11.sp, color = TextSecondary)
                            Text("• گرامر (500 MB)", fontSize = 11.sp, color = TextSecondary)
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        OutlinedButton(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CleaningServices, contentDescription = null, tint = Danger, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("پاکسازی کش دروس مشاهده شده (850 MB)", color = Danger, fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

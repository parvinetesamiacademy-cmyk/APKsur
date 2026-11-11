package com.app.aparatdownloader.ui.player

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
fun PlayerScreen(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جلسه ویدیویی آموزشی", color = TextPrimary, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.Close, contentDescription = "بستن", tint = TextPrimary)
                    }
                },
                actions = {
                    Surface(color = PrimaryLight, shape = RoundedCornerShape(6.dp), modifier = Modifier.padding(end = 12.dp)) {
                        Text("DRM فعال | ضبط صفحه مسدود", color = PrimaryDark, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Video Player Mock Area
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(Color.Black),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Surface(
                            shape = RoundedCornerShape(32.dp),
                            color = Primary.copy(alpha = 0.85f),
                            modifier = Modifier.size(56.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Surface, modifier = Modifier.size(32.dp))
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("UID-99824 • محافظت شده", color = TextMuted, fontSize = 11.sp)
                    }

                    // Video Controls Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .background(Color.Black.copy(alpha = 0.6f))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("12:45 / 28:20", color = Surface, fontSize = 11.sp)
                        Surface(color = Surface.copy(alpha = 0.2f), shape = RoundedCornerShape(4.dp)) {
                            Text(" 1.0x ", color = Surface, fontSize = 11.sp, modifier = Modifier.padding(4.dp))
                        }
                    }
                }
            }

            // Lesson Title & Status
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("جلسه ۳: متد تضمینی تقویت لهجه و تلفظ (Shadowing Technique)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Verified, contentDescription = null, tint = Success, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("مجوز پخش آفلاین معتبر تا ۲۸ روز آینده", fontSize = 11.sp, color = TextSecondary)
                            }
                            Text("بدون نیاز به اینترنت", fontSize = 11.sp, color = Primary)
                        }
                    }
                }
            }

            // Transcript & Subtitles Section
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text("رونوشت و ترجمه همزمان (Shadowing)", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary)
                    Spacer(modifier = Modifier.height(8.dp))

                    // Transcript Card 1
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text("12:55 - 12:38", fontSize = 10.sp, color = TextMuted)
                            }
                            Text(
                                "When practicing shadowing, do not just copy the words; emulate the natural rhythm, intonation, and emotional cadence.",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(color = SurfaceContainerBorder)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "هنگام تمرین تکنیک شادویینگ (تکرار سایه‌وار)، تنها کلمات را تکرار نکنید؛ ریتم طبیعی، آهنگ کلام و حس گوینده را تقلید نمایید.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }

                    // Transcript Card 2
                    Card(
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Surface),
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                Text("13:18 - 12:56", fontSize = 10.sp, color = TextMuted)
                            }
                            Text(
                                "Notice how my vocal pitch dips slightly right before introducing the core contrasting argument.",
                                fontSize = 13.sp,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Divider(color = SurfaceContainerBorder)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                "توجه کنید که چگونه لحن صدای من درست قبل از بیان استدلال متضاد اصلی، کمی افت پیدا می‌کند.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // Voice Practice Action
            item {
                Card(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PrimaryLight)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("آزمایش تلفظ شما (شادویینگ زنده)", fontWeight = FontWeight.Bold, color = PrimaryDark, fontSize = 13.sp)
                            Text("برای ارزیابی هوش مصنوعی ضبط کنید", fontSize = 11.sp, color = TextSecondary)
                        }
                        Button(
                            onClick = {},
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("شروع تمرین", fontSize = 12.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

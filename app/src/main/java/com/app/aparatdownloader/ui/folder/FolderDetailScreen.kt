package com.app.aparatdownloader.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

data class LessonItem(
    val id: String,
    val title: String,
    val duration: String,
    val size: String,
    val status: String,
    val progress: Float? = null,
    val speed: String? = null
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(onLessonClick: (String) -> Unit, onBack: () -> Unit) {
    val lessons = listOf(
        LessonItem("1", "تکنیـک ۳: متد تضمینی تقویت لهجه و تلفظ (Shadowing)", "۲۲ دقیقه", "۸۴ مگابایت", "آفلاین / آماده پخش"),
        LessonItem("2", "۵۰ اصطلاح ضروری مکالمه و فلوئنس در سفر", "۲۸ دقیقه", "۱۰۵ مگابایت", "آماده پخش"),
        LessonItem("3", "غلبه بر مکث و لکنت زبان با تمرین ریتم آوازی", "۳۵ دقیقه", "۹۸ مگابایت", "در حال دانلود (۴۵٪)", 0.45f, "MB/s 1.2"),
        LessonItem("4", "اتصال کلمات در مکالمه سریع (Connected Speech)", "۱۹ دقیقه", "۷۲ مگابایت", "در صف دریافت #1"),
        LessonItem("5", "پاسخ به سوالات چالش‌برانگیز در مصاحبه و آزمون", "۲۴ دقیقه", "۸۹ مگابایت", "لایسنس فعال")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("جزئیات دوره و پوشه", color = TextPrimary, fontSize = 16.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "بازگشت", tint = TextPrimary)
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
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
                // Header Info Card
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
                            Surface(color = PrimaryLight, shape = RoundedCornerShape(8.dp)) {
                                Text("ترم پیشرفته C1 • رمزنگاری شده", color = PrimaryDark, fontSize = 11.sp, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                            }
                            Icon(Icons.Default.Folder, contentDescription = null, tint = Primary)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("دوره فشرده مکالمه و اسپیکینگ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text("محتوای رسمی و اختصاصی آکادمی لومینا", fontSize = 12.sp, color = TextSecondary)

                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("تعداد محتوا", fontSize = 11.sp, color = TextMuted)
                                Text("۱۲ ویدیو", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column {
                                Text("حجم پکیج", fontSize = 11.sp, color = TextMuted)
                                Text("۶۸۰ مگابایت", fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Column {
                                Text("پیشرفت مشاهده", fontSize = 11.sp, color = TextMuted)
                                Text("۲۵٪", fontWeight = FontWeight.Bold, color = Primary)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {},
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("دانلود امن یکجای کل ویدیوهای این پوشه", color = Surface)
                        }
                    }
                }
            }

            item {
                Text("لیست دروس و جلسات", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 16.sp, modifier = Modifier.padding(vertical = 4.dp))
            }

            items(lessons) { lesson ->
                Card(
                    onClick = { onLessonClick(lesson.id) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = PrimaryLight,
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Primary)
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(lesson.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary, maxLines = 1)
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text("${lesson.duration} • ${lesson.size}", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                            Surface(
                                color = if (lesson.status.contains("در حال")) Warning.copy(alpha = 0.15f) else PrimaryLight,
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    lesson.status,
                                    fontSize = 10.sp,
                                    color = if (lesson.status.contains("در حال")) Warning else PrimaryDark,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                                )
                            }
                        }
                        if (lesson.progress != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LinearProgressIndicator(
                                progress = { lesson.progress },
                                modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                                color = Warning,
                                trackColor = SurfaceContainerLow
                            )
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

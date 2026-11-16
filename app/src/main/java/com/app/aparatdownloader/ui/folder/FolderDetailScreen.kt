package com.app.aparatdownloader.ui.folder

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.aparatdownloader.data.api.Playlist
import com.app.aparatdownloader.data.api.PlaylistDetail
import com.app.aparatdownloader.data.api.VideoItem
import com.app.aparatdownloader.ui.common.*
import android.widget.Toast
import androidx.compose.material.icons.filled.Download
import androidx.compose.ui.platform.LocalContext
import androidx.work.*
import com.app.aparatdownloader.download.VideoDownloadWorker
import java.io.File
import com.app.aparatdownloader.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FolderDetailScreen(
    playlist: Playlist,
    onLessonClick: (VideoItem) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    val api = rememberApi()
    val ctx = LocalContext.current
    var state by remember { mutableStateOf<LoadState<PlaylistDetail>>(LoadState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        loadWithCache<PlaylistDetail>(ctx, "videos_${playlist.id}", { state = it }) { api.getPlaylistVideos(playlist.id) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(playlist.title, color = TextPrimary, fontSize = 16.sp, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "بازگشت", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val s = state) {
                is LoadState.Loading -> LoadingState()
                is LoadState.Error -> ErrorState(s, onRetry = { reloadKey++ }, onLogout = onLogout)
                is LoadState.Success -> {
                    val videos = s.data.videos.sortedBy { it.position }
                    val dir = File(ctx.filesDir, "videos")
                    // حجم واقعیِ فایل‌های دانلودشده روی دستگاه؛ اگر دانلود نشده از حجم سرور استفاده می‌شود
                    val totalBytes = videos.sumOf { v ->
                        val local = File(dir, "${v.id}.enc").takeIf { it.exists() }?.length()
                        local ?: (v.size_bytes ?: 0L)
                    }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Spacer(Modifier.height(8.dp))
                            SummaryCard(playlist.title, playlist.description, videos.size, formatSize(totalBytes))
                        }
                        item {
                            Button(
                                onClick = {
                                    val n = videos.count { startDownload(ctx, it, playlist.title, it.position, silent = true) }
                                    Toast.makeText(ctx, "$n ویدیو به صف دانلود اضافه شد".toFa(), Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Download, null)
                                Spacer(Modifier.width(8.dp))
                                Text("دانلود کل دوره")
                            }
                        }
                        item {
                            Text(
                                "لیست دروس و جلسات",
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                fontSize = 16.sp,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }
                        itemsIndexed(videos, key = { _, v -> v.id }) { index, video ->
                            LessonRow(index + 1, video, onDownload = { startDownload(ctx, video, playlist.title, video.position) }) { onLessonClick(video) }
                        }
                        item { Spacer(Modifier.height(24.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(title: String, description: String?, count: Int, size: String?) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(18.dp).fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
            if (!description.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(description, fontSize = 13.sp, color = TextSecondary, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("تعداد محتوا", fontSize = 11.sp, color = TextMuted)
                    Text("$count ویدیو".toFa(), fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                if (size != null) {
                    Column {
                        Text("حجم پکیج", fontSize = 11.sp, color = TextMuted)
                        Text(size, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
private fun LessonRow(number: Int, video: VideoItem, onDownload: () -> Unit, onClick: () -> Unit) {
    val meta = listOfNotNull(formatDuration(video.duration_sec), formatSize(video.size_bytes))
        .joinToString(" • ")
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(color = PrimaryLight, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(number.toString().toFa(), color = PrimaryDark, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(video.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = TextPrimary, maxLines = 2)
                if (meta.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(meta, fontSize = 11.sp, color = TextSecondary)
                }
            }
            IconButton(onClick = onDownload) { Icon(Icons.Default.Download, contentDescription = "دانلود", tint = Primary) }
            Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Primary)
        }
    }
}

/** true = به صف اضافه شد */
private fun startDownload(ctx: android.content.Context, video: VideoItem, course: String, idx: Int, silent: Boolean = false): Boolean {
    val dir = File(ctx.filesDir, "videos").apply { mkdirs() }
    // محدودیت دانلود تکراری برداشته شد: اگر قبلاً دانلود شده بود، فایل قدیمی پاک و دوباره دانلود می‌شود
    try {
        File(dir, "${video.id}.enc").delete()
        File(dir, "${video.id}.part").delete()
        File(dir, "${video.id}.dur").writeText((video.duration_sec ?: 0).toString())
        com.app.aparatdownloader.download.enqueueDownload(ctx, video.id, video.title, course, idx)
    } catch (e: Exception) {
        if (!silent) Toast.makeText(ctx, "خطا در شروع دانلود", Toast.LENGTH_SHORT).show()
        return false
    }
    if (!silent) Toast.makeText(ctx, "به صف دانلود اضافه شد", Toast.LENGTH_SHORT).show()
    return true
}

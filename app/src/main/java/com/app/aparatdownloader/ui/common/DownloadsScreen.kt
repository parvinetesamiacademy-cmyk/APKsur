package com.app.aparatdownloader.ui.common

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.app.aparatdownloader.ui.theme.*
import com.app.aparatdownloader.download.enqueueDownload
import com.app.aparatdownloader.download.pauseDownload
import com.app.aparatdownloader.download.cancelDownload
import com.app.aparatdownloader.download.enqueueDownload
import com.app.aparatdownloader.download.pauseDownload
import com.app.aparatdownloader.download.cancelDownload
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import com.app.aparatdownloader.data.WatchProgress
import java.io.File

private data class DlItem(val id: String, val title: String, val size: Long, val idx: Int, val course: String, val frac: Float)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(onPlay: (String, String) -> Unit) {
    val ctx = LocalContext.current
    val dir = remember { File(ctx.filesDir, "videos") }
    var dl by remember { mutableStateOf(listOf<DlItem>()) }
    var toDelete by remember { mutableStateOf<DlItem?>(null) }

    fun load() {
        dl = dir.listFiles()?.filter { it.isFile && it.name.endsWith(".enc") }?.map { f ->
            val id = f.nameWithoutExtension
            val (idx, course) = courseInfo(dir, id)
            val dur = File(dir, "$id.dur").takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: 0
            DlItem(
                id = id,
                title = File(dir, "$id.title").takeIf { it.exists() }?.readText() ?: id,
                size = f.length(), idx = idx, course = course,
                frac = WatchProgress.fraction(ctx, id, dur)
            )
        }?.sortedBy { it.idx } ?: emptyList()
    }
    LaunchedEffect(Unit) { load() }

    toDelete?.let { d ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text("حذف ویدیو؟") },
            text = { Text("«${d.title}» از دستگاه حذف می‌شود. چون هر ویدیو فقط یک‌بار قابل دانلود است، بعد از حذف نمی‌توانید دوباره آن را دانلود کنید.") },
            confirmButton = {
                TextButton(onClick = {
                    File(dir, "${d.id}.enc").delete(); File(dir, "${d.id}.title").delete()
                    File(dir, "${d.id}.course").delete(); File(dir, "${d.id}.dur").delete()
                    WatchProgress.clear(ctx, d.id)
                    toDelete = null; load()
                }) { Text("حذف", color = Danger) }
            },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text("انصراف") } }
        )
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("دوره‌های دانلودشده", color = TextPrimary, fontWeight = FontWeight.Bold) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) },
        containerColor = Background
    ) { pad ->
        if (dl.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("هنوز ویدیویی دانلود نشده است", color = TextSecondary)
            }
        } else {
            val groups = dl.groupBy { it.course }
            val overall = dl.map { it.frac }.average().toFloat()
            LazyColumn(
                Modifier.padding(pad).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                item { OverallCard(groups.size, dl.size, overall) }
                items(groups.keys.toList(), key = { it }) { cname ->
                    val list = groups[cname]!!
                    DownloadedCourseCard(cname, list, onPlay = { onPlay(it.id, it.title) }, onDelete = { toDelete = it })
                }
            }
        }
    }
}

private fun faPct(f: Float) = "${(f * 100).toInt()}٪".toFa()

@Composable
private fun AnimatedBar(frac: Float, modifier: Modifier = Modifier, height: Dp = 8.dp, onGradient: Boolean = false) {
    val f by animateFloatAsState(frac.coerceIn(0f, 1f), label = "bar")
    val track = if (onGradient) Color.White.copy(alpha = 0.28f) else SurfaceContainerBorder
    val fill = if (onGradient) Brush.horizontalGradient(listOf(Color.White, Color.White))
        else Brush.horizontalGradient(if (frac >= 1f) listOf(Success, Success) else listOf(Primary, Accent))
    Box(modifier.height(height).clip(RoundedCornerShape(50)).background(track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(f).clip(RoundedCornerShape(50)).background(fill))
    }
}

@Composable
private fun OverallCard(courses: Int, videos: Int, frac: Float) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("پیشرفت کلی تماشا", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                    Text("$courses دوره • $videos ویدیو".toFa(), color = TextSecondary, fontSize = 12.sp)
                }
                Text(faPct(frac), color = PrimaryDark, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp)
            }
            Spacer(Modifier.height(12.dp))
            AnimatedBar(frac, Modifier.fillMaxWidth(), height = 10.dp)
        }
    }
}

@Composable
private fun DownloadedCourseCard(name: String, list: List<DlItem>, onPlay: (DlItem) -> Unit, onDelete: (DlItem) -> Unit) {
    val courseFrac = list.map { it.frac }.average().toFloat()
    val finished = list.count { it.frac >= 1f }
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Surface), elevation = CardDefaults.cardElevation(2.dp)) {
        Column {
            Column(
                Modifier.fillMaxWidth()
                    .background(Brush.linearGradient(listOf(PrimaryDark, Primary, Accent)))
                    .padding(18.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp, modifier = Modifier.weight(1f), maxLines = 2)
                    Text(faPct(courseFrac), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text("${list.size} ویدیو • $finished کامل‌شده".toFa(), color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                AnimatedBar(courseFrac, Modifier.fillMaxWidth(), height = 10.dp, onGradient = true)
            }
            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                list.forEachIndexed { i, it ->
                    DownloadedVideoRow(i + 1, it, onPlay = { onPlay(it) }, onDelete = { onDelete(it) })
                }
            }
        }
    }
}

@Composable
private fun DownloadedVideoRow(number: Int, v: DlItem, onPlay: () -> Unit, onDelete: () -> Unit) {
    val status = when { v.frac >= 1f -> "کامل شد"; v.frac > 0f -> "در حال تماشا"; else -> "تماشا نشده" }
    val statusColor = when { v.frac >= 1f -> Success; v.frac > 0f -> PrimaryDark; else -> TextMuted }
    Card(
        onClick = onPlay, shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceContainerLow),
        border = androidx.compose.foundation.BorderStroke(1.dp, SurfaceContainerBorder)
    ) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(PrimaryLight), contentAlignment = Alignment.Center) {
                Text(number.toString().toFa(), color = PrimaryDark, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(v.title, color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2)
                Text(formatSize(v.size) ?: "", color = TextSecondary, fontSize = 11.sp)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AnimatedBar(v.frac, Modifier.weight(1f), height = 7.dp)
                    Spacer(Modifier.width(8.dp))
                    Text(faPct(v.frac), color = statusColor, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text(status, color = statusColor, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Spacer(Modifier.width(6.dp))
            FilledIconButton(
                onClick = onPlay, modifier = Modifier.size(42.dp),
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = Primary, contentColor = Color.White)
            ) { Icon(Icons.Default.PlayArrow, "پخش") }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "حذف", tint = Danger) }
        }
    }
}


private fun courseInfo(dir: File, id: String): Pair<Int, String> {
    val l = File(dir, "$id.course").takeIf { it.exists() }?.readLines() ?: return 0 to "سایر"
    return (l.getOrNull(0)?.toIntOrNull() ?: 0) to (l.getOrNull(1) ?: "سایر")
}

@Composable
private fun CourseBox(name: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Background), border = androidx.compose.foundation.BorderStroke(1.dp, Primary)) {
        Column(Modifier.fillMaxWidth().padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(name, color = Primary, fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)
            content()
        }
    }
}

private fun fmtSpeed(b: Long) = (formatSize(b) ?: "0") + "/s"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen() {
    val ctx = LocalContext.current
    val wm = remember { WorkManager.getInstance(ctx) }
    val infos by wm.getWorkInfosByTagFlow("dl").collectAsState(initial = emptyList())
    var online by remember { mutableStateOf(true) }
    DisposableEffect(Unit) {
        val cm = ctx.getSystemService(android.content.Context.CONNECTIVITY_SERVICE) as android.net.ConnectivityManager
        fun cur() = cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(android.net.NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        online = cur()
        val cb = object : android.net.ConnectivityManager.NetworkCallback() {
            override fun onAvailable(n: android.net.Network) { online = true }
            override fun onLost(n: android.net.Network) { online = cur() }
        }
        cm.registerDefaultNetworkCallback(cb)
        onDispose { cm.unregisterNetworkCallback(cb) }
    }
    // پایش فایل‌های نیمه‌کاره (برای حالت توقف‌شده و بایت‌های دریافتی)
    var tick by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { kotlinx.coroutines.delay(1000); tick++ } }
    val dir = File(ctx.filesDir, "videos")
    fun vidOf(w: WorkInfo) = w.tags.firstOrNull { it.startsWith("vid:") }?.removePrefix("vid:") ?: ""
    val active = infos.filter { it.state == WorkInfo.State.ENQUEUED || it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.BLOCKED || it.state == WorkInfo.State.FAILED }
    val activeIds = active.map { vidOf(it) }.toSet()
    val paused = remember(tick, infos) {
        dir.listFiles()?.filter { it.name.endsWith(".part") && it.nameWithoutExtension !in activeIds }?.map { it.nameWithoutExtension } ?: emptyList()
    }
    fun titleOf(id: String) = File(dir, "$id.title").takeIf { it.exists() }?.readText() ?: id
    fun doneOf(id: String) = maxOf(0L, File(dir, "$id.part").length() - 16)

    Scaffold(
        topBar = { TopAppBar(title = { Text("صف دانلود", color = TextPrimary) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) },
        containerColor = Background
    ) { pad ->
        Column(Modifier.padding(pad).padding(horizontal = 16.dp)) {
            Text(
                if (online) "● اینترنت برقرار است" else "● اینترنت قطع است؛ دانلود پس از اتصال ادامه می‌یابد",
                color = if (online) Primary else Danger, modifier = Modifier.padding(vertical = 8.dp)
            )
            if (active.isEmpty() && paused.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("صف دانلود خالی است", color = TextSecondary) }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { active.forEach { pauseDownload(ctx, vidOf(it)) } }) { Text("توقف همه") }
                    OutlinedButton(onClick = { paused.forEach { enqueueDownload(ctx, it, titleOf(it)) } }) { Text("ادامه همه") }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val entries = active.map { Triple(vidOf(it), it, true) } + paused.map { Triple(it, null as WorkInfo?, false) }
                    val qg = entries.groupBy { courseInfo(dir, it.first).second }
                    items(qg.keys.toList(), key = { it }) { cname ->
                        CourseBox(cname) {
                            qg[cname]!!.sortedBy { courseInfo(dir, it.first).first }.forEach { (id, w, isAct) ->
                                if (isAct && w != null) {
                                                                        val running = w.state == WorkInfo.State.RUNNING
                                                val done = w.progress.getLong("done", doneOf(id))
                                                val total = w.progress.getLong("total", 0L)
                                                val pct = w.progress.getInt("p", 0)
                                                val speed = w.progress.getLong("speed", 0L)
                                                QueueCard(
                                                    title = titleOf(id), progress = if (total > 0) pct / 100f else null,
                                                    status = when {
                                                        w.state == WorkInfo.State.FAILED -> w.outputData.getString("err") ?: "ناموفق"
                                                        running && total > 0 -> "$pct% • ${formatSize(done)} از ${formatSize(total)} • ${fmtSpeed(speed)}".toFa()
                                                        running -> "در انتظار نوبت..."
                                                        !online -> "منتظر اتصال اینترنت"
                                                        else -> "در انتظار / تلاش مجدد..."
                                                    },
                                                    failed = w.state == WorkInfo.State.FAILED,
                                                    onPauseResume = { if (w.state == WorkInfo.State.FAILED) enqueueDownload(ctx, id, titleOf(id)) else pauseDownload(ctx, id) },
                                                    pauseLabel = if (w.state == WorkInfo.State.FAILED) "تلاش مجدد" else "توقف",
                                                    onCancel = { cancelDownload(ctx, id) }
                                                )
                                                        } else {
                        QueueCard(
                                                    title = titleOf(id), progress = null,
                                                    status = "متوقف‌شده • ${formatSize(doneOf(id))} دریافت شده".toFa(), failed = false,
                                                    onPauseResume = { enqueueDownload(ctx, id, titleOf(id)) }, pauseLabel = "ادامه",
                                                    onCancel = { cancelDownload(ctx, id) }
                                                )
                                                        }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueCard(title: String, progress: Float?, status: String, failed: Boolean,
                      onPauseResume: () -> Unit, pauseLabel: String, onCancel: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Text(title, color = TextPrimary, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            if (progress != null) LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = Primary)
            else LinearProgressIndicator(progress = { 0f }, modifier = Modifier.fillMaxWidth(), color = Primary)
            Text(status, color = if (failed) Danger else TextSecondary, fontSize = androidx.compose.ui.unit.TextUnit.Unspecified, modifier = Modifier.padding(top = 4.dp))
            Row {
                TextButton(onClick = onPauseResume) { Text(pauseLabel) }
                TextButton(onClick = onCancel) { Text("لغو", color = Danger) }
            }
        }
    }
}

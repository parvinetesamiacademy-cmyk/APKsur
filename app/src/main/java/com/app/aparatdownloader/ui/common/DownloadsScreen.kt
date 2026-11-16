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
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(onPlay: (String, String) -> Unit) {
    val ctx = LocalContext.current
    var files by remember { mutableStateOf(listOf<File>()) }
    fun load() {
        files = File(ctx.filesDir, "videos").listFiles()
            ?.filter { it.isFile && it.name.endsWith(".enc") }?.sortedBy { it.name } ?: emptyList()
    }
    LaunchedEffect(Unit) { load() }
    fun titleOf(f: File) = File(f.parentFile, f.nameWithoutExtension + ".title")
        .takeIf { it.exists() }?.readText() ?: f.nameWithoutExtension
    Scaffold(
        topBar = { TopAppBar(title = { Text("دانلودها", color = TextPrimary) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) },
        containerColor = Background
    ) { pad ->
        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("هنوز ویدیویی دانلود نشده است", color = TextSecondary)
            }
        } else LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(files, key = { it.name }) { f ->
                Card(onClick = { onPlay(f.nameWithoutExtension, titleOf(f)) }, colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(titleOf(f), color = TextPrimary, maxLines = 1)
                            Text(formatSize(f.length()) ?: "", color = TextSecondary)
                        }
                        IconButton(onClick = {
                            f.delete(); File(f.parentFile, f.nameWithoutExtension + ".title").delete(); load()
                        }) { Icon(Icons.Default.Delete, "حذف", tint = Danger) }
                    }
                }
            }
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
                    items(active, key = { it.id }) { w ->
                        val id = vidOf(w)
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
                    }
                    items(paused, key = { "p_$it" }) { id ->
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

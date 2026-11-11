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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen() {
    val ctx = LocalContext.current
    val wm = remember { WorkManager.getInstance(ctx) }
    val infos by wm.getWorkInfosByTagFlow("dl").collectAsState(initial = emptyList())
    val list = infos.filter { it.state != WorkInfo.State.SUCCEEDED && it.state != WorkInfo.State.CANCELLED }
    Scaffold(
        topBar = { TopAppBar(title = { Text("صف دانلود", color = TextPrimary) }, colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)) },
        containerColor = Background
    ) { pad ->
        if (list.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(pad), contentAlignment = Alignment.Center) {
                Text("صف دانلود خالی است", color = TextSecondary)
            }
        } else LazyColumn(Modifier.padding(pad).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(list, key = { it.id }) { w ->
                val title = w.tags.firstOrNull { it.startsWith("title:") }?.removePrefix("title:") ?: ""
                val pct = w.progress.getInt("p", 0)
                Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(title, color = TextPrimary, modifier = Modifier.weight(1f), maxLines = 1)
                            TextButton(onClick = { wm.cancelWorkById(w.id) }) { Text("لغو", color = Danger) }
                        }
                        when (w.state) {
                            WorkInfo.State.RUNNING -> {
                                LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth(), color = Primary)
                                Text("$pct%".toFa(), color = TextSecondary)
                            }
                            WorkInfo.State.FAILED -> Text("ناموفق", color = Danger)
                            else -> Text("در انتظار...", color = TextSecondary)
                        }
                    }
                }
            }
        }
    }
}

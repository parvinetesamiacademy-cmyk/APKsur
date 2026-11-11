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
import com.app.aparatdownloader.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen() {
    val ctx = LocalContext.current
    var files by remember { mutableStateOf(listOf<File>()) }
    fun load() { files = File(ctx.filesDir, "videos").listFiles()?.filter { it.isFile }?.sortedBy { it.name } ?: emptyList() }
    LaunchedEffect(Unit) { load() }
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
                Card(colors = CardDefaults.cardColors(containerColor = Surface)) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(f.nameWithoutExtension, color = TextPrimary, maxLines = 1)
                            Text(formatSize(f.length()) ?: "", color = TextSecondary)
                        }
                        IconButton(onClick = { f.delete(); load() }) { Icon(Icons.Default.Delete, "حذف", tint = Danger) }
                    }
                }
            }
        }
    }
}

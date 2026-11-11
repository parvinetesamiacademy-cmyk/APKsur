package com.app.aparatdownloader.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.app.aparatdownloader.ui.components.FolderCard
import com.app.aparatdownloader.ui.theme.*

data class CourseFolder(
    val id: String,
    val title: String,
    val level: String,
    val subfoldersCount: Int,
    val videosCount: Int,
    val progressPercent: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(onFolderClick: (String) -> Unit, onDownloadAll: (String) -> Unit) {
    val folders = listOf(
        CourseFolder(
            id = "folder_speaking_01",
            title = "دوره فشرده مکالمه و اسپیکینگ (Pronunciation & Fluency)",
            level = "C1 - B2",
            subfoldersCount = 4,
            videosCount = 32,
            progressPercent = 65
        ),
        CourseFolder(
            id = "folder_grammar_02",
            title = "گرامر جامع کاربردی زبان انگلیسی (Applied Grammar)",
            level = "Intermediate",
            subfoldersCount = 3,
            videosCount = 24,
            progressPercent = 40
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دوره‌های آموزشی Lumina Academy", color = TextPrimary) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = Background
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            items(folders) { folder ->
                FolderCard(
                    title = folder.title,
                    level = folder.level,
                    subfoldersCount = folder.subfoldersCount,
                    videosCount = folder.videosCount,
                    progressPercent = folder.progressPercent,
                    onEnterFolder = { onFolderClick(folder.id) },
                    onDownloadAll = { onDownloadAll(folder.id) }
                )
            }
        }
    }
}

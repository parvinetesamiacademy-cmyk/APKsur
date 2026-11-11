package com.app.aparatdownloader.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.app.aparatdownloader.data.api.Playlist
import com.app.aparatdownloader.data.api.VideoItem
import com.app.aparatdownloader.ui.common.ComingSoonScreen
import com.app.aparatdownloader.ui.folder.FolderDetailScreen
import com.app.aparatdownloader.ui.player.PlayerScreen
import com.app.aparatdownloader.ui.playlist.PlaylistScreen
import com.app.aparatdownloader.ui.profile.ProfileScreen
import com.app.aparatdownloader.ui.theme.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Courses : Screen("courses", "دوره‌ها", Icons.Default.VideoLibrary)
    object Downloads : Screen("downloads", "دانلودها", Icons.Default.Download)
    object Queue : Screen("queue", "صف دانلود", Icons.Default.HourglassEmpty)
    object Profile : Screen("profile", "پروفایل", Icons.Default.Person)
}

@Composable
fun MainAppScreen(onLogout: () -> Unit) {
    var selectedTab by remember { mutableStateOf("courses") }
    var openPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var playing by remember { mutableStateOf<Pair<String, String>?>(null) }

    // دکمه‌ی بازگشت سیستم: پلیر ← لیست دروس ← دوره‌ها
    BackHandler(enabled = playing != null) { playing = null }
    BackHandler(enabled = playing == null && openPlaylist != null) { openPlaylist = null }

    playing?.let { (id, title) ->
        PlayerScreen(videoId = id, title = title, onBack = { playing = null }, onLogout = onLogout)
        return
    }

    openPlaylist?.let { playlist ->
        FolderDetailScreen(
            playlist = playlist,
            onLessonClick = { playing = it.id to it.title },
            onBack = { openPlaylist = null },
            onLogout = onLogout
        )
        return
    }

    val items = listOf(Screen.Courses, Screen.Downloads, Screen.Queue, Screen.Profile)

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Surface) {
                items.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.title) },
                        label = { Text(screen.title) },
                        selected = selectedTab == screen.route,
                        onClick = { selectedTab = screen.route },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Primary,
                            selectedTextColor = Primary,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                            indicatorColor = PrimaryLight
                        )
                    )
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                "courses" -> PlaylistScreen(
                    onFolderClick = { openPlaylist = it },
                    onLogout = onLogout
                )
                "downloads" -> com.app.aparatdownloader.ui.common.DownloadsScreen(onPlay = { id, t -> playing = id to t })
                "queue" -> com.app.aparatdownloader.ui.common.QueueScreen()
                "profile" -> ProfileScreen(onLogout = onLogout)
            }
        }
    }
}

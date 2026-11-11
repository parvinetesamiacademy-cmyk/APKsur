package com.app.aparatdownloader.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen() {
    var selectedTab by remember { mutableStateOf<String>("courses") }
    var currentSubScreen by remember { mutableStateOf<String?>(null) }

    if (currentSubScreen == "folder_detail") {
        FolderDetailScreen(
            onLessonClick = { currentSubScreen = "player" },
            onBack = { currentSubScreen = null }
        )
        return
    }

    if (currentSubScreen == "player") {
        PlayerScreen(onBack = { currentSubScreen = "folder_detail" })
        return
    }

    val items = listOf(
        Screen.Courses,
        Screen.Downloads,
        Screen.Queue,
        Screen.Profile
    )

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
        androidx.compose.foundation.layout.Box(modifier = Modifier.padding(padding)) {
            when (selectedTab) {
                "courses" -> PlaylistScreen(
                    onFolderClick = { currentSubScreen = "folder_detail" },
                    onDownloadAll = { currentSubScreen = "folder_detail" }
                )
                "downloads" -> PlaylistScreen(
                    onFolderClick = { currentSubScreen = "folder_detail" },
                    onDownloadAll = { currentSubScreen = "folder_detail" }
                )
                "queue" -> PlaylistScreen(
                    onFolderClick = { currentSubScreen = "folder_detail" },
                    onDownloadAll = { currentSubScreen = "folder_detail" }
                )
                "profile" -> ProfileScreen()
            }
        }
    }
}

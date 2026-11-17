package com.app.aparatdownloader.ui.playlist

import com.app.aparatdownloader.ui.onboarding.EmptyHint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.app.aparatdownloader.data.api.Playlist
import com.app.aparatdownloader.ui.common.*
import com.app.aparatdownloader.ui.components.CourseCard
import com.app.aparatdownloader.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistScreen(onFolderClick: (Playlist) -> Unit, onLogout: () -> Unit) {
    val api = rememberApi()
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var state by remember { mutableStateOf<LoadState<List<Playlist>>>(LoadState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(reloadKey) {
        loadWithCache<List<Playlist>>(ctx, "playlists", { state = it }) { api.getPlaylists() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("دوره‌ها", color = TextPrimary) },
                actions = {
                    IconButton(onClick = { reloadKey++ }) {
                        Icon(Icons.Default.Refresh, contentDescription = "بروزرسانی", tint = TextSecondary)
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
                    val list = s.data.sortedBy { it.sort_order ?: Int.MAX_VALUE }
                    if (list.isEmpty()) {
                        EmptyHint("هنوز دوره‌ای برای شما فعال نشده است", "ویدیوهای ترم شما به‌زودی توسط آکادمی اضافه می‌شود. صفحه را دوباره باز کنید یا با پشتیبانی تماس بگیرید.")
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 12.dp)
                        ) {
                            items(list, key = { it.id }) { playlist ->
                                CourseCard(playlist = playlist, onClick = { onFolderClick(playlist) })
                            }
                        }
                    }
                }
            }
        }
    }
}

package com.app.aparatdownloader.ui.player

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.app.aparatdownloader.data.api.resolveVideoUrl
import com.app.aparatdownloader.security.CtrFileDataSource
import com.app.aparatdownloader.security.KeyStoreManager
import com.app.aparatdownloader.security.VideoCrypto
import com.app.aparatdownloader.ui.common.*
import com.app.aparatdownloader.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun PlayerScreen(videoId: String, title: String, onBack: () -> Unit, onLogout: () -> Unit) {
    val ctx = LocalContext.current
    val api = rememberApi()
    val player = remember { ExoPlayer.Builder(ctx).build() }
    var state by remember { mutableStateOf<LoadState<Unit>>(LoadState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }

    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(reloadKey) {
        state = LoadState.Loading
        val local = File(File(ctx.filesDir, "videos"), "$videoId.enc")
        val key = KeyStoreManager(ctx).getVideoKey(videoId)
        if (local.exists() && key != null) {
            val kb = VideoCrypto.keyBytes(key)
            val factory = DataSource.Factory { CtrFileDataSource(kb) }
            player.setMediaSource(
                ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(Uri.fromFile(local)))
            )
            player.prepare(); player.playWhenReady = true
            state = LoadState.Success(Unit)
        } else {
            when (val r = resolveVideoUrl(api, videoId)) {
                is LoadState.Success -> {
                    player.setMediaItem(MediaItem.fromUri(r.data))
                    player.prepare(); player.playWhenReady = true
                    state = LoadState.Success(Unit)
                }
                is LoadState.Error -> state = r
                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = TextPrimary, fontSize = 16.sp, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.Close, "بستن", tint = TextPrimary) }
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
                is LoadState.Success -> AndroidView(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16 / 9f),
                    factory = { c -> PlayerView(c).apply { this.player = player } }
                )
            }
        }
    }
}

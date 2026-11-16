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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import com.app.aparatdownloader.data.api.resolveVideoUrl
import com.app.aparatdownloader.security.CtrFileDataSource
import com.app.aparatdownloader.security.KeyStoreManager
import com.app.aparatdownloader.security.VideoCrypto
import com.app.aparatdownloader.ui.common.*
import com.app.aparatdownloader.ui.theme.*
import java.io.File
import android.app.Activity
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

@OptIn(ExperimentalMaterial3Api::class, UnstableApi::class)
@Composable
fun PlayerScreen(videoId: String, title: String, onBack: () -> Unit, onLogout: () -> Unit) {
    val ctx = LocalContext.current
    val api = rememberApi()
    val player = remember { ExoPlayer.Builder(ctx).build() }
    var state by remember { mutableStateOf<LoadState<Unit>>(LoadState.Loading) }
    var reloadKey by remember { mutableIntStateOf(0) }

    var fs by remember { mutableStateOf(true) }
    val activity = remember(ctx) { var c = ctx; while (c is ContextWrapper && c !is Activity) c = c.baseContext; c as? Activity }
    LaunchedEffect(fs) {
        activity?.let { a ->
            a.requestedOrientation = if (fs) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            val ic = WindowCompat.getInsetsController(a.window, a.window.decorView)
            if (fs) { ic.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE; ic.hide(WindowInsetsCompat.Type.systemBars()) }
            else ic.show(WindowInsetsCompat.Type.systemBars())
        }
    }
    BackHandler(enabled = fs) { fs = false }
    DisposableEffect(Unit) { onDispose {
        player.release()
        activity?.let { a ->
            a.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            WindowCompat.getInsetsController(a.window, a.window.decorView).show(WindowInsetsCompat.Type.systemBars())
        }
    } }

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
                    val http = DefaultHttpDataSource.Factory()
                        .setUserAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36")
                        .setAllowCrossProtocolRedirects(true)
                        .setDefaultRequestProperties(mapOf("Referer" to "https://www.aparat.com/"))
                    player.setMediaSource(ProgressiveMediaSource.Factory(http).createMediaSource(MediaItem.fromUri(r.data)))
                    player.prepare(); player.playWhenReady = true
                    state = LoadState.Success(Unit)
                }
                is LoadState.Error -> state = r
                else -> {}
            }
        }
    }

    val playerView: @Composable (Modifier) -> Unit = { m ->
        AndroidView(modifier = m, factory = { c ->
            PlayerView(c).apply {
                this.player = player
                setFullscreenButtonClickListener { enter -> fs = enter }
            }
        })
    }
    Scaffold(
        topBar = {
            if (!fs) TopAppBar(
                title = { Text(title, color = TextPrimary, fontSize = 16.sp, maxLines = 1) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.Close, "بستن", tint = TextPrimary) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Surface)
            )
        },
        containerColor = if (fs) Color.Black else Background
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(if (fs) PaddingValues(0.dp) else padding).background(if (fs) Color.Black else Color.Transparent)) {
            when (val s = state) {
                is LoadState.Loading -> LoadingState()
                is LoadState.Error -> ErrorState(s, onRetry = { reloadKey++ }, onLogout = onLogout)
                is LoadState.Success -> {
                    if (fs) playerView(Modifier.fillMaxSize())
                    else playerView(Modifier.fillMaxWidth().aspectRatio(16 / 9f))
                }
            }
            if (fs) IconButton(onClick = onBack, modifier = Modifier.align(Alignment.TopStart).padding(8.dp)) {
                Icon(Icons.Default.Close, "بستن", tint = Color.White)
            }
        }
    }
}

package com.example.iptvplayer

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private const val PLAYLIST_URL = "https://raw.githubusercontent.com/Dimonovich/TV/Dimonovich/FREE/.m3u"
private const val UA = "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36"
private val BG = Color(0xFF080B10)
private val CARD = Color(0xFF121820)
private val PURPLE = Color(0xFFA855F7)
private val MUTED = Color(0xFF8D98A8)

data class Channel(val name: String, val group: String, val url: String, val userAgent: String = "")

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IPTVApp() }
    }
}

@Composable
private fun IPTVApp() {
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var selected by remember { mutableStateOf<Int?>(null) }
    var query by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        channels = withContext(Dispatchers.IO) { loadM3u() }
        loading = false
    }

    MaterialTheme(colorScheme = darkColorScheme(background = BG, surface = CARD, primary = PURPLE)) {
        if (selected != null && channels.isNotEmpty()) {
            PlayerScreen(
                channels = channels,
                index = selected!!,
                onSelect = { selected = it },
                onBack = { selected = null }
            )
        } else {
            Scaffold(containerColor = BG) { padding ->
                Column(Modifier.fillMaxSize().background(BG).padding(padding)) {
                    Text("IPTV Player", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(18.dp))
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        singleLine = true,
                        placeholder = { Text("Поиск канала", color = MUTED) }
                    )
                    if (loading) {
                        Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PURPLE) }
                    } else {
                        val filtered = channels.withIndex().filter { it.value.name.contains(query, true) || it.value.group.contains(query, true) }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(filtered) { _, item ->
                                val originalIndex = item.index
                                val c = item.value
                                Row(
                                    Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(14.dp)).clickable { selected = originalIndex }.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(Modifier.size(48.dp).background(Color(0xFF273144), RoundedCornerShape(12.dp)), Alignment.Center) {
                                        Text("${originalIndex + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Column(Modifier.padding(start = 12.dp).weight(1f)) {
                                        Text(c.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(c.group.ifBlank { "ТВ канал" }, color = MUTED, fontSize = 12.sp)
                                    }
                                    Text("▶", color = PURPLE)
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
private fun PlayerScreen(
    channels: List<Channel>,
    index: Int,
    onSelect: (Int) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val channel = channels[index]
    var fullscreen by remember { mutableStateOf(false) }
    var settings by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    val player = remember(channel.url, channel.userAgent) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(channel.userAgent.ifBlank { UA })
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory))
            .build()
            .apply {
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: PlaybackException) {
                        errorText = "Не удалось загрузить канал\n${error.errorCodeName}"
                    }
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_READY) errorText = null
                    }
                })
                setMediaItem(MediaItem.fromUri(channel.url))
                prepare()
            }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    BackHandler {
        when {
            settings -> settings = false
            fullscreen -> fullscreen = false
            else -> onBack()
        }
    }

    LaunchedEffect(fullscreen) {
        if (fullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VideoView(player, zoom = true, modifier = Modifier.fillMaxSize())
            PlayerButtons(
                player = player,
                fullscreen = true,
                onFullscreen = { fullscreen = false },
                onSettings = { settings = true },
                modifier = Modifier.align(Alignment.BottomEnd)
            )
            errorText?.let { ErrorMessage(it, Modifier.align(Alignment.Center)) }
        }
    } else {
        Column(Modifier.fillMaxSize().background(BG)) {
            Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("‹", color = Color.White, fontSize = 34.sp, modifier = Modifier.clickable { onBack() })
                Text(channel.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp).weight(1f))
            }
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                VideoView(player, zoom = false, modifier = Modifier.fillMaxSize())
                PlayerButtons(
                    player = player,
                    fullscreen = false,
                    onFullscreen = { fullscreen = true },
                    onSettings = { settings = true },
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
                errorText?.let { ErrorMessage(it, Modifier.align(Alignment.Center)) }
            }
            Text("${index + 1}. ${channel.name}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp))
            Text("Следующие каналы", color = MUTED, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
            LazyColumn(contentPadding = PaddingValues(12.dp, 10.dp, 12.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                itemsIndexed(channels.drop(index + 1)) { offset, next ->
                    val nextIndex = index + 1 + offset
                    Row(
                        Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(13.dp)).clickable { onSelect(nextIndex) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${nextIndex + 1}", color = PURPLE, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                        Column(Modifier.weight(1f)) {
                            Text(next.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(next.group.ifBlank { "ТВ канал" }, color = MUTED, fontSize = 11.sp)
                        }
                        Text("▶", color = PURPLE)
                    }
                }
            }
        }
    }

    if (settings) {
        AlertDialog(
            onDismissRequest = { settings = false },
            title = { Text("Настройки плеера") },
            text = { Text("User-Agent берётся из M3U автоматически.\n\nПеремотка и лишние кнопки управления отключены.", color = MUTED) },
            confirmButton = { TextButton(onClick = { settings = false }) { Text("Закрыть") } }
        )
    }
}

@Composable
private fun VideoView(player: ExoPlayer, zoom: Boolean, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            PlayerView(ctx).apply {
                this.player = player
                keepScreenOn = true
                useController = false
                resizeMode = if (zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
        update = {
            it.player = player
            it.useController = false
            it.resizeMode = if (zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT
        }
    )
}

@Composable
private fun PlayerButtons(
    player: ExoPlayer,
    fullscreen: Boolean,
    onFullscreen: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier
) {
    Row(modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(46.dp).background(Color.Black.copy(.65f), RoundedCornerShape(23.dp)).clickable { if (player.isPlaying) player.pause() else player.play() }, Alignment.Center) {
            Text(if (player.isPlaying) "Ⅱ" else "▶", color = Color.White, fontSize = 22.sp)
        }
        Box(Modifier.size(46.dp).background(Color.Black.copy(.65f), RoundedCornerShape(23.dp)).clickable { onSettings() }, Alignment.Center) {
            Text("⚙", color = Color.White, fontSize = 21.sp)
        }
        Box(Modifier.size(46.dp).background(Color.Black.copy(.65f), RoundedCornerShape(23.dp)).clickable { onFullscreen() }, Alignment.Center) {
            Text(if (fullscreen) "⛶" else "⛶", color = Color.White, fontSize = 23.sp)
        }
    }
}

@Composable
private fun ErrorMessage(text: String, modifier: Modifier) {
    Text(text, color = Color.White, modifier = modifier.padding(24.dp), fontSize = 14.sp)
}

private fun loadM3u(): List<Channel> = try {
    parseM3u(URL(PLAYLIST_URL).readText())
} catch (_: Exception) {
    emptyList()
}

private fun parseM3u(text: String): List<Channel> {
    val result = mutableListOf<Channel>()
    var name = ""
    var group = ""
    var ua = ""
    text.lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.startsWith("#EXTINF:", true) -> {
                name = line.substringAfterLast(',').trim()
                group = Regex("""group-title=\"([^\"]*)\"""", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1).orEmpty()
                ua = ""
            }
            line.startsWith("#EXTVLCOPT:http-user-agent=", true) -> {
                ua = line.substringAfter('=').trim()
            }
            line.startsWith("#") || line.isBlank() -> Unit
            else -> {
                if (name.isNotBlank()) result += Channel(name, group, line, ua)
                name = ""
                group = ""
                ua = ""
            }
        }
    }
    return result
}

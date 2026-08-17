package com.example.iptvplayer

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private const val PLAYLIST_URL = "https://raw.githubusercontent.com/Dimonovich/TV/Dimonovich/FREE/.m3u"
private const val IA_SEARCH = "https://archive.org/advancedsearch.php?q=mediatype%3Amovies%20AND%20%28licenseurl%3Acreativecommons.org%20OR%20licenseurl%3Apublicdomain%29&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=description&fl%5B%5D=year&fl%5B%5D=licenseurl&rows=60&page=1&output=json"
private const val UA = "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/130.0.0.0 Mobile Safari/537.36"
private val BG = Color(0xFF080B10)
private val CARD = Color(0xFF121820)
private val PURPLE = Color(0xFFA855F7)
private val MUTED = Color(0xFF8D98A8)
private val ICON_BG = Color(0xFF172131)

data class Channel(val name: String, val group: String, val url: String, val userAgent: String = "")
data class Movie(val id: String, val title: String, val year: String, val description: String, val license: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IPTVApp() }
    }
}

@Composable
private fun IPTVApp() {
    var page by remember { mutableStateOf("home") }
    var query by remember { mutableStateOf("") }
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var movieLoading by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { channels = withContext(Dispatchers.IO) { loadM3u() }; loading = false }
    LaunchedEffect(page) {
        if (page == "movies" && movies.isEmpty() && !movieLoading) {
            movieLoading = true
            movies = withContext(Dispatchers.IO) { loadMovies() }
            movieLoading = false
        }
    }
    MaterialTheme(colorScheme = darkColorScheme(background = BG, surface = CARD, primary = PURPLE)) {
        if (selected != null && channels.isNotEmpty()) {
            PlayerScreen(channels, selected!!, { selected = it }, { selected = null })
            return@MaterialTheme
        }
        Scaffold(containerColor = BG, bottomBar = { BottomBar(page) { page = it; query = "" } }) { padding ->
            Column(Modifier.fillMaxSize().background(BG).padding(padding)) {
                Text("IPTV Player", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(18.dp))
                Box(Modifier.weight(1f)) {
                    when (page) {
                        "channels" -> Channels(channels, query, { query = it }, loading) { selected = it }
                        "movies" -> Movies(movies, query, { query = it }, movieLoading)
                        else -> Home({ page = "channels" }, { page = "movies" })
                    }
                }
            }
        }
    }
}

@Composable
private fun Home(tv: () -> Unit, movie: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Box(
            Modifier.fillMaxWidth().height(150.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF6D28D9), Color(0xFF172B8A))), RoundedCornerShape(22.dp))
                .padding(20.dp)
        ) {
            Column {
                Text("TV", color = Color.White.copy(.28f), fontSize = 54.sp, fontWeight = FontWeight.Black)
                Text("Смотрите любимые каналы", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("и доступное кино", color = Color.White.copy(.8f))
            }
        }
        Spacer(Modifier.height(18.dp))
        HomeBtn("tv", "ТВ каналы", "Ваш M3U плейлист", tv)
        HomeBtn("movie", "Фильмы", "Internet Archive · открытые лицензии", movie)
    }
}

@Composable
private fun HomeBtn(kind: String, title: String, sub: String, on: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 5.dp).background(CARD, RoundedCornerShape(16.dp)).clickable(onClick = on).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ThreeDHomeIcon(kind, Modifier.size(62.dp))
        Column(Modifier.padding(start = 8.dp).weight(1f)) {
            Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(sub, color = MUTED, fontSize = 12.sp)
        }
        Text("›", color = PURPLE, fontSize = 28.sp)
    }
}

@Composable
private fun ThreeDHomeIcon(kind: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(ICON_BG), contentAlignment = Alignment.Center) {
        if (kind == "tv") ChannelStack3D() else Film3D()
    }
}

@Composable
private fun ChannelStack3D() {
    Box(Modifier.fillMaxSize()) {
        MiniChannelTile("1", Color(0xFF2E6CD4), 5.dp, 10.dp, -12f)
        MiniChannelTile("М", Color(0xFFE53935), 29.dp, 5.dp, 8f)
        MiniChannelTile("НТВ", Color(0xFF1E8C47), 10.dp, 30.dp, -5f)
        MiniChannelTile("ТВЦ", Color(0xFF514A92), 36.dp, 29.dp, 10f)
        MiniChannelTile("5", Color(0xFFD7DCE8), 25.dp, 47.dp, -4f, Color(0xFF5B2A86))
    }
}

@Composable
private fun MiniChannelTile(label: String, color: Color, x: Dp, y: Dp, rotation: Float, textColor: Color = Color.White) {
    Box(
        Modifier.offset(x, y).size(28.dp)
            .background(Brush.linearGradient(listOf(color.copy(alpha = .98f), color.copy(alpha = .55f))), RoundedCornerShape(7.dp))
            .shadow(4.dp, RoundedCornerShape(7.dp))
            .graphicsLayer { rotationZ = rotation },
        Alignment.Center
    ) {
        Box(Modifier.fillMaxSize().padding(1.dp).background(Color.Black.copy(.16f), RoundedCornerShape(6.dp)), Alignment.Center) {
            Text(label, color = textColor, fontSize = if (label.length > 2) 6.sp else 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
private fun Film3D() {
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(Color.Black.copy(.35f), radius = 18f, center = Offset(25f, 45f))
        drawCircle(Color(0xFF6D7689), radius = 17f, center = Offset(23f, 42f))
        drawCircle(Color(0xFF192231), radius = 13f, center = Offset(23f, 42f))
        for (hole in listOf(Offset(23f, 34f), Offset(30f, 42f), Offset(23f, 50f), Offset(16f, 42f))) drawCircle(Color(0xFF7A8AA0), radius = 2.6f, center = hole)
        drawRoundRect(Brush.linearGradient(listOf(Color(0xFF202A38), Color(0xFF6E7D91))), Offset(6f, 23f), Size(40f, 8f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f))
        drawLine(Color.White.copy(.75f), Offset(11f, 24f), Offset(15f, 30f), strokeWidth = 2.5f)
        drawLine(Color.White.copy(.75f), Offset(21f, 24f), Offset(25f, 30f), strokeWidth = 2.5f)
        drawLine(Color.White.copy(.75f), Offset(31f, 24f), Offset(35f, 30f), strokeWidth = 2.5f)
        drawCircle(Color(0xFFF2D0A1), radius = 4.5f, center = Offset(51f, 14f))
        drawLine(Color(0xFFF2D0A1), Offset(49f, 18f), Offset(41f, 29f), strokeWidth = 3.6f)
        drawLine(Color(0xFFF2D0A1), Offset(41f, 29f), Offset(33f, 24f), strokeWidth = 3.2f)
        drawLine(Color(0xFFF2D0A1), Offset(41f, 29f), Offset(51f, 26f), strokeWidth = 3.2f)
        drawLine(Color(0xFFF2D0A1), Offset(41f, 30f), Offset(34f, 41f), strokeWidth = 3.5f)
        drawLine(Color(0xFFF2D0A1), Offset(42f, 30f), Offset(52f, 38f), strokeWidth = 3.5f)
        drawCircle(Color(0xFF6D28D9).copy(.45f), radius = 3f, center = Offset(51f, 14f))
    }
}

@Composable
private fun Channels(channels: List<Channel>, q: String, onQ: (String) -> Unit, loading: Boolean, onPlay: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(q, onQ, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Поиск канала", color = MUTED) })
        if (loading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PURPLE) }
        else {
            val list = channels.withIndex().filter { it.value.name.contains(q, true) || it.value.group.contains(q, true) }
            LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { itemsIndexed(list) { _, item -> ChannelRow(item.index, item.value, onPlay) } }
        }
    }
}

@Composable
private fun ChannelRow(index: Int, c: Channel, on: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(14.dp)).clickable { on(index) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(ICON_BG, RoundedCornerShape(11.dp)), Alignment.Center) { ChannelLogo(index, c.name) }
        Column(Modifier.padding(start = 10.dp).weight(1f)) { Text(c.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(c.group.ifBlank { "ТВ канал" }, color = MUTED, fontSize = 12.sp) }
        Text("▶", color = PURPLE)
    }
}

@Composable
private fun ChannelLogo(index: Int, name: String) {
    val palettes = listOf(
        listOf(Color(0xFF2E6CD4), Color(0xFF203C86)), listOf(Color(0xFF18A85C), Color(0xFF0E6D42)), listOf(Color(0xFFE53935), Color(0xFF9E2222)),
        listOf(Color(0xFF4E7EA8), Color(0xFF243A5C)), listOf(Color(0xFF5E9AD6), Color(0xFF284465)), listOf(Color(0xFFE34B32), Color(0xFF9B271A)), listOf(Color(0xFFF08A22), Color(0xFF9B4E0B))
    )
    val colors = palettes[index % palettes.size]
    val short = when { name.contains("Крым", true) -> "24"; name.contains("Югра", true) -> "Ю"; name.contains("Липец", true) -> "◷"; name.contains("4 канал", true) -> "4"; name.length > 5 -> name.take(2).uppercase(); else -> name.take(1).uppercase() }
    Box(Modifier.size(30.dp).background(Brush.linearGradient(colors), RoundedCornerShape(8.dp)).graphicsLayer { rotationZ = if (index % 2 == 0) -3f else 3f }, Alignment.Center) {
        Text(short, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = if (short.length > 1) 10.sp else 14.sp)
    }
}

@Composable
private fun PlayerScreen(channels: List<Channel>, index: Int, onSelect: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val channel = channels[index]
    var fullscreen by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    val player = remember(channel.url, channel.userAgent) {
        val httpFactory = DefaultHttpDataSource.Factory().setUserAgent(channel.userAgent.ifBlank { UA }).setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(15000).setReadTimeoutMs(20000)
        ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory)).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) { errorText = "Не удалось загрузить канал\n${error.errorCodeName}" }
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
                override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) errorText = null }
            })
            setMediaItem(MediaItem.fromUri(channel.url)); prepare()
        }
    }
    DisposableEffect(player) { isPlaying = player.isPlaying; onDispose { player.release(); activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT } }
    BackHandler { if (fullscreen) fullscreen = false else onBack() }
    LaunchedEffect(fullscreen) {
        if (fullscreen) { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE; WindowCompat.setDecorFitsSystemWindows(activity.window, false); WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply { hide(WindowInsetsCompat.Type.systemBars()); systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE } }
        else { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; WindowCompat.setDecorFitsSystemWindows(activity.window, true); WindowInsetsControllerCompat(activity.window, activity.window.decorView).show(WindowInsetsCompat.Type.systemBars()) }
    }
    if (fullscreen) Box(Modifier.fillMaxSize().background(Color.Black)) {
        VideoView(player, true, Modifier.fillMaxSize()); PlayerButtons(player, isPlaying, { fullscreen = false }, Modifier.align(Alignment.BottomEnd)); errorText?.let { ErrorMessage(it, Modifier.align(Alignment.Center)) }
    } else Column(Modifier.fillMaxSize().background(BG)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
            VideoView(player, false, Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.TopStart).padding(10.dp).size(42.dp).background(Color.Black.copy(.5f), CircleShape).clickable { onBack() }, Alignment.Center) { Text("‹", color = Color.White, fontSize = 28.sp) }
            PlayerButtons(player, isPlaying, { fullscreen = true }, Modifier.align(Alignment.BottomEnd)); errorText?.let { ErrorMessage(it, Modifier.align(Alignment.Center)) }
        }
        Text("${index + 1}. ${channel.name}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp))
        Text("Следующие каналы", color = MUTED, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(contentPadding = PaddingValues(12.dp, 10.dp, 12.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            itemsIndexed(channels.drop(index + 1)) { offset, next -> val nextIndex = index + 1 + offset; Row(Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(13.dp)).clickable { onSelect(nextIndex) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("${nextIndex + 1}", color = PURPLE, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp)); Column(Modifier.weight(1f)) { Text(next.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(next.group.ifBlank { "ТВ канал" }, color = MUTED, fontSize = 11.sp) }; Text("▶", color = PURPLE)
            } }
        }
    }
}

@Composable
private fun VideoView(player: ExoPlayer, zoom: Boolean, modifier: Modifier) {
    AndroidView(modifier = modifier, factory = { ctx -> PlayerView(ctx).apply { this.player = player; keepScreenOn = true; useController = false; resizeMode = if (zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT } }, update = { it.player = player; it.useController = false; it.resizeMode = if (zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT })
}

@Composable
private fun PlayerButtons(player: ExoPlayer, isPlaying: Boolean, onFullscreen: () -> Unit, modifier: Modifier) {
    Row(modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(46.dp).background(Color.Black.copy(.78f), CircleShape).clickable { if (player.isPlaying) player.pause() else player.play() }, Alignment.Center) {
            Canvas(Modifier.size(46.dp)) {
                if (isPlaying) { drawRoundRect(Color.White, Offset(16f, 14f), Size(5f, 18f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f, 2.5f)); drawRoundRect(Color.White, Offset(25f, 14f), Size(5f, 18f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f, 2.5f)) }
                else { val p = Path().apply { moveTo(18f, 13f); lineTo(18f, 33f); lineTo(33f, 23f); close() }; drawPath(p, Color.White) }
            }
        }
        Box(Modifier.size(40.dp).background(Color.Black.copy(.65f), CircleShape).clickable { onFullscreen() }, Alignment.Center) {
            Canvas(Modifier.size(40.dp)) {
                val w = 2.7f
                drawLine(Color.White, Offset(12f, 17f), Offset(12f, 12f), strokeWidth = w); drawLine(Color.White, Offset(12f, 12f), Offset(17f, 12f), strokeWidth = w); drawLine(Color.White, Offset(23f, 12f), Offset(28f, 12f), strokeWidth = w); drawLine(Color.White, Offset(28f, 12f), Offset(28f, 17f), strokeWidth = w)
                drawLine(Color.White, Offset(12f, 24f), Offset(12f, 29f), strokeWidth = w); drawLine(Color.White, Offset(12f, 29f), Offset(17f, 29f), strokeWidth = w); drawLine(Color.White, Offset(23f, 29f), Offset(28f, 29f), strokeWidth = w); drawLine(Color.White, Offset(28f, 29f), Offset(28f, 24f), strokeWidth = w)
            }
        }
    }
}

@Composable private fun ErrorMessage(text: String, modifier: Modifier) { Text(text, color = Color.White, modifier = modifier.padding(24.dp), fontSize = 14.sp) }

@Composable
private fun Movies(movies: List<Movie>, q: String, onQ: (String) -> Unit, loading: Boolean) {
    val filtered = movies.filter { it.title.contains(q, true) || it.description.contains(q, true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(q, onQ, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Поиск фильмов", color = MUTED) })
        Spacer(Modifier.height(12.dp)); Text("Internet Archive", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Public Domain / Creative Commons", color = MUTED, fontSize = 12.sp)
        when {
            loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PURPLE) }
            filtered.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Каталог пуст или источник недоступен", color = MUTED) }
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) { itemsIndexed(filtered) { _, movie -> MovieCard(movie) } }
        }
    }
}

@Composable
private fun MovieCard(movie: Movie) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(16.dp)).clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://archive.org/details/${movie.id}"))) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = "https://archive.org/services/img/${movie.id}", contentDescription = movie.title, modifier = Modifier.width(88.dp).height(118.dp).background(Color(0xFF222A35), RoundedCornerShape(12.dp)))
        Column(Modifier.padding(start = 14.dp).weight(1f)) { Text(movie.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis); if (movie.year.isNotBlank()) Text(movie.year, color = PURPLE, fontSize = 12.sp); Text(movie.description.ifBlank { "Открыть карточку фильма в Internet Archive" }, color = MUTED, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis); Text("▶ Открыть и смотреть", color = PURPLE, fontSize = 13.sp, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun NavIcon(id: String, selected: Boolean) {
    val tint = if (selected) Color.White else Color(0xFFC7CDD8)
    Canvas(Modifier.size(24.dp)) {
        when (id) {
            "home" -> { val roof = Path().apply { moveTo(4f, 11f); lineTo(12f, 4f); lineTo(20f, 11f) }; drawPath(roof, tint, style = Stroke(width = 2.2f)); drawRoundRect(tint, Offset(6f, 10f), Size(12f, 9f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f, 1.5f), style = Stroke(width = 2.2f)) }
            "channels" -> { drawRoundRect(tint, Offset(3f, 5f), Size(18f, 13f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f), style = Stroke(width = 2.2f)); drawCircle(PURPLE, 2f, Offset(12f, 11.5f)) }
            "movies" -> { drawRoundRect(tint, Offset(3f, 5f), Size(18f, 14f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.5f, 2.5f), style = Stroke(width = 2.2f)); drawLine(tint, Offset(6f, 5f), Offset(8f, 9f), strokeWidth = 2f); drawLine(tint, Offset(10f, 5f), Offset(12f, 9f), strokeWidth = 2f); drawLine(tint, Offset(14f, 5f), Offset(16f, 9f), strokeWidth = 2f); val play = Path().apply { moveTo(10f, 11f); lineTo(10f, 16f); lineTo(15f, 13.5f); close() }; drawPath(play, PURPLE) }
            else -> { drawLine(tint, Offset(4f, 6f), Offset(20f, 6f), strokeWidth = 2.2f); drawLine(tint, Offset(4f, 12f), Offset(20f, 12f), strokeWidth = 2.2f); drawLine(tint, Offset(4f, 18f), Offset(20f, 18f), strokeWidth = 2.2f) }
        }
    }
}

@Composable
private fun BottomBar(page: String, onPage: (String) -> Unit) {
    NavigationBar(containerColor = Color(0xFF0D1117)) {
        listOf(Triple("home", "Главная"), Triple("channels", "ТВ"), Triple("movies", "Фильмы"), Triple("settings", "Ещё")).forEach { (id, label) -> NavigationBarItem(selected = page == id, onClick = { onPage(id) }, icon = { NavIcon(id, page == id) }, label = { Text(label, fontSize = 10.sp) }) }
    }
}

private fun loadM3u(): List<Channel> = try { parseM3u(URL(PLAYLIST_URL).readText()) } catch (_: Exception) { emptyList() }
private fun parseM3u(text: String): List<Channel> {
    val result = mutableListOf<Channel>(); var name = ""; var group = ""; var ua = ""
    text.lineSequence().forEach { raw -> val line = raw.trim(); when {
        line.startsWith("#EXTINF:", true) -> { name = line.substringAfterLast(',').trim(); group = Regex("""group-title=\"([^\"]*)\"""", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1).orEmpty(); ua = "" }
        line.startsWith("#EXTVLCOPT:http-user-agent=", true) -> ua = line.substringAfter('=').trim()
        line.startsWith("#") || line.isBlank() -> Unit
        else -> { if (name.isNotBlank()) result += Channel(name, group, line, ua); name = ""; group = ""; ua = "" }
    } }
    return result
}
private fun loadMovies(): List<Movie> = try {
    val docs = JSONObject(URL(IA_SEARCH).readText()).getJSONObject("response").getJSONArray("docs")
    buildList { for (i in 0 until docs.length()) { val item = docs.getJSONObject(i); val id = item.optString("identifier"); val title = item.optString("title"); val license = item.optString("licenseurl"); if (id.isNotBlank() && title.isNotBlank() && (license.contains("creativecommons.org", true) || license.contains("publicdomain", true))) add(Movie(id, title, item.optString("year"), item.optString("description"), license)) } }.distinctBy { it.id }
} catch (_: Exception) { emptyList() }

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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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
        if (page == "movies" && movies.isEmpty() && !movieLoading) { movieLoading = true; movies = withContext(Dispatchers.IO) { loadMovies() }; movieLoading = false }
    }
    MaterialTheme(colorScheme = darkColorScheme(background = BG, surface = CARD, primary = PURPLE)) {
        if (selected != null && channels.isNotEmpty()) { PlayerScreen(channels, selected!!, { selected = it }, { selected = null }); return@MaterialTheme }
        Scaffold(containerColor = BG, bottomBar = { BottomBar(page) { page = it; query = "" } }) { padding ->
            Column(Modifier.fillMaxSize().background(BG).padding(padding)) {
                Text("IPTV Player", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(18.dp))
                Box(Modifier.weight(1f)) {
                    when (page) { "channels" -> Channels(channels, query, { query = it }, loading) { selected = it }; "movies" -> Movies(movies, query, { query = it }, movieLoading); else -> Home({ page = "channels" }, { page = "movies" }) }
                }
            }
        }
    }
}

@Composable private fun Home(tv: () -> Unit, movie: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.linearGradient(listOf(Color(0xFF6D28D9), Color(0xFF172B8A))), RoundedCornerShape(22.dp)).padding(20.dp)) { Column { Text("TV", color = Color.White.copy(.28f), fontSize = 54.sp, fontWeight = FontWeight.Black); Text("Смотрите любимые каналы", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("и доступное кино", color = Color.White.copy(.8f)) } }
        Spacer(Modifier.height(18.dp)); HomeBtn("📺", "ТВ каналы", "Ваш M3U плейлист", tv); HomeBtn("🎬", "Фильмы", "Internet Archive · открытые лицензии", movie)
    }
}
@Composable private fun HomeBtn(icon: String, title: String, sub: String, on: () -> Unit) { Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).background(CARD, RoundedCornerShape(16.dp)).clickable(onClick = on).padding(16.dp), verticalAlignment = Alignment.CenterVertically) { Text(icon, fontSize = 30.sp); Column(Modifier.padding(start = 14.dp).weight(1f)) { Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text(sub, color = MUTED, fontSize = 12.sp) }; Text("›", color = PURPLE, fontSize = 28.sp) } }
@Composable private fun Channels(channels: List<Channel>, q: String, onQ: (String) -> Unit, loading: Boolean, onPlay: (Int) -> Unit) { Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) { OutlinedTextField(q, onQ, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Поиск канала", color = MUTED) }); if (loading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PURPLE) } else { val list = channels.withIndex().filter { it.value.name.contains(q, true) || it.value.group.contains(q, true) }; LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { itemsIndexed(list) { _, item -> ChannelRow(item.index, item.value, onPlay) } } } } }
@Composable private fun ChannelRow(index: Int, c: Channel, on: (Int) -> Unit) { Row(Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(14.dp)).clickable { on(index) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(48.dp).background(Color(0xFF273144), RoundedCornerShape(12.dp)), Alignment.Center) { Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold) }; Column(Modifier.padding(start = 12.dp).weight(1f)) { Text(c.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(c.group.ifBlank { "ТВ канал" }, color = MUTED, fontSize = 12.sp) }; Text("▶", color = PURPLE) } }

@Composable private fun PlayerScreen(channels: List<Channel>, index: Int, onSelect: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current; val activity = context as ComponentActivity; val channel = channels[index]
    var fullscreen by remember { mutableStateOf(false) }; var errorText by remember { mutableStateOf<String?>(null) }; var isPlaying by remember { mutableStateOf(true) }
    val player = remember(channel.url, channel.userAgent) {
        val httpFactory = DefaultHttpDataSource.Factory().setUserAgent(channel.userAgent.ifBlank { UA }).setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(15000).setReadTimeoutMs(20000)
        ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory)).build().apply { playWhenReady = true; addListener(object : Player.Listener { override fun onPlayerError(error: PlaybackException) { errorText = "Не удалось загрузить канал\n${error.errorCodeName}" }; override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }; override fun onPlaybackStateChanged(state: Int) { if (state == Player.STATE_READY) errorText = null } }); setMediaItem(MediaItem.fromUri(channel.url)); prepare() }
    }
    DisposableEffect(player) { isPlaying = player.isPlaying; onDispose { player.release(); activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT } }
    BackHandler { if (fullscreen) fullscreen = false else onBack() }
    LaunchedEffect(fullscreen) { if (fullscreen) { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE; WindowCompat.setDecorFitsSystemWindows(activity.window, false); WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply { hide(WindowInsetsCompat.Type.systemBars()); systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE } } else { activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT; WindowCompat.setDecorFitsSystemWindows(activity.window, true); WindowInsetsControllerCompat(activity.window, activity.window.decorView).show(WindowInsetsCompat.Type.systemBars()) } }
    if (fullscreen) Box(Modifier.fillMaxSize().background(Color.Black)) { VideoView(player, true, Modifier.fillMaxSize()); PlayerButtons(player, isPlaying, true, { fullscreen = false }, Modifier.align(Alignment.BottomEnd)); errorText?.let { ErrorMessage(it, Modifier.align(Alignment.Center)) } }
    else Column(Modifier.fillMaxSize().background(BG)) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) { VideoView(player, false, Modifier.fillMaxSize()); Box(Modifier.align(Alignment.TopStart).padding(10.dp).size(42.dp).background(Color.Black.copy(.5f), RoundedCornerShape(21.dp)).clickable { onBack() }, Alignment.Center) { Text("‹", color = Color.White, fontSize = 28.sp) }; PlayerButtons(player, isPlaying, false, { fullscreen = true }, Modifier.align(Alignment.BottomEnd)); errorText?.let { ErrorMessage(it, Modifier.align(Alignment.Center)) } }
        Text("${index + 1}. ${channel.name}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 4.dp)); Text("Следующие каналы", color = MUTED, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(contentPadding = PaddingValues(12.dp, 10.dp, 12.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { itemsIndexed(channels.drop(index + 1)) { offset, next -> val nextIndex = index + 1 + offset; Row(Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(13.dp)).clickable { onSelect(nextIndex) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text("${nextIndex + 1}", color = PURPLE, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp)); Column(Modifier.weight(1f)) { Text(next.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(next.group.ifBlank { "ТВ канал" }, color = MUTED, fontSize = 11.sp) }; Text("▶", color = PURPLE) } } }
    }
}

@Composable private fun VideoView(player: ExoPlayer, zoom: Boolean, modifier: Modifier) { AndroidView(modifier = modifier, factory = { ctx -> PlayerView(ctx).apply { this.player = player; keepScreenOn = true; useController = false; resizeMode = if (zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT } }, update = { it.player = player; it.useController = false; it.resizeMode = if (zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT }) }

@Composable private fun PlayerButtons(player: ExoPlayer, isPlaying: Boolean, fullscreen: Boolean, onFullscreen: () -> Unit, modifier: Modifier) {
    Row(modifier.padding(12.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(54.dp).background(Color.Black.copy(.78f), CircleShape).clickable { if (player.isPlaying) player.pause() else player.play() }, Alignment.Center) {
            Canvas(Modifier.size(54.dp)) {
                if (isPlaying) {
                    drawRoundRect(Color.White, left = 19f, top = 16f, right = 25f, bottom = 38f, cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f))
                    drawRoundRect(Color.White, left = 29f, top = 16f, right = 35f, bottom = 38f, cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f))
                } else {
                    val p = Path().apply { moveTo(21f, 15f); lineTo(21f, 39f); lineTo(39f, 27f); close() }
                    drawPath(p, Color.White)
                }
            }
        }
        Box(Modifier.size(46.dp).background(Color.Black.copy(.65f), CircleShape).clickable { onFullscreen() }, Alignment.Center) {
            Canvas(Modifier.size(46.dp)) {
                val w = 3.2f
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(14f, 20f), androidx.compose.ui.geometry.Offset(14f, 14f), strokeWidth = w)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(14f, 14f), androidx.compose.ui.geometry.Offset(20f, 14f), strokeWidth = w)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(32f, 14f), androidx.compose.ui.geometry.Offset(38f, 14f), strokeWidth = w)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(38f, 14f), androidx.compose.ui.geometry.Offset(38f, 20f), strokeWidth = w)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(14f, 34f), androidx.compose.ui.geometry.Offset(14f, 40f), strokeWidth = w)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(14f, 40f), androidx.compose.ui.geometry.Offset(20f, 40f), strokeWidth = w)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(32f, 40f), androidx.compose.ui.geometry.Offset(38f, 40f), strokeWidth = w)
                drawLine(Color.White, androidx.compose.ui.geometry.Offset(38f, 40f), androidx.compose.ui.geometry.Offset(38f, 34f), strokeWidth = w)
            }
        }
    }
}
@Composable private fun ErrorMessage(text: String, modifier: Modifier) { Text(text, color = Color.White, modifier = modifier.padding(24.dp), fontSize = 14.sp) }
@Composable private fun Movies(movies: List<Movie>, q: String, onQ: (String) -> Unit, loading: Boolean) { val filtered = movies.filter { it.title.contains(q, true) || it.description.contains(q, true) }; Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) { OutlinedTextField(q, onQ, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Поиск фильмов", color = MUTED) }); Spacer(Modifier.height(12.dp)); Text("🎬 Internet Archive", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Public Domain / Creative Commons", color = MUTED, fontSize = 12.sp); when { loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PURPLE) }; filtered.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Каталог пуст или источник недоступен", color = MUTED) }; else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) { itemsIndexed(filtered) { _, movie -> MovieCard(movie) } } } } }
@Composable private fun MovieCard(movie: Movie) { val context = LocalContext.current; Row(Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(16.dp)).clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://archive.org/details/${movie.id}"))) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) { AsyncImage(model = "https://archive.org/services/img/${movie.id}", contentDescription = movie.title, modifier = Modifier.width(88.dp).height(118.dp).background(Color(0xFF222A35), RoundedCornerShape(12.dp))); Column(Modifier.padding(start = 14.dp).weight(1f)) { Text(movie.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis); if (movie.year.isNotBlank()) Text(movie.year, color = PURPLE, fontSize = 12.sp); Text(movie.description.ifBlank { "Открыть карточку фильма в Internet Archive" }, color = MUTED, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis); Text("▶ Открыть и смотреть", color = PURPLE, fontSize = 13.sp, fontWeight = FontWeight.Bold) } } }
@Composable private fun BottomBar(page: String, onPage: (String) -> Unit) { NavigationBar(containerColor = Color(0xFF0D1117)) { listOf(Triple("home", "⌂", "Главная"), Triple("channels", "▣", "ТВ"), Triple("movies", "🎬", "Фильмы"), Triple("settings", "⚙", "Ещё")).forEach { (id, icon, label) -> NavigationBarItem(selected = page == id, onClick = { onPage(id) }, icon = { Text(icon, fontSize = 20.sp) }, label = { Text(label, fontSize = 10.sp) }) } } }
private fun loadM3u(): List<Channel> = try { parseM3u(URL(PLAYLIST_URL).readText()) } catch (_: Exception) { emptyList() }
private fun parseM3u(text: String): List<Channel> { val result = mutableListOf<Channel>(); var name = ""; var group = ""; var ua = ""; text.lineSequence().forEach { raw -> val line = raw.trim(); when { line.startsWith("#EXTINF:", true) -> { name = line.substringAfterLast(',').trim(); group = Regex("""group-title=\"([^\"]*)\"""", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1).orEmpty(); ua = "" }; line.startsWith("#EXTVLCOPT:http-user-agent=", true) -> ua = line.substringAfter('=').trim(); line.startsWith("#") || line.isBlank() -> Unit; else -> { if (name.isNotBlank()) result += Channel(name, group, line, ua); name = ""; group = ""; ua = "" } } }; return result }
private fun loadMovies(): List<Movie> = try { val docs = JSONObject(URL(IA_SEARCH).readText()).getJSONObject("response").getJSONArray("docs"); buildList { for (i in 0 until docs.length()) { val item = docs.getJSONObject(i); val id = item.optString("identifier"); val title = item.optString("title"); val license = item.optString("licenseurl"); if (id.isNotBlank() && title.isNotBlank() && (license.contains("creativecommons.org", true) || license.contains("publicdomain", true))) add(Movie(id, title, item.optString("year"), item.optString("description"), license)) } }.distinctBy { it.id } } catch (_: Exception) { emptyList() }

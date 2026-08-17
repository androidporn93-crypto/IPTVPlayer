package com.example.iptvplayer

import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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

data class Channel(val name: String, val group: String, val url: String, val userAgent: String = "", val logo: String = "")
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

    LaunchedEffect(Unit) {
        channels = withContext(Dispatchers.IO) { loadM3u() }
        loading = false
    }
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
                        "favorites" -> FavoritesPage()
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
        Modifier.fillMaxWidth().height(124.dp).padding(vertical = 5.dp)
            .background(CARD, RoundedCornerShape(16.dp)).clickable(onClick = on).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (kind == "movie") {
            Image(
                painter = painterResource(R.drawable.home_movies_icon),
                contentDescription = "Фильмы",
                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(14.dp))
            )
        } else {
            ThreeDHomeIcon(kind, Modifier.size(94.dp))
        }
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(sub, color = MUTED, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = PURPLE, fontSize = 34.sp)
    }
}

@Composable
private fun ThreeDHomeIcon(kind: String, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(16.dp)).background(ICON_BG), contentAlignment = Alignment.Center) {
        ChannelStack3D()
    }
}

@Composable
private fun ChannelStack3D() {
    Box(Modifier.fillMaxSize()) {
        MiniChannelTile("1", Color(0xFF2E6CD4), 8.dp, 12.dp, -12f)
        MiniChannelTile("М", Color(0xFFE53935), 36.dp, 8.dp, 8f)
        MiniChannelTile("НТВ", Color(0xFF1E8C47), 14.dp, 40.dp, -5f)
        MiniChannelTile("ТВЦ", Color(0xFF514A92), 44.dp, 38.dp, 10f)
        MiniChannelTile("5", Color(0xFFD7DCE8), 28.dp, 66.dp, -4f, Color(0xFF5B2A86))
    }
}

@Composable
private fun MiniChannelTile(label: String, color: Color, x: Dp, y: Dp, rotation: Float, textColor: Color = Color.White) {
    Box(Modifier.offset(x, y).size(32.dp).background(Brush.linearGradient(listOf(color.copy(.98f), color.copy(.55f))), RoundedCornerShape(8.dp)), Alignment.Center) {
        Text(label, color = textColor, fontSize = if (label.length > 2) 7.sp else 12.sp, fontWeight = FontWeight.ExtraBold)
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
private fun ChannelAvatar(index: Int, channel: Channel, outerSize: Dp = 56.dp, innerSize: Dp = 40.dp) {
    Box(Modifier.size(outerSize).background(ICON_BG, RoundedCornerShape(14.dp)), Alignment.Center) {
        if (channel.logo.isNotBlank()) AsyncImage(model = channel.logo, contentDescription = channel.name, modifier = Modifier.size(innerSize).clip(RoundedCornerShape(10.dp)))
        else ChannelLogo(index, channel.name, innerSize)
    }
}

@Composable
private fun ChannelRow(index: Int, c: Channel, on: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(82.dp).background(CARD, RoundedCornerShape(14.dp)).clickable { on(index) }.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        ChannelAvatar(index, c)
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(c.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(c.group.ifBlank { "ТВ канал" }, color = MUTED, fontSize = 12.sp)
        }
        Text("▶", color = PURPLE, fontSize = 21.sp)
    }
}

@Composable
private fun ChannelLogo(index: Int, name: String, size: Dp = 40.dp) {
    val palettes = listOf(
        listOf(Color(0xFF2E6CD4), Color(0xFF203C86)), listOf(Color(0xFF18A85C), Color(0xFF0E6D42)), listOf(Color(0xFFE53935), Color(0xFF9E2222)),
        listOf(Color(0xFF4E7EA8), Color(0xFF243A5C)), listOf(Color(0xFF5E9AD6), Color(0xFF284465)), listOf(Color(0xFFE34B32), Color(0xFF9B271A)), listOf(Color(0xFFF08A22), Color(0xFF9B4E0B))
    )
    val colors = palettes[index % palettes.size]
    val short = when {
        name.contains("Крым", true) -> "24"
        name.contains("Югра", true) -> "Ю"
        name.contains("Липец", true) -> "◷"
        name.contains("4 канал", true) -> "4"
        name.contains("НТВ", true) -> "НТ"
        name.contains("Россия", true) -> "РО"
        name.length > 5 -> name.take(2).uppercase()
        else -> name.take(1).uppercase()
    }
    Box(Modifier.size(size).background(Brush.linearGradient(colors), RoundedCornerShape(10.dp)), Alignment.Center) { Text(short, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = if (short.length > 1) 11.sp else 15.sp) }
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
            })
            setMediaItem(MediaItem.fromUri(channel.url)); prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release(); activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT } }
    BackHandler { if (fullscreen) fullscreen = false else onBack() }
    LaunchedEffect(fullscreen) {
        if (fullscreen) {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            WindowInsetsControllerCompat(activity.window, activity.window.decorView).hide(WindowInsetsCompat.Type.systemBars())
        } else {
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
            WindowInsetsControllerCompat(activity.window, activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
        }
    }
    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            VideoView(player, true, Modifier.fillMaxSize())
            Box(Modifier.align(Alignment.Center)) { PlayerButtons(player, isPlaying, { fullscreen = false }, Modifier) }
            errorText?.let { ErrorMessage(it, Modifier.align(Alignment.Center).padding(top = 64.dp)) }
        }
    } else {
        Column(Modifier.fillMaxSize().background(BG)) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
                VideoView(player, false, Modifier.fillMaxSize())
                Box(Modifier.align(Alignment.TopStart).padding(10.dp).size(40.dp).background(Color.Black.copy(.5f), CircleShape).clickable { onBack() }, Alignment.Center) { Text("‹", color = Color.White, fontSize = 26.sp) }
                PlayerButtons(player, isPlaying, { fullscreen = true }, Modifier.align(Alignment.BottomEnd))
                errorText?.let { ErrorMessage(it, Modifier.align(Alignment.Center)) }
            }
            Text("${index + 1}. ${channel.name}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 3.dp))
            Text("Следующие каналы", color = MUTED, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
            LazyColumn(contentPadding = PaddingValues(12.dp, 10.dp, 12.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                itemsIndexed(channels.drop(index + 1)) { offset, next ->
                    val nextIndex = index + 1 + offset
                    Row(Modifier.fillMaxWidth().height(68.dp).background(CARD, RoundedCornerShape(12.dp)).clickable { onSelect(nextIndex) }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${nextIndex + 1}", color = PURPLE, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                        ChannelAvatar(nextIndex, next, 46.dp, 34.dp)
                        Column(Modifier.padding(start = 10.dp).weight(1f)) {
                            Text(next.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(next.group.ifBlank { "ТВ канал" }, color = MUTED, fontSize = 10.sp)
                        }
                        Box(Modifier.size(34.dp).background(Color(0xFF4C1D95), CircleShape), Alignment.Center) { Text("▶", color = Color.White, fontSize = 13.sp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun VideoView(player: ExoPlayer, zoom: Boolean, modifier: Modifier) {
    AndroidView(modifier = modifier, factory = { ctx -> PlayerView(ctx).apply { this.player = player; keepScreenOn = true; useController = false; resizeMode = if (zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT } }, update = { it.player = player; it.useController = false; it.resizeMode = if (zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT })
}

@Composable
private fun PlayerButtons(player: ExoPlayer, isPlaying: Boolean, onFullscreen: () -> Unit, modifier: Modifier) {
    Row(modifier.padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(42.dp).background(Color.Black.copy(.70f), CircleShape).clickable { if (player.isPlaying) player.pause() else player.play() }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(20.dp)) {
                val w = size.width; val h = size.height
                if (isPlaying) {
                    drawRoundRect(Color.White, Offset(w * .18f, h * .16f), Size(w * .23f, h * .68f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
                    drawRoundRect(Color.White, Offset(w * .59f, h * .16f), Size(w * .23f, h * .68f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f, 2f))
                } else {
                    val p = Path().apply { moveTo(w * .30f, h * .14f); lineTo(w * .30f, h * .86f); lineTo(w * .80f, h * .50f); close() }
                    drawPath(p, Color.White)
                }
            }
        }
        Box(Modifier.size(40.dp).background(Color.Black.copy(.62f), CircleShape).clickable { onFullscreen() }, contentAlignment = Alignment.Center) {
            Canvas(Modifier.size(18.dp)) {
                val sw = 2f
                drawLine(Color.White, Offset(3f, 7f), Offset(3f, 3f), sw); drawLine(Color.White, Offset(3f, 3f), Offset(7f, 3f), sw)
                drawLine(Color.White, Offset(11f, 3f), Offset(15f, 3f), sw); drawLine(Color.White, Offset(15f, 3f), Offset(15f, 7f), sw)
                drawLine(Color.White, Offset(3f, 11f), Offset(3f, 15f), sw); drawLine(Color.White, Offset(3f, 15f), Offset(7f, 15f), sw)
                drawLine(Color.White, Offset(11f, 15f), Offset(15f, 15f), sw); drawLine(Color.White, Offset(15f, 15f), Offset(15f, 11f), sw)
            }
        }
    }
}

@Composable
private fun ErrorMessage(text: String, modifier: Modifier) { Text(text, color = Color.White, modifier = modifier.padding(24.dp), fontSize = 14.sp) }

@Composable
private fun Movies(movies: List<Movie>, q: String, onQ: (String) -> Unit, loading: Boolean) {
    val filtered = movies.filter { it.title.contains(q, true) || it.description.contains(q, true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(q, onQ, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Поиск фильмов", color = MUTED) })
        Spacer(Modifier.height(12.dp))
        Text("Internet Archive", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Public Domain / Creative Commons", color = MUTED, fontSize = 12.sp)
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
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(movie.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis)
            if (movie.year.isNotBlank()) Text(movie.year, color = PURPLE, fontSize = 12.sp)
            Text(movie.description.ifBlank { "Открыть карточку фильма в Internet Archive" }, color = MUTED, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text("▶ Открыть и смотреть", color = PURPLE, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun FavoritesPage() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Canvas(Modifier.size(48.dp)) {
                val heart = Path().apply { moveTo(24f, 40f); cubicTo(5f, 27f, 6f, 11f, 15f, 10f); cubicTo(20f, 9f, 23f, 13f, 24f, 16f); cubicTo(25f, 13f, 28f, 9f, 33f, 10f); cubicTo(42f, 11f, 43f, 27f, 24f, 40f); close() }
                drawPath(heart, Color(0xFFC7CDD8), style = Stroke(width = 3.2f))
            }
            Spacer(Modifier.height(12.dp))
            Text("Избранное пока пусто", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Text("Добавим избранное позже", color = MUTED, fontSize = 13.sp)
        }
    }
}

@Composable
private fun NavIcon(id: String, selected: Boolean) {
    val tint = if (selected) Color.White else Color(0xFFC7CDD8)
    Canvas(Modifier.size(26.dp)) {
        val w = size.width; val h = size.height
        when (id) {
            "channels" -> {
                drawRoundRect(tint, Offset(w * .14f, h * .22f), Size(w * .72f, h * .56f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f), style = Stroke(width = 2.5f))
                drawLine(tint, Offset(w * .30f, h * .88f), Offset(w * .70f, h * .88f), strokeWidth = 2.5f)
                drawCircle(PURPLE, radius = 2f, center = Offset(w * .50f, h * .50f))
            }
            "movies" -> {
                drawRoundRect(tint, Offset(w * .13f, h * .18f), Size(w * .74f, h * .62f), cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f, 3f), style = Stroke(width = 2.3f))
                drawLine(tint, Offset(w * .27f, h * .18f), Offset(w * .32f, h * .38f), strokeWidth = 2f)
                drawLine(tint, Offset(w * .46f, h * .18f), Offset(w * .51f, h * .38f), strokeWidth = 2f)
                drawLine(tint, Offset(w * .65f, h * .18f), Offset(w * .70f, h * .38f), strokeWidth = 2f)
                val play = Path().apply { moveTo(w * .42f, h * .46f); lineTo(w * .42f, h * .69f); lineTo(w * .68f, h * .57f); close() }
                drawPath(play, PURPLE)
            }
            else -> {
                val heart = Path().apply { moveTo(w * .50f, h * .84f); cubicTo(w * .11f, h * .58f, w * .16f, h * .18f, w * .38f, h * .24f); cubicTo(w * .47f, h * .26f, w * .50f, h * .34f, w * .50f, h * .40f); cubicTo(w * .50f, h * .34f, w * .53f, h * .26f, w * .62f, h * .24f); cubicTo(w * .84f, h * .18f, w * .89f, h * .58f, w * .50f, h * .84f) }
                drawPath(heart, tint, style = Stroke(width = 2.4f))
            }
        }
    }
}

@Composable
private fun BottomBar(page: String, onPage: (String) -> Unit) {
    NavigationBar(containerColor = Color(0xFF0D1117), tonalElevation = 0.dp) {
        listOf("movies" to "Фильмы", "channels" to "ТВ", "favorites" to "Избранное").forEach { (id, label) ->
            NavigationBarItem(selected = page == id, onClick = { onPage(id) }, icon = { NavIcon(id, page == id) }, label = { Text(label, fontSize = 12.sp) })
        }
    }
}

private fun loadM3u(): List<Channel> = try { parseM3u(URL(PLAYLIST_URL).readText()) } catch (_: Exception) { emptyList() }

private fun parseM3u(text: String): List<Channel> {
    val result = mutableListOf<Channel>(); var name = ""; var group = ""; var ua = ""; var logo = ""
    val groupRe = Regex("""group-title\\s*=\\s*[\"']([^\"']*)[\"']""", RegexOption.IGNORE_CASE)
    val logoRe = Regex("""tvg-logo\\s*=\\s*[\"']([^\"']*)[\"']""", RegexOption.IGNORE_CASE)
    text.lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.startsWith("#EXTINF:", true) -> { name = line.substringAfterLast(',').trim(); group = groupRe.find(line)?.groupValues?.getOrNull(1).orEmpty(); logo = logoRe.find(line)?.groupValues?.getOrNull(1).orEmpty(); ua = "" }
            line.startsWith("#EXTVLCOPT:http-user-agent=", true) -> ua = line.substringAfter('=').trim()
            line.startsWith("#") || line.isBlank() -> Unit
            else -> { if (name.isNotBlank()) result += Channel(name, group, line, ua, logo); name = ""; group = ""; ua = ""; logo = "" }
        }
    }
    return result
}

private fun loadMovies(): List<Movie> = try {
    val docs = JSONObject(URL(IA_SEARCH).readText()).getJSONObject("response").getJSONArray("docs")
    buildList {
        for (i in 0 until docs.length()) {
            val item = docs.getJSONObject(i); val id = item.optString("identifier"); val title = item.optString("title"); val license = item.optString("licenseurl")
            if (id.isNotBlank() && title.isNotBlank() && (license.contains("creativecommons.org", true) || license.contains("publicdomain", true))) add(Movie(id, title, item.optString("year"), item.optString("description"), license))
        }
    }.distinctBy { it.id }
} catch (_: Exception) { emptyList() }

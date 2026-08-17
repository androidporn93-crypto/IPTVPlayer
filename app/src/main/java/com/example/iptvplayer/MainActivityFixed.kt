package com.example.iptvplayer

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
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
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
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
private const val PREFS = "iptv_prefs"
private const val PREF_FAVORITES = "favorite_urls"
private val BG = Color(0xFF080B10)
private val CARD = Color(0xFF121820)
private val PURPLE = Color(0xFFA855F7)
private val MUTED = Color(0xFF8D98A8)
private val ICON_BG = Color(0xFF172131)

data class FixedChannel(val name: String, val group: String, val url: String, val userAgent: String = "", val logo: String = "")
data class FixedMovie(val id: String, val title: String, val year: String, val description: String, val license: String)

class MainActivityFixed : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FixedIPTVApp() }
    }
}

@Composable
private fun FixedIPTVApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf("home") }
    var query by remember { mutableStateOf("") }
    var channels by remember { mutableStateOf<List<FixedChannel>>(emptyList()) }
    var movies by remember { mutableStateOf<List<FixedMovie>>(emptyList()) }
    var favorites by remember { mutableStateOf(prefs.getStringSet(PREF_FAVORITES, emptySet())?.toSet() ?: emptySet()) }
    var loading by remember { mutableStateOf(true) }
    var movieLoading by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }

    fun toggleFavorite(c: FixedChannel) {
        favorites = favorites.toMutableSet().apply { if (!add(c.url)) remove(c.url) }
        prefs.edit().putStringSet(PREF_FAVORITES, favorites).apply()
    }

    LaunchedEffect(Unit) {
        channels = withContext(Dispatchers.IO) { fixedLoadM3u() }
        loading = false
    }
    LaunchedEffect(page) {
        if (page == "movies" && movies.isEmpty() && !movieLoading) {
            movieLoading = true
            movies = withContext(Dispatchers.IO) { fixedLoadMovies() }
            movieLoading = false
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = BG, surface = CARD, primary = PURPLE)) {
        if (selected != null && channels.isNotEmpty()) {
            FixedPlayer(channels, selected!!, channels[selected!!].url in favorites, { toggleFavorite(channels[selected!!]) }, { selected = it }, { selected = null })
            return@MaterialTheme
        }

        // Intentionally no bottom navigation: the reference design is a clean full-screen home/list.
        Column(Modifier.fillMaxSize().background(BG)) {
            FixedHeader(onSettings = {})
            Box(Modifier.weight(1f).fillMaxWidth()) {
                when (page) {
                    "channels" -> FixedChannels(channels, query, { query = it }, loading, favorites) { selected = it }
                    "movies" -> FixedMovies(movies, query, { query = it }, movieLoading)
                    "favorites" -> FixedFavorites(channels, favorites, { selected = it }, ::toggleFavorite)
                    else -> FixedHome({ page = "channels" }, { page = "movies" })
                }
            }
        }
    }
}

@Composable
private fun FixedHeader(onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(start = 34.dp, end = 28.dp, top = 24.dp, bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("IPTV", color = PURPLE, fontSize = 31.sp, fontWeight = FontWeight.Bold)
        Text(" Player", color = Color.White, fontSize = 31.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.weight(1f))
        Box(Modifier.size(42.dp).background(Color(0xFF151A22), CircleShape).clickable(onClick = onSettings), Alignment.Center) {
            Text("⚙", color = Color.White, fontSize = 23.sp)
        }
    }
}

@Composable
private fun FixedHome(tv: () -> Unit, movie: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp)) {
        Box(Modifier.fillMaxWidth().height(268.dp).clip(RoundedCornerShape(26.dp))) {
            Image(painterResource(R.drawable.hero_tv), "TV", Modifier.fillMaxSize())
            Column(Modifier.align(Alignment.CenterStart).padding(start = 36.dp, top = 4.dp)) {
                Text("TV", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(4.dp))
                Text("Смотрите любимые каналы", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("и доступное кино", color = Color.White.copy(.82f), fontSize = 18.sp)
            }
        }
        Spacer(Modifier.height(18.dp))
        FixedHomeCard(R.drawable.iptv_channels_photo, "ТВ каналы", "Ваш M3U плейлист", tv)
        Spacer(Modifier.height(14.dp))
        FixedHomeCard(R.drawable.movies_photo, "Фильмы", "Internet Archive · открытые лицензии", movie)
    }
}

@Composable
private fun FixedHomeCard(image: Int, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(136.dp).clip(RoundedCornerShape(22.dp)).background(CARD).clickable(onClick = onClick).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Image(painterResource(image), title, Modifier.size(110.dp).clip(RoundedCornerShape(16.dp)))
        Column(Modifier.padding(start = 18.dp).weight(1f)) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(subtitle, color = MUTED, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = PURPLE, fontSize = 42.sp, modifier = Modifier.padding(end = 5.dp))
    }
}

@Composable
private fun FixedChannels(channels: List<FixedChannel>, q: String, onQ: (String) -> Unit, loading: Boolean, favorites: Set<String>, onPlay: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp)) {
        OutlinedTextField(q, onQ, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Поиск канала", color = MUTED) })
        if (loading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PURPLE) }
        else {
            val list = channels.withIndex().filter { it.value.name.contains(q, true) || it.value.group.contains(q, true) }
            LazyColumn(contentPadding = PaddingValues(vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                itemsIndexed(list) { _, item -> FixedChannelRow(item.index, item.value, item.value.url in favorites, onPlay) }
            }
        }
    }
}

@Composable
private fun FixedChannelRow(index: Int, c: FixedChannel, favorite: Boolean, on: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().height(88.dp).background(CARD, RoundedCornerShape(18.dp)).clickable { on(index) }.padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(62.dp).background(ICON_BG, RoundedCornerShape(16.dp)), Alignment.Center) {
            if (c.logo.isNotBlank()) AsyncImage(c.logo, c.name, Modifier.size(48.dp).clip(RoundedCornerShape(12.dp))) else FixedChannelLogo(index, c.name)
        }
        Column(Modifier.padding(start = 15.dp).weight(1f)) {
            Text(c.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(c.group.ifBlank { "ТВ канал" }, color = MUTED, fontSize = 13.sp)
        }
        FixedHeart(favorite, 48.dp) { on(index) }
        Spacer(Modifier.width(10.dp))
        FixedPlay(44.dp) { on(index) }
    }
}

@Composable
private fun FixedChannelLogo(index: Int, name: String) {
    val colors = listOf(Color(0xFF2866CF), Color(0xFF18A85C), Color(0xFFE33445), Color(0xFF4D7EA9), Color(0xFF5E9AD6), Color(0xFFE34B32), Color(0xFFF08A22))
    val short = when {
        name.contains("Крым", true) -> "24"
        name.contains("Югра", true) -> "Ю"
        name.contains("Липец", true) -> "◷"
        name.contains("4 канал", true) -> "4"
        name.contains("НТВ", true) -> "НТВ"
        name.contains("Россия", true) -> "РОССИЯ"
        else -> name.take(4).uppercase()
    }
    Box(Modifier.size(48.dp).background(Brush.linearGradient(listOf(colors[index % colors.size], colors[index % colors.size].copy(.55f))), RoundedCornerShape(12.dp)), Alignment.Center) {
        Text(short, color = Color.White, fontSize = if (short.length > 4) 8.sp else 12.sp, fontWeight = FontWeight.ExtraBold)
    }
}

@Composable
private fun FixedPlay(size: Dp, onClick: () -> Unit) {
    Box(Modifier.size(size).background(Color(0xFF4C1D95), CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Canvas(Modifier.size(size * .38f)) { val p = Path().apply { moveTo(size.width * .30f, size.height * .14f); lineTo(size.width * .30f, size.height * .86f); lineTo(size.width * .80f, size.height * .50f); close() }; drawPath(p, Color.White) }
    }
}

@Composable
private fun FixedHeart(favorite: Boolean, size: Dp, onClick: () -> Unit) {
    Box(Modifier.size(size).background(Color.Black.copy(.58f), CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Canvas(Modifier.size(size * .48f)) {
            val w = this.size.width; val h = this.size.height
            val heart = Path().apply { moveTo(w*.5f,h*.88f); cubicTo(w*.08f,h*.58f,w*.12f,h*.15f,w*.38f,h*.22f); cubicTo(w*.47f,h*.24f,w*.5f,h*.34f,w*.5f,h*.4f); cubicTo(w*.5f,h*.34f,w*.53f,h*.24f,w*.62f,h*.22f); cubicTo(w*.88f,h*.15f,w*.92f,h*.58f,w*.5f,h*.88f) }
            drawPath(heart, if (favorite) PURPLE else Color.White, style = Stroke(width = 3f))
        }
    }
}

@Composable
private fun FixedPlayer(channels: List<FixedChannel>, index: Int, favorite: Boolean, onFavorite: () -> Unit, onSelect: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val channel = channels[index]
    var fullscreen by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var playing by remember { mutableStateOf(true) }
    var controls by remember { mutableStateOf(true) }
    val player = remember(channel.url, channel.userAgent) {
        val http = DefaultHttpDataSource.Factory().setUserAgent(channel.userAgent.ifBlank { UA }).setAllowCrossProtocolRedirects(true).setConnectTimeoutMs(15000).setReadTimeoutMs(20000)
        ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(http)).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(e: PlaybackException) { error = e.errorCodeName }
                override fun onIsPlayingChanged(v: Boolean) { playing = v }
            })
            setMediaItem(MediaItem.fromUri(channel.url)); prepare()
        }
    }
    DisposableEffect(player) { onDispose { player.release(); activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT } }
    BackHandler { if (fullscreen) fullscreen = false else onBack() }
    LaunchedEffect(fullscreen) {
        activity.requestedOrientation = if (fullscreen) ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        WindowCompat.setDecorFitsSystemWindows(activity.window, !fullscreen)
        val ctl = WindowInsetsControllerCompat(activity.window, activity.window.decorView)
        if (fullscreen) ctl.hide(WindowInsetsCompat.Type.systemBars()) else ctl.show(WindowInsetsCompat.Type.systemBars())
        controls = true
    }
    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) { detectTapGestures(onTap = { controls = !controls }) }) {
            FixedVideo(player, true, Modifier.fillMaxSize())
            if (controls) {
                // No controller/play button in the upper-left corner.
                FixedPlayPause(64.dp, Modifier.align(Alignment.Center)) { if (player.isPlaying) player.pause() else player.play() }
                FixedFullscreen(46.dp, Modifier.align(Alignment.BottomEnd).padding(18.dp)) { fullscreen = false }
            }
            error?.let { Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center).padding(top = 100.dp)) }
        }
    } else {
        Column(Modifier.fillMaxSize().background(BG)) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f/9f).background(Color.Black).pointerInput(Unit) { detectTapGestures(onTap = { controls = !controls }) }) {
                FixedVideo(player, false, Modifier.fillMaxSize())
                if (controls) {
                    Box(Modifier.align(Alignment.TopStart).padding(10.dp).size(40.dp).background(Color.Black.copy(.55f), CircleShape).clickable { onBack() }, Alignment.Center) { Text("‹", color = Color.White, fontSize = 28.sp) }
                    FixedHeart(favorite, 42.dp, Modifier.align(Alignment.TopEnd).padding(10.dp)) { onFavorite() }
                    FixedPlayPause(52.dp, Modifier.align(Alignment.Center)) { if (player.isPlaying) player.pause() else player.play() }
                    FixedFullscreen(44.dp, Modifier.align(Alignment.BottomEnd).padding(10.dp)) { fullscreen = true }
                }
                error?.let { Text(it, color = Color.White, modifier = Modifier.align(Alignment.Center).padding(20.dp)) }
            }
            Text("${index + 1}. ${channel.name}", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 4.dp))
            Text("Следующие каналы", color = MUTED, fontSize = 14.sp, modifier = Modifier.padding(horizontal = 16.dp))
            LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(channels.drop(index + 1)) { offset, next ->
                    val ni = index + 1 + offset
                    Row(Modifier.fillMaxWidth().height(68.dp).background(CARD, RoundedCornerShape(12.dp)).clickable { onSelect(ni) }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${ni + 1}", color = PURPLE, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(38.dp))
                        Box(Modifier.size(42.dp).background(ICON_BG, RoundedCornerShape(10.dp)), Alignment.Center) { FixedChannelLogo(ni, next.name) }
                        Text(next.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 10.dp).weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        FixedPlay(34.dp) { onSelect(ni) }
                    }
                }
            }
        }
    }
}

@Composable
private fun FixedPlayPause(size: Dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(size).background(Color(0xFF4C1D95).copy(.92f), CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Canvas(Modifier.size(size * .38f)) { val w = this.size.width; val h = this.size.height; val p = Path().apply { moveTo(w*.30f,h*.14f); lineTo(w*.30f,h*.86f); lineTo(w*.80f,h*.50f); close() }; drawPath(p, Color.White) }
    }
}

@Composable
private fun FixedFullscreen(size: Dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(size).background(Color.Black.copy(.65f), CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Text("⛶", color = Color.White, fontSize = size.value.sp * .55f)
    }
}

@Composable
private fun FixedVideo(player: ExoPlayer, zoom: Boolean, modifier: Modifier) {
    AndroidView(modifier = modifier, factory = { ctx -> PlayerView(ctx).apply { this.player = player; keepScreenOn = true; useController = false; resizeMode = if (zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT } }, update = { it.player = player; it.useController = false; it.resizeMode = if (zoom) AspectRatioFrameLayout.RESIZE_MODE_ZOOM else AspectRatioFrameLayout.RESIZE_MODE_FIT })
}

@Composable
private fun FixedMovies(movies: List<FixedMovie>, q: String, onQ: (String) -> Unit, loading: Boolean) {
    val filtered = movies.filter { it.title.contains(q, true) || it.description.contains(q, true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 30.dp)) {
        OutlinedTextField(q, onQ, Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Поиск фильмов", color = MUTED) })
        Spacer(Modifier.height(12.dp)); Text("Internet Archive", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Public Domain / Creative Commons", color = MUTED, fontSize = 12.sp)
        if (loading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = PURPLE) }
        else LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { itemsIndexed(filtered) { _, m -> FixedMovieCard(m) } }
    }
}

@Composable
private fun FixedMovieCard(m: FixedMovie) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().background(CARD, RoundedCornerShape(16.dp)).clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://archive.org/details/${m.id}"))) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage("https://archive.org/services/img/${m.id}", m.title, Modifier.width(88.dp).height(118.dp).clip(RoundedCornerShape(12.dp)))
        Column(Modifier.padding(start = 14.dp).weight(1f)) { Text(m.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis); if (m.year.isNotBlank()) Text(m.year, color = PURPLE, fontSize = 12.sp); Text(m.description, color = MUTED, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun FixedFavorites(channels: List<FixedChannel>, favorites: Set<String>, onPlay: (Int) -> Unit, onToggle: (FixedChannel) -> Unit) {
    val list = channels.withIndex().filter { it.value.url in favorites }
    if (list.isEmpty()) Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Избранное пока пусто", color = MUTED) }
    else LazyColumn(Modifier.fillMaxSize().padding(horizontal = 30.dp), contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { itemsIndexed(list) { _, item -> FixedChannelRow(item.index, item.value, true, onPlay) } }
}

private fun fixedLoadM3u(): List<FixedChannel> = try { fixedParseM3u(URL(PLAYLIST_URL).readText()) } catch (_: Exception) { emptyList() }
private fun fixedParseM3u(text: String): List<FixedChannel> {
    val result = mutableListOf<FixedChannel>(); var name = ""; var group = ""; var ua = ""; var logo = ""
    val groupRe = Regex("""group-title\\s*=\\s*[\"']([^\"']*)[\"']""", RegexOption.IGNORE_CASE)
    val logoRe = Regex("""tvg-logo\\s*=\\s*[\"']([^\"']*)[\"']""", RegexOption.IGNORE_CASE)
    text.lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.startsWith("#EXTINF:", true) -> { name = line.substringAfterLast(',').trim(); group = groupRe.find(line)?.groupValues?.getOrNull(1).orEmpty(); logo = logoRe.find(line)?.groupValues?.getOrNull(1).orEmpty(); ua = "" }
            line.startsWith("#EXTVLCOPT:http-user-agent=", true) -> ua = line.substringAfter('=').trim()
            line.startsWith("#") || line.isBlank() -> Unit
            else -> { if (name.isNotBlank()) result += FixedChannel(name, group, line, ua, logo); name = ""; group = ""; ua = ""; logo = "" }
        }
    }
    return result
}
private fun fixedLoadMovies(): List<FixedMovie> = try {
    val docs = JSONObject(URL(IA_SEARCH).readText()).getJSONObject("response").getJSONArray("docs")
    buildList { for (i in 0 until docs.length()) { val x = docs.getJSONObject(i); val id=x.optString("identifier"); val title=x.optString("title"); val lic=x.optString("licenseurl"); if (id.isNotBlank() && title.isNotBlank() && (lic.contains("creativecommons.org", true) || lic.contains("publicdomain", true))) add(FixedMovie(id,title,x.optString("year"),x.optString("description"),lic)) } }.distinctBy { it.id }
} catch (_: Exception) { emptyList() }

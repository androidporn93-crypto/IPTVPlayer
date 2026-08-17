package com.example.iptvplayer

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

private const val PLAYLIST_URL = "https://raw.githubusercontent.com/Dimonovich/TV/Dimonovich/FREE/.m3u"
private const val IA_SEARCH = "https://archive.org/advancedsearch.php?q=mediatype%3Amovies%20AND%20%28licenseurl%3Acreativecommons.org%20OR%20licenseurl%3Apublicdomain%29&fl%5B%5D=identifier&fl%5B%5D=title&fl%5B%5D=description&fl%5B%5D=year&fl%5B%5D=licenseurl&rows=60&page=1&output=json"
private val Bg = Color(0xFF080B10)
private val Card = Color(0xFF121820)
private val Purple = Color(0xFFA855F7)
private val Muted = Color(0xFF8D98A8)

data class Channel(val name: String, val group: String, val url: String)
data class Movie(val id: String, val title: String, val year: String, val description: String, val license: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContent { IPTVApp() } }
}

@Composable
fun IPTVApp() {
    var page by remember { mutableStateOf("home") }
    var search by remember { mutableStateOf("") }
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var loadingTv by remember { mutableStateOf(true) }
    var loadingMovies by remember { mutableStateOf(false) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(Unit) { channels = withContext(Dispatchers.IO) { loadM3u(PLAYLIST_URL) }; loadingTv = false }
    LaunchedEffect(page) {
        if (page == "movies" && movies.isEmpty() && !loadingMovies) { loadingMovies = true; movies = withContext(Dispatchers.IO) { loadMoviesFromArchive() }; loadingMovies = false }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Card, primary = Purple)) {
        if (selectedIndex != null && channels.isNotEmpty()) {
            PlayerScreen(channels, selectedIndex!!, { selectedIndex = it }) { selectedIndex = null }
            return@MaterialTheme
        }
        Scaffold(containerColor = Bg, bottomBar = { BottomBar(page) { page = it; search = "" } }) { padding ->
            Column(Modifier.fillMaxSize().background(Bg).padding(padding)) {
                Text("IPTV Player", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp))
                Box(Modifier.weight(1f)) {
                    when (page) {
                        "channels" -> ChannelPage(channels, search, { search = it }, loadingTv) { selectedIndex = it }
                        "movies" -> MoviePage(movies, search, { search = it }, loadingMovies)
                        else -> HomePage({ page = "channels" }, { page = "movies" })
                    }
                }
            }
        }
    }
}

@Composable
private fun HomePage(onTv: () -> Unit, onMovies: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.linearGradient(listOf(Color(0xFF6D28D9), Color(0xFF172B8A))), RoundedCornerShape(22.dp)).padding(20.dp)) {
            Column { Text("TV", color = Color.White.copy(alpha = .28f), fontSize = 54.sp, fontWeight = FontWeight.Black); Text("Смотрите любимые каналы", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text("и доступное кино", color = Color.White.copy(alpha = .8f), fontSize = 14.sp) }
        }
        Spacer(Modifier.height(18.dp)); HomeButton("📺", "ТВ каналы", "Ваш M3U плейлист", onTv); HomeButton("🎬", "Фильмы", "Internet Archive · открытые лицензии", onMovies)
    }
}

@Composable
private fun HomeButton(icon: String, title: String, subtitle: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp).background(Card, RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(icon, fontSize = 30.sp); Column(Modifier.padding(start = 14.dp).weight(1f)) { Text(title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold); Text(subtitle, color = Muted, fontSize = 12.sp) }; Text("›", color = Purple, fontSize = 28.sp)
    }
}

@Composable
private fun ChannelPage(channels: List<Channel>, query: String, onQueryChange: (String) -> Unit, loading: Boolean, onPlay: (Int) -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Поиск канала", color = Muted) })
        if (loading) Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Purple) } else {
            val filtered = channels.withIndex().filter { it.value.name.contains(query, true) || it.value.group.contains(query, true) }
            LazyColumn(contentPadding = PaddingValues(vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { itemsIndexed(filtered) { _, item -> ChannelRow(item.index, item.value, onPlay) } }
        }
    }
}

@Composable
private fun ChannelRow(index: Int, channel: Channel, onPlay: (Int) -> Unit) {
    Row(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(14.dp)).clickable { onPlay(index) }.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(48.dp).background(Color(0xFF273144), RoundedCornerShape(12.dp)), Alignment.Center) { Text("${index + 1}", color = Color.White, fontWeight = FontWeight.Bold) }
        Column(Modifier.padding(start = 12.dp).weight(1f)) { Text(channel.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(channel.group.ifBlank { "ТВ канал" }, color = Muted, fontSize = 12.sp) }
        Text("▶", color = Purple)
    }
}

@Composable
private fun PlayerScreen(channels: List<Channel>, selectedIndex: Int, onSelect: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val channel = channels[selectedIndex]
    var fullscreen by remember { mutableStateOf(false) }
    val player = remember(channel.url) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(channel.url)); prepare(); playWhenReady = true } }

    BackHandler { if (fullscreen) fullscreen = false else onBack() }
    LaunchedEffect(fullscreen) {
        if (fullscreen) {
            WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            WindowInsetsControllerCompat(activity.window, activity.window.decorView).apply {
                hide(WindowInsetsCompat.Type.systemBars())
                systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            }
        } else {
            WindowCompat.setDecorFitsSystemWindows(activity.window, true)
            WindowInsetsControllerCompat(activity.window, activity.window.decorView).show(WindowInsetsCompat.Type.systemBars())
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }

    if (fullscreen) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; keepScreenOn = true; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; controllerShowTimeoutMs = 2500; controllerHideOnTouch = true } }, modifier = Modifier.fillMaxSize())
            Text("⛶", color = Color.White, fontSize = 28.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(20.dp).clickable { fullscreen = false })
        }
        return
    }

    Column(Modifier.fillMaxSize().background(Bg)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Color.White, fontSize = 32.sp, modifier = Modifier.clickable { onBack() })
            Text(channel.name, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(start = 10.dp).weight(1f))
        }
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black)) {
            AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; keepScreenOn = true; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT; controllerShowTimeoutMs = 2500; controllerHideOnTouch = true } }, modifier = Modifier.fillMaxSize())
            Text("⛶", color = Color.White, fontSize = 27.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp).clickable { fullscreen = true })
        }
        Text("${selectedIndex + 1}. ${channel.name}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 6.dp))
        Text("Следующие каналы", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
        LazyColumn(contentPadding = PaddingValues(12.dp, 10.dp, 12.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            itemsIndexed(channels.drop(selectedIndex + 1)) { offset, next ->
                val actualIndex = selectedIndex + 1 + offset
                Row(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(13.dp)).clickable { onSelect(actualIndex) }.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("${actualIndex + 1}", color = Purple, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                    Column(Modifier.weight(1f)) { Text(next.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(next.group.ifBlank { "ТВ канал" }, color = Muted, fontSize = 11.sp) }
                    Text("▶", color = Purple)
                }
            }
        }
    }
}

@Composable
private fun MoviePage(movies: List<Movie>, query: String, onQueryChange: (String) -> Unit, loading: Boolean) {
    val filtered = movies.filter { it.title.contains(query, true) || it.description.contains(query, true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(value = query, onValueChange = onQueryChange, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("Поиск фильмов", color = Muted) })
        Spacer(Modifier.height(12.dp)); Text("🎬 Internet Archive", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold); Text("Public Domain / Creative Commons", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 10.dp))
        when { loading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator(color = Purple) }; filtered.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("Каталог пуст или источник недоступен", color = Muted) }; else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 20.dp)) { itemsIndexed(filtered) { _, movie -> MovieCard(movie) } } }
    }
}

@Composable
private fun MovieCard(movie: Movie) {
    val context = LocalContext.current
    Row(Modifier.fillMaxWidth().background(Card, RoundedCornerShape(16.dp)).clickable { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://archive.org/details/${movie.id}"))) }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(model = "https://archive.org/services/img/${movie.id}", contentDescription = movie.title, modifier = Modifier.width(88.dp).height(118.dp).background(Color(0xFF222A35), RoundedCornerShape(12.dp)))
        Column(Modifier.padding(start = 14.dp).weight(1f)) { Text(movie.title, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis); if (movie.year.isNotBlank()) Text(movie.year, color = Purple, fontSize = 12.sp); Text(movie.description.ifBlank { "Открыть карточку фильма в Internet Archive" }, color = Muted, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 5.dp)); Text("▶ Открыть и смотреть", color = Purple, fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 7.dp)) }
    }
}

@Composable
private fun BottomBar(page: String, onPageChange: (String) -> Unit) {
    val tabs = listOf(Triple("home", "⌂", "Главная"), Triple("channels", "▣", "ТВ"), Triple("movies", "🎬", "Фильмы"), Triple("settings", "⚙", "Ещё"))
    NavigationBar(containerColor = Color(0xFF0D1117)) { tabs.forEach { (id, icon, label) -> NavigationBarItem(selected = page == id, onClick = { onPageChange(id) }, icon = { Text(icon, fontSize = 20.sp) }, label = { Text(label, fontSize = 10.sp) }) } }
}

private fun loadM3u(url: String): List<Channel> = try { parseM3u(URL(url).readText()) } catch (_: Exception) { emptyList() }
private fun parseM3u(text: String): List<Channel> {
    val result = mutableListOf<Channel>(); var name = ""; var group = ""
    text.lineSequence().forEach { raw -> val line = raw.trim(); when { line.startsWith("#EXTINF:", true) -> { name = line.substringAfterLast(",").trim(); group = Regex("""group-title=\"([^\"]*)\"""", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1).orEmpty() }; line.startsWith("#") || line.isBlank() -> Unit; else -> { if (name.isNotBlank()) result += Channel(name, group, line); name = ""; group = "" } } }
    return result
}
private fun loadMoviesFromArchive(): List<Movie> = try {
    val docs = JSONObject(URL(IA_SEARCH).readText()).getJSONObject("response").getJSONArray("docs")
    buildList { for (i in 0 until docs.length()) { val d = docs.getJSONObject(i); val id = d.optString("identifier"); val title = d.optString("title"); val license = d.optString("licenseurl"); if (id.isNotBlank() && title.isNotBlank() && (license.contains("creativecommons.org", true) || license.contains("publicdomain", true))) add(Movie(id, title, d.optString("year"), d.optString("description").replace("\\n", " "), license)) } }.distinctBy { it.id }
} catch (_: Exception) { emptyList() }

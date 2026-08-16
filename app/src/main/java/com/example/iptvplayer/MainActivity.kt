package com.example.iptvplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private const val PLAYLIST_URL = "https://raw.githubusercontent.com/Dimonovich/TV/Dimonovich/FREE/.m3u"

private val Bg = Color(0xFF080B10)
private val Card = Color(0xFF121820)
private val Card2 = Color(0xFF171E28)
private val Purple = Color(0xFFA855F7)
private val Purple2 = Color(0xFF6D28D9)
private val Muted = Color(0xFF8D98A8)

data class Channel(val name: String, val group: String, val logo: String?, val url: String, val userAgent: String? = null)

data class NavItem(val title: String, val icon: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IPTVApp() }
    }
}

@Composable
fun IPTVApp() {
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var selected by remember { mutableStateOf<Channel?>(null) }
    var page by remember { mutableStateOf("home") }
    var search by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(true) }
    var drawer by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(setOf<String>()) }

    LaunchedEffect(Unit) {
        channels = withContext(Dispatchers.IO) { loadM3u(PLAYLIST_URL) }
        loading = false
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Bg, surface = Card, primary = Purple)) {
        if (selected != null) {
            PlayerScreen(selected!!, onBack = { selected = null })
            return@MaterialTheme
        }

        Box(Modifier.fillMaxSize().background(Bg)) {
            Column(Modifier.fillMaxSize()) {
                TopBar(
                    title = when (page) { "channels" -> "ТВ каналы"; "favorites" -> "Избранное"; "settings" -> "Настройки"; else -> "IPTV Player" },
                    onMenu = { drawer = true },
                    onSearch = { page = "channels" }
                )

                if (loading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Purple) }
                } else {
                    when (page) {
                        "channels" -> ChannelPage(channels, search, { search = it }, favorites, { favorites = if (it.name in favorites) favorites - it.name else favorites + it.name }, { selected = it })
                        "favorites" -> ChannelPage(channels.filter { it.name in favorites }, search, { search = it }, favorites, { favorites = if (it.name in favorites) favorites - it.name else favorites + it.name }, { selected = it })
                        "settings" -> SettingsPage()
                        else -> HomePage(channels, favorites, { selected = it }, { page = "channels" })
                    }
                }

                if (page != "settings") BottomBar(page, { page = it })
            }

            if (drawer) {
                Drawer(onClose = { drawer = false }, onNavigate = { page = it; drawer = false })
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onMenu: () -> Unit, onSearch: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("☰", color = Color.White, fontSize = 24.sp, modifier = Modifier.clickable { onMenu() })
        Spacer(Modifier.width(18.dp))
        Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("IPTV", color = Purple, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.padding(end = 10.dp))
        Text("⌕", color = Color.White, fontSize = 28.sp, modifier = Modifier.clickable { onSearch() })
    }
}

@Composable
private fun HomePage(channels: List<Channel>, favorites: Set<String>, onPlay: (Channel) -> Unit, onAll: () -> Unit) {
    val live = channels.take(5)
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(22.dp)).background(Brush.linearGradient(listOf(Purple2, Color(0xFF172B8A))))) {
            Column(Modifier.padding(20.dp).align(Alignment.CenterStart)) {
                Text("TV", color = Color.White.copy(alpha = .25f), fontSize = 58.sp, fontWeight = FontWeight.Black)
                Text("Смотрите любимые каналы", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("в отличном качестве", color = Color.White.copy(alpha = .85f), fontSize = 14.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Сейчас в эфире", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("Смотреть все", color = Purple, fontSize = 13.sp, modifier = Modifier.clickable { onAll() })
        }
        Spacer(Modifier.height(10.dp))
        LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 10.dp)) {
            items(live) { channel -> ChannelRow(channel, channel.name in favorites, { onPlay(channel) }, {}) }
        }
    }
}

@Composable
private fun ChannelPage(channels: List<Channel>, search: String, onSearch: (String) -> Unit, favorites: Set<String>, onFavorite: (Channel) -> Unit, onPlay: (Channel) -> Unit) {
    val filtered = channels.filter { it.name.contains(search, true) || it.group.contains(search, true) }
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        OutlinedTextField(value = search, onValueChange = onSearch, singleLine = true, placeholder = { Text("Поиск канала", color = Muted) }, modifier = Modifier.fillMaxWidth(), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Purple, unfocusedBorderColor = Card2, focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Purple))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("Все", "Новости", "Спорт", "Кино").forEachIndexed { i, x -> FilterChip(selected = i == 0, onClick = {}, label = { Text(x) }) } }
        Spacer(Modifier.height(8.dp))
        Text("${filtered.size} каналов", color = Muted, fontSize = 13.sp, modifier = Modifier.padding(vertical = 4.dp))
        LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(6.dp), contentPadding = PaddingValues(bottom = 80.dp)) {
            items(filtered) { channel -> ChannelRow(channel, channel.name in favorites, { onPlay(channel) }, { onFavorite(channel) }) }
        }
    }
}

@Composable
private fun ChannelRow(channel: Channel, favorite: Boolean, onPlay: () -> Unit, onFavorite: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Card).clickable { onPlay() }.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(50.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFF263144)), contentAlignment = Alignment.Center) {
            Text(channel.name.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(channel.name, color = Color.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(channel.group.ifBlank { "ТВ канал" }, color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(5.dp))
            Box(Modifier.fillMaxWidth().height(2.dp).background(Color(0xFF2A313C))) { Box(Modifier.fillMaxWidth(.55f).height(2.dp).background(Purple)) }
        }
        Text(if (favorite) "★" else "☆", color = if (favorite) Purple else Muted, fontSize = 25.sp, modifier = Modifier.clickable { onFavorite() }.padding(4.dp))
        Text("▶", color = Purple, fontSize = 16.sp, modifier = Modifier.padding(6.dp))
    }
}

@Composable
private fun BottomBar(page: String, onPage: (String) -> Unit) {
    NavigationBar(containerColor = Color(0xFF0D1117)) {
        listOf("home" to "⌂\nГлавная", "channels" to "▣\nТВ каналы", "favorites" to "♡\nИзбранное", "settings" to "•••\nЕще").forEach { (id, label) ->
            NavigationBarItem(selected = page == id, onClick = { onPage(id) }, icon = { Text(label, textAlign = androidx.compose.ui.text.style.TextAlign.Center, fontSize = 11.sp) }, label = null)
        }
    }
}

@Composable
private fun Drawer(onClose: () -> Unit, onNavigate: (String) -> Unit) {
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = .65f)).clickable { onClose() }) {
        Column(Modifier.fillMaxHeight().width(300.dp).background(Color(0xFF0D131A)).padding(24.dp).clickable(enabled = false) {}) {
            Spacer(Modifier.height(18.dp))
            Text("IPTV", color = Purple, fontSize = 28.sp, fontWeight = FontWeight.Black)
            Text("Player", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Light)
            Text("v0.1", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 28.dp))
            listOf("home" to "⌂   Главная", "channels" to "▣   ТВ каналы", "favorites" to "☆   Избранное", "settings" to "⚙   Настройки").forEach { item ->
                Text(item.second, color = Color.White, fontSize = 16.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable { onNavigate(item.first) }.padding(vertical = 15.dp, horizontal = 12.dp))
            }
        }
    }
}

@Composable
private fun SettingsPage() {
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingSection("ПЛЕЕР", listOf("Плеер по умолчанию" to "ExoPlayer", "Аппаратное декодирование" to "Вкл", "Автозапуск следующего канала" to "Выкл"))
        SettingSection("ПРИЛОЖЕНИЕ", listOf("Язык" to "Русский", "Тема" to "Темная", "Очистить кэш" to "12,4 МБ"))
        SettingSection("ПЛЕЙЛИСТ", listOf("Источник" to "M3U", "Обновлять при запуске" to "Вкл"))
        Text("EPG отключен", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(8.dp))
    }
}

@Composable
private fun SettingSection(title: String, values: List<Pair<String, String>>) {
    Text(title, color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, top = 6.dp))
    Card(colors = CardDefaults.cardColors(containerColor = Card), shape = RoundedCornerShape(15.dp)) {
        Column {
            values.forEachIndexed { index, pair ->
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(pair.first, color = Color.White, modifier = Modifier.weight(1f))
                    Text(pair.second, color = Muted)
                    Text("›", color = Muted, fontSize = 22.sp, modifier = Modifier.padding(start = 8.dp))
                }
                if (index != values.lastIndex) HorizontalDivider(color = Color(0xFF222A35))
            }
        }
    }
}

@Composable
private fun PlayerScreen(channel: Channel, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(channel.url) { ExoPlayer.Builder(context).build().apply { setMediaItem(MediaItem.fromUri(channel.url)); prepare(); playWhenReady = true } }
    DisposableEffect(player) { onDispose { player.release() } }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("‹", color = Color.White, fontSize = 34.sp, modifier = Modifier.clickable { onBack() })
            Column(Modifier.weight(1f).padding(start = 8.dp)) { Text(channel.name, color = Color.White, fontWeight = FontWeight.Bold); Text(channel.group, color = Muted, fontSize = 12.sp) }
            Text("☆", color = Color.White, fontSize = 28.sp)
            Text("⚙", color = Color.White, fontSize = 22.sp, modifier = Modifier.padding(start = 12.dp))
        }
        AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true; controllerAutoShow = true } }, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Column(Modifier.fillMaxWidth().padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Text("● LIVE", color = Color(0xFFFF4B4B), fontWeight = FontWeight.Bold); Spacer(Modifier.width(10.dp)); Text("Прямой эфир", color = Muted) }
            Spacer(Modifier.height(16.dp))
            Text("Следующие каналы", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        }
    }
}

private fun loadM3u(url: String): List<Channel> = try { parseM3u(URL(url).readText()) } catch (_: Exception) { emptyList() }

private fun parseM3u(text: String): List<Channel> {
    val result = mutableListOf<Channel>()
    var name = ""; var group = ""; var logo: String? = null; var ua: String? = null
    text.lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.startsWith("#EXTINF:", true) -> {
                name = line.substringAfterLast(",").trim()
                group = Regex("""group-title=\"([^\"]*)\"""", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1).orEmpty()
                logo = Regex("""tvg-logo=\"([^\"]*)\"""", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1)
            }
            line.startsWith("#EXTVLCOPT:http-user-agent=", true) -> ua = line.substringAfter("=", "").trim()
            line.startsWith("#") || line.isBlank() -> Unit
            else -> { if (name.isNotBlank()) result += Channel(name, group, logo, line, ua); name = ""; group = ""; logo = null; ua = null }
        }
    }
    return result
}

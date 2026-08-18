package com.example.iptvplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private const val PLAYLIST_URL =
    "https://raw.githubusercontent.com/Dimonovich/TV/Dimonovich/FREE/.m3u"

private object AppColors {
    val Background = Color(0xFF0A0A10)
    val Card = Color(0xFF16161F)
    val CardBorder = Color(0xFF211F2B)
    val PurpleBright = Color(0xFF9B4BFF)
    val PurpleDark = Color(0xFF4C15B0)
    val PurpleAccent = Color(0xFFA45BFF)
    val TextDim = Color(0xFF9997A8)
    val TextDim2 = Color(0xFF6F6D7D)
}

data class Channel(
    val name: String,
    val group: String,
    val logo: String?,
    val url: String,
    val userAgent: String? = null
)

private enum class Screen { HOME, CHANNELS, MOVIES, PLAYER }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { IPTVApp() }
    }
}

@Composable
fun IPTVApp() {
    var screen by remember { mutableStateOf(Screen.HOME) }
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var selected by remember { mutableStateOf<Channel?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        channels = withContext(Dispatchers.IO) { loadM3u(PLAYLIST_URL) }
        loading = false
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = AppColors.Background,
            surface = AppColors.Card,
            primary = AppColors.PurpleAccent
        )
    ) {
        val showBottomBar = screen != Screen.PLAYER

        Scaffold(
            containerColor = AppColors.Background,
            bottomBar = {
                if (showBottomBar) {
                    BottomNavBar(
                        current = screen,
                        onSelect = { screen = it }
                    )
                }
            }
        ) { innerPadding ->
            Box(Modifier.padding(innerPadding)) {
                when (screen) {
                    Screen.HOME -> HomeScreen(
                        onOpenChannels = { screen = Screen.CHANNELS },
                        onOpenMovies = { screen = Screen.MOVIES }
                    )
                    Screen.CHANNELS -> ChannelListScreen(
                        channels = channels,
                        loading = loading,
                        onBack = { screen = Screen.HOME },
                        onSelectChannel = {
                            selected = it
                            screen = Screen.PLAYER
                        }
                    )
                    Screen.MOVIES -> MoviesScreen(onBack = { screen = Screen.HOME })
                    Screen.PLAYER -> selected?.let { channel ->
                        PlayerScreen(channel, onBack = { screen = Screen.CHANNELS })
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onOpenChannels: () -> Unit,
    onOpenMovies: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(12.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Text(
                    "IPTV ",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Text(
                    "Player",
                    color = AppColors.PurpleAccent,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF17161F))
                    .clickable { /* TODO: settings screen */ },
                contentAlignment = Alignment.Center
            ) {
                Text("⚙", color = AppColors.TextDim, fontSize = 18.sp)
            }
        }

        Spacer(Modifier.height(18.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 200.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(
                    Brush.linearGradient(
                        colors = listOf(AppColors.PurpleBright, AppColors.PurpleDark)
                    )
                )
                .clickable { onOpenChannels() }
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = AppColors.PurpleBright, fontSize = 22.sp)
            }
            Column(Modifier.align(Alignment.BottomStart)) {
                Text(
                    "TV",
                    color = Color.White,
                    fontSize = 42.sp,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Смотрите любимые каналы и доступное кино",
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.fillMaxWidth(0.62f)
                )
            }
        }

        Spacer(Modifier.height(18.dp))

        HomeCard(
            title = "ТВ каналы",
            subtitle = "Ваш M3U плейлист",
            badgeText = "TV",
            onClick = onOpenChannels
        )

        Spacer(Modifier.height(14.dp))

        HomeCard(
            title = "Фильмы",
            subtitle = "Internet Archive · открытые лицензии",
            badgeText = "🎬",
            onClick = onOpenMovies
        )
    }
}

@Composable
private fun HomeCard(
    title: String,
    subtitle: String,
    badgeText: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(AppColors.Card)
            .clickable { onClick() }
            .padding(18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1F1D2B)),
            contentAlignment = Alignment.Center
        ) {
            Text(badgeText, fontSize = 20.sp, color = AppColors.PurpleAccent, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = AppColors.TextDim, fontSize = 13.sp)
        }
        Text("›", color = AppColors.PurpleAccent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun BottomNavBar(current: Screen, onSelect: (Screen) -> Unit) {
    NavigationBar(
        containerColor = Color(0xFF0C0C10),
        contentColor = AppColors.TextDim2
    ) {
        NavigationBarItem(
            selected = current == Screen.HOME,
            onClick = { onSelect(Screen.HOME) },
            icon = { Text("🏠", fontSize = 18.sp) },
            label = { Text("Главная") },
            colors = navColors()
        )
        NavigationBarItem(
            selected = current == Screen.CHANNELS,
            onClick = { onSelect(Screen.CHANNELS) },
            icon = { Text("📺", fontSize = 18.sp) },
            label = { Text("ТВ") },
            colors = navColors()
        )
        NavigationBarItem(
            selected = current == Screen.MOVIES,
            onClick = { onSelect(Screen.MOVIES) },
            icon = { Text("🎬", fontSize = 18.sp) },
            label = { Text("Фильмы") },
            colors = navColors()
        )
        NavigationBarItem(
            selected = false,
            onClick = { /* TODO: settings/more screen */ },
            icon = { Text("☰", fontSize = 18.sp) },
            label = { Text("Ещё") },
            colors = navColors()
        )
    }
}

@Composable
private fun navColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = AppColors.PurpleAccent,
    selectedTextColor = AppColors.PurpleAccent,
    unselectedIconColor = AppColors.TextDim2,
    unselectedTextColor = AppColors.TextDim2,
    indicatorColor = Color(0xFF1C1A26)
)

@Composable
private fun ChannelListScreen(
    channels: List<Channel>,
    loading: Boolean,
    onBack: () -> Unit,
    onSelectChannel: (Channel) -> Unit
) {
    var search by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.Background)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Назад", color = AppColors.PurpleAccent) }
        }
        Text(
            "ТВ каналы",
            color = Color.White,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(horizontal = 18.dp)
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Поиск канала") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp)
        )
        Spacer(Modifier.height(10.dp))

        val filtered = channels.filter {
            it.name.contains(search, true) || it.group.contains(search, true)
        }

        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.PurpleAccent)
            }
        } else {
            Text(
                "${filtered.size} каналов",
                color = Color.LightGray,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp)
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered) { channel ->
                    Row(
                        Modifier.fillMaxWidth()
                            .clickable { onSelectChannel(channel) }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            modifier = Modifier.size(48.dp),
                            shape = RoundedCornerShape(10.dp),
                            color = Color(0xFF252831)
                        ) {}
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(channel.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text(
                                channel.group.ifBlank { "Без категории" },
                                color = Color.Gray,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoviesScreen(onBack: () -> Unit) {
    Column(
        Modifier.fillMaxSize().background(AppColors.Background)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Назад", color = AppColors.PurpleAccent) }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                "Раздел «Фильмы» скоро появится",
                color = AppColors.TextDim,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(32.dp)
            )
        }
    }
}

@Composable
fun PlayerScreen(channel: Channel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(channel.url) {
        ExoPlayer.Builder(context).build().apply {
            val itemBuilder = MediaItem.Builder().setUri(channel.url)
            channel.userAgent?.let {
                // User-Agent support will be wired through a custom HttpDataSource in the next build.
            }
            setMediaItem(itemBuilder.build())
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Назад", color = AppColors.PurpleAccent) }
            Text(
                channel.name,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        )
    }
}

private fun loadM3u(url: String): List<Channel> {
    return try {
        val text = URL(url).readText()
        parseM3u(text)
    } catch (_: Exception) {
        emptyList()
    }
}

private fun parseM3u(text: String): List<Channel> {
    val result = mutableListOf<Channel>()
    var name = ""
    var group = ""
    var logo: String? = null
    var ua: String? = null

    text.lineSequence().forEach { raw ->
        val line = raw.trim()
        when {
            line.startsWith("#EXTINF:", true) -> {
                name = line.substringAfterLast(",").trim()
                group = Regex("""group-title="([^"]*)"""", RegexOption.IGNORE_CASE)
                    .find(line)?.groupValues?.getOrNull(1).orEmpty()
                logo = Regex("""tvg-logo="([^"]*)"""", RegexOption.IGNORE_CASE)
                    .find(line)?.groupValues?.getOrNull(1)
            }
            line.startsWith("#EXTVLCOPT:http-user-agent=", true) -> {
                ua = line.substringAfter("=", "").trim()
            }
            line.startsWith("#") || line.isBlank() -> Unit
            else -> {
                if (name.isNotBlank()) {
                    result += Channel(name, group, logo, line, ua)
                }
                name = ""; group = ""; logo = null; ua = null
            }
        }
    }
    return result
}

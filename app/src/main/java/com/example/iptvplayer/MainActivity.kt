package com.example.iptvplayer

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    val PurpleBright = Color(0xFF9B4BFF)
    val PurpleDark = Color(0xFF202080)
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
        Scaffold(containerColor = AppColors.Background) { innerPadding ->
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

private fun loadAssetImage(context: Context, assetName: String): ImageBitmap? = try {
    val encoded = context.assets.open(assetName).bufferedReader().use { it.readText() }
    val bytes = Base64.decode(encoded, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
} catch (_: Exception) {
    null
}

@Composable
private fun HomeScreen(
    onOpenChannels: () -> Unit,
    onOpenMovies: () -> Unit
) {
    val context = LocalContext.current
    val channelsImage = remember { loadAssetImage(context, "iptv_channels_photo.webp.b64") }
    val moviesImage = remember { loadAssetImage(context, "movies_photo.webp.b64") }
    val heroImage = remember { loadAssetImage(context, "hero_tv.png.b64") }

    Column(
        Modifier
            .fillMaxSize()
            .background(AppColors.Background)
            .padding(horizontal = 20.dp)
    ) {
        Spacer(Modifier.height(14.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row {
                Text("IPTV ", color = AppColors.PurpleAccent, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text("Player", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
            }
            Box(
                Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF17161F)),
                contentAlignment = Alignment.Center
            ) {
                Text("⚙", color = Color.White, fontSize = 22.sp)
            }
        }

        Spacer(Modifier.height(18.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(210.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(AppColors.PurpleBright, AppColors.PurpleDark)))
                .clickable { onOpenChannels() }
        ) {
            if (heroImage != null) {
                Image(
                    bitmap = heroImage,
                    contentDescription = "TV",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(26.dp))
                )
            } else {
                Text("TV", color = Color.White, fontSize = 52.sp, fontWeight = FontWeight.ExtraBold,
                    modifier = Modifier.align(Alignment.Center))
            }
        }

        Spacer(Modifier.height(18.dp))
        HomeCard("ТВ каналы", "Ваш M3U плейлист", channelsImage, onOpenChannels)
        Spacer(Modifier.height(14.dp))
        HomeCard("Фильмы", "Internet Archive · открытые лицензии", moviesImage, onOpenMovies)
    }
}

@Composable
private fun HomeCard(
    title: String,
    subtitle: String,
    image: ImageBitmap?,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(132.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(AppColors.Card)
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (image != null) {
            Image(
                bitmap = image,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.width(150.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp))
            )
        } else {
            Box(
                Modifier.width(150.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(Color(0xFF1F1D2B)),
                contentAlignment = Alignment.Center
            ) {
                Text(title.take(2), color = AppColors.PurpleAccent, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(Modifier.padding(start = 18.dp).weight(1f)) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text(subtitle, color = AppColors.TextDim, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = AppColors.PurpleAccent, fontSize = 38.sp, modifier = Modifier.padding(end = 6.dp))
    }
}

@Composable
private fun ChannelListScreen(
    channels: List<Channel>,
    loading: Boolean,
    onBack: () -> Unit,
    onSelectChannel: (Channel) -> Unit
) {
    var search by remember { mutableStateOf("") }
    Column(Modifier.fillMaxSize().background(AppColors.Background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад", color = AppColors.PurpleAccent) }
        }
        Text("ТВ каналы", color = Color.White, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(horizontal = 18.dp))
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(value = search, onValueChange = { search = it }, placeholder = { Text("Поиск канала") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp))
        Spacer(Modifier.height(10.dp))
        val filtered = channels.filter { it.name.contains(search, true) || it.group.contains(search, true) }
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = AppColors.PurpleAccent) }
        } else {
            Text("${filtered.size} каналов", color = Color.LightGray, modifier = Modifier.padding(horizontal = 18.dp, vertical = 8.dp))
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered) { channel ->
                    Row(Modifier.fillMaxWidth().clickable { onSelectChannel(channel) }.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(modifier = Modifier.size(48.dp), shape = RoundedCornerShape(10.dp), color = Color(0xFF252831)) {}
                        Column(Modifier.padding(start = 12.dp)) {
                            Text(channel.name, color = Color.White, style = MaterialTheme.typography.titleMedium)
                            Text(channel.group.ifBlank { "Без категории" }, color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MoviesScreen(onBack: () -> Unit) {
    Column(Modifier.fillMaxSize().background(AppColors.Background)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад", color = AppColors.PurpleAccent) }
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Раздел «Фильмы» скоро появится", color = AppColors.TextDim, textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
        }
    }
}

@Composable
fun PlayerScreen(channel: Channel, onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val player = remember(channel.url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.Builder().setUri(channel.url).build())
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад", color = AppColors.PurpleAccent) }
            Text(channel.name, color = Color.White, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(start = 8.dp))
        }
        AndroidView(factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
    }
}

private fun loadM3u(url: String): List<Channel> = try {
    parseM3u(URL(url).readText())
} catch (_: Exception) {
    emptyList()
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
                group = Regex("""group-title="([^"]*)""", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1).orEmpty()
                logo = Regex("""tvg-logo="([^"]*)""", RegexOption.IGNORE_CASE).find(line)?.groupValues?.getOrNull(1)
            }
            line.startsWith("#EXTVLCOPT:http-user-agent=", true) -> ua = line.substringAfter("=", "").trim()
            line.startsWith("#") || line.isBlank() -> Unit
            else -> {
                if (name.isNotBlank()) result += Channel(name, group, logo, line, ua)
                name = ""; group = ""; logo = null; ua = null
            }
        }
    }
    return result
}

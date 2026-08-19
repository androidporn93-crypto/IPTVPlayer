package com.example.iptvplayer

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

private const val PLAYLIST_URL =
    "https://raw.githubusercontent.com/Dimonovich/TV/Dimonovich/FREE/.m3u"
private const val PREFS = "iptv_player"
private const val FAVORITES = "favorites"

private object AppColors {
    val Background = Color(0xFF050713)
    val Surface = Color(0x661A1835)
    val SurfaceStrong = Color(0xCC15162A)
    val Purple = Color(0xFFA447FF)
    val PurpleDeep = Color(0xFF5520B8)
    val Blue = Color(0xFF1769D5)
    val TextDim = Color(0xFFA5A3B9)
}

data class Channel(
    val name: String,
    val group: String,
    val logo: String?,
    val url: String,
    val userAgent: String? = null
)

data class Category(
    val id: String,
    val title: String,
    val icon: String,
    val matcher: (Channel) -> Boolean
)

private enum class Screen { HOME, CHANNELS, PLAYER }

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
    var selectedCategory by remember { mutableStateOf("all") }
    var loading by remember { mutableStateOf(true) }
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var favorites by remember { mutableStateOf(loadFavorites(prefs)) }

    LaunchedEffect(Unit) {
        channels = withContext(Dispatchers.IO) { loadM3u(PLAYLIST_URL) }
        loading = false
    }

    val categories = remember(channels, favorites) {
        buildCategories(favorites)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = AppColors.Background,
            surface = AppColors.SurfaceStrong,
            primary = AppColors.Purple
        )
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .background(spaceBackground())
        ) {
            when (screen) {
                Screen.HOME -> HomeScreen(
                    categories = categories,
                    channels = channels,
                    onOpenCategory = {
                        selectedCategory = it
                        screen = Screen.CHANNELS
                    }
                )
                Screen.CHANNELS -> ChannelListScreen(
                    categoryId = selectedCategory,
                    channels = channels,
                    favorites = favorites,
                    loading = loading,
                    onBack = { screen = Screen.HOME },
                    onToggleFavorite = { channel ->
                        favorites = toggleFavorite(prefs, favorites, channel)
                    },
                    onSelectChannel = {
                        selected = it
                        screen = Screen.PLAYER
                    }
                )
                Screen.PLAYER -> selected?.let { channel ->
                    PlayerScreen(channel, onBack = { screen = Screen.CHANNELS })
                }
            }
        }
    }
}

private fun spaceBackground(): Brush = Brush.radialGradient(
    colors = listOf(
        Color(0xFF24134F),
        Color(0xFF101331),
        AppColors.Background
    ),
    radius = 1200f
)

private fun loadAssetImage(context: Context, assetName: String): ImageBitmap? = try {
    val encoded = context.assets.open(assetName).bufferedReader().use { it.readText() }
    val bytes = Base64.decode(encoded, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
} catch (_: Exception) {
    null
}

private fun buildCategories(favorites: Set<String>): List<Category> = listOf(
    Category("all", "Все каналы", "📺") { true },
    Category("favorites", "Избранное", "⭐") { favorites.contains(it.url) },
    Category("uzbek", "Узбекские", "🇺🇿") { matches(it, "узбек", "uzbek", "uzb") },
    Category("news", "Новости", "📰") { matches(it, "новост", "news", "24") },
    Category("sport", "Спорт", "⚽") { matches(it, "спорт", "sport") },
    Category("movie", "Кино", "🎬") { matches(it, "кино", "movie", "film", "фильм") },
    Category("kids", "Детские", "🧸") { matches(it, "дет", "kids", "child", "мульт") },
    Category("music", "Музыка", "🎧") { matches(it, "музык", "music", "музыка") },
    Category("educational", "Познавательные", "📚") { matches(it, "позн", "educ", "science", "документ") },
    Category("entertainment", "Развлекательные", "🎭") { matches(it, "развлек", "entertain", "юмор", "шоу") },
    Category("regional", "Региональные", "📍") { matches(it, "регион", "region", "местн") },
    Category("foreign", "Зарубежные", "🌍") { matches(it, "зарубеж", "foreign", "international", "англ") },
    Category("other", "Остальные", "📁") { channel ->
        val known = listOf("узбек", "uzbek", "uzb", "новост", "news", "спорт", "sport", "кино", "movie", "film", "фильм", "дет", "kids", "child", "мульт", "музык", "music", "позн", "educ", "science", "документ", "развлек", "entertain", "юмор", "шоу", "регион", "region", "местн", "зарубеж", "foreign", "international", "англ")
        known.none { channel.group.contains(it, true) || channel.name.contains(it, true) }
    }
)

private fun matches(channel: Channel, vararg words: String): Boolean =
    words.any { channel.group.contains(it, true) || channel.name.contains(it, true) }

@Composable
private fun HomeScreen(
    categories: List<Category>,
    channels: List<Channel>,
    onOpenCategory: (String) -> Unit
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tablet = maxWidth >= 700.dp
        val columns = if (tablet) 4 else 3
        val horizontal = if (tablet) 28.dp else 14.dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = horizontal, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(columns) }) {
                Column {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "IPTVPlayer",
                        color = Color.White,
                        fontSize = if (tablet) 30.sp else 26.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(Modifier.height(12.dp))
                    HeroBanner(tablet)
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Категории",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            items(categories) { category ->
                val count = categoryCount(category, channels)
                CategoryCard(
                    category = category,
                    count = count,
                    highlighted = category.id == "all",
                    onClick = { onOpenCategory(category.id) }
                )
            }
        }
    }
}

@Composable
private fun HeroBanner(tablet: Boolean) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(if (tablet) 210.dp else 178.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF34105F), Color(0xFF071A43), Color(0xFF11122D))
                )
            )
            .shadow(10.dp, RoundedCornerShape(24.dp))
            .padding(horizontal = if (tablet) 28.dp else 20.dp)
    ) {
        Column(
            Modifier.align(Alignment.CenterStart),
            verticalArrangement = Arrangement.Center
        ) {
            Text("TV", color = Color.White, fontSize = if (tablet) 50.sp else 42.sp, fontWeight = FontWeight.Black)
            Text("В ХОРОШЕМ КАЧЕСТВЕ", color = Color.White, fontSize = if (tablet) 21.sp else 17.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Box(Modifier.width(72.dp).height(3.dp).clip(RoundedCornerShape(4.dp)).background(AppColors.Purple))
        }

        Box(
            Modifier
                .align(Alignment.CenterEnd)
                .width(if (tablet) 310.dp else 220.dp)
                .height(if (tablet) 145.dp else 112.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF070914))
                .shadow(12.dp, RoundedCornerShape(14.dp))
        ) {
            Text(
                "IPTVPlayer",
                color = Color.White,
                fontSize = if (tablet) 25.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .width(85.dp)
                    .height(8.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Color(0xFF25273A))
            )
        }

        Box(
            Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-8).dp, y = (-2).dp)
                .width(if (tablet) 115.dp else 88.dp)
                .height(30.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(Color(0xFF080910))
                .shadow(8.dp, RoundedCornerShape(15.dp))
        )
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    count: Int,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val border = if (highlighted) AppColors.Purple else Color(0x665F4B86)
    Column(
        Modifier
            .fillMaxWidth()
            .height(116.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(AppColors.Surface)
            .shadow(if (highlighted) 9.dp else 3.dp, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF4A3A75), Color(0xFF111126))
                    )
                )
                .shadow(7.dp, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(category.icon, fontSize = 31.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text(
            category.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "$count каналов",
            color = AppColors.TextDim,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

private fun categoryCount(category: Category, channels: List<Channel>): Int = when (category.id) {
    "favorites" -> channels.count { category.matcher(it) }
    else -> channels.count { category.matcher(it) }
}

@Composable
private fun ChannelListScreen(
    categoryId: String,
    channels: List<Channel>,
    favorites: Set<String>,
    loading: Boolean,
    onBack: () -> Unit,
    onToggleFavorite: (Channel) -> Unit,
    onSelectChannel: (Channel) -> Unit
) {
    var search by remember { mutableStateOf("") }
    val category = buildCategories(favorites).firstOrNull { it.id == categoryId }
    val title = category?.title ?: "Все каналы"
    val filtered = channels
        .filter { category?.matcher?.invoke(it) ?: true }
        .filter { it.name.contains(search, true) || it.group.contains(search, true) }

    Column(Modifier.fillMaxSize().background(spaceBackground())) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("← Назад", color = AppColors.Purple) }
            Text(title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Поиск канала") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp),
            shape = RoundedCornerShape(16.dp)
        )
        Spacer(Modifier.height(8.dp))
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AppColors.Purple)
            }
        } else {
            Text(
                "${filtered.size} каналов",
                color = AppColors.TextDim,
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 6.dp)
            )
            LazyColumn(Modifier.fillMaxSize()) {
                items(filtered) { channel ->
                    ChannelRow(
                        channel = channel,
                        favorite = favorites.contains(channel.url),
                        onToggleFavorite = { onToggleFavorite(channel) },
                        onClick = { onSelectChannel(channel) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    favorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0x6615172A))
            .clickable { onClick() }
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (!channel.logo.isNullOrBlank()) {
            AsyncImage(
                model = channel.logo,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(54.dp).clip(RoundedCornerShape(14.dp))
            )
        } else {
            Box(
                Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFF29263F)),
                contentAlignment = Alignment.Center
            ) { Text("TV", color = AppColors.Purple, fontWeight = FontWeight.Bold) }
        }
        Column(Modifier.weight(1f).padding(start = 12.dp)) {
            Text(channel.name, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(channel.group.ifBlank { "Телеканал" }, color = AppColors.TextDim, fontSize = 12.sp, maxLines = 1)
        }
        TextButton(onClick = onToggleFavorite) {
            Text(if (favorite) "★" else "☆", color = if (favorite) Color(0xFFFFC83D) else AppColors.TextDim, fontSize = 25.sp)
        }
        Text("›", color = AppColors.Purple, fontSize = 34.sp)
    }
}

@Composable
private fun PlayerScreen(channel: Channel, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(channel.url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.Builder().setUri(channel.url).build())
            prepare()
            playWhenReady = true
        }
    }
    DisposableEffect(player) { onDispose { player.release() } }
    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text("← Назад", color = AppColors.Purple) }
            Text(channel.name, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp))
        }
        AndroidView(
            factory = { ctx -> PlayerView(ctx).apply { this.player = player; useController = true } },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        )
    }
}

private fun loadFavorites(prefs: SharedPreferences): Set<String> =
    prefs.getStringSet(FAVORITES, emptySet())?.toSet() ?: emptySet()

private fun toggleFavorite(
    prefs: SharedPreferences,
    current: Set<String>,
    channel: Channel
): Set<String> {
    val next = current.toMutableSet()
    if (!next.add(channel.url)) next.remove(channel.url)
    prefs.edit().putStringSet(FAVORITES, next).apply()
    return next
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
                group = Regex("""group-title=\"([^\"]*)\"""", RegexOption.IGNORE_CASE)
                    .find(line)?.groupValues?.getOrNull(1).orEmpty()
                logo = Regex("""tvg-logo=\"([^\"]*)\"""", RegexOption.IGNORE_CASE)
                    .find(line)?.groupValues?.getOrNull(1)
            }
            line.startsWith("#EXTVLCOPT:http-user-agent=", true) -> ua = line.substringAfter("=", "").trim()
            line.startsWith("#") || line.isBlank() -> Unit
            else -> {
                if (name.isNotBlank()) result += Channel(name, group, logo, line, ua)
                name = ""
                group = ""
                logo = null
                ua = null
            }
        }
    }
    return result
}

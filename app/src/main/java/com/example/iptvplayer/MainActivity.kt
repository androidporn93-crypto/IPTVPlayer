package com.example.iptvplayer

import android.content.Context
import android.content.SharedPreferences
import android.graphics.BitmapFactory
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.graphicsLayer
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
    val Background = Color(0xFF05030D)
    val Surface = Color(0x66130D26)
    val SurfaceStrong = Color(0xE6100C1F)
    val Purple = Color(0xFFB04CFF)
    val PurpleSoft = Color(0xFFDB8BFF)
    val PurpleDeep = Color(0xFF5E18B8)
    val White = Color(0xFFF7F4FF)
    val TextDim = Color(0xFFAAA1C4)
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

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = AppColors.Background,
            surface = AppColors.SurfaceStrong,
            primary = AppColors.Purple,
            onBackground = AppColors.White,
            onSurface = AppColors.White
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            AnimatedSpaceBackground()

            when (screen) {
                Screen.HOME -> HomeScreen(
                    channels = channels,
                    favorites = favorites,
                    onOpenCategory = { categoryId ->
                        selectedCategory = categoryId
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

@Composable
private fun AnimatedSpaceBackground() {
    val transition = rememberInfiniteTransition(label = "space")
    val pulse by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(tween(2200), RepeatMode.Reverse),
        label = "spacePulse"
    )

    Canvas(Modifier.fillMaxSize()) {
        drawRect(
            brush = Brush.radialGradient(
                colors = listOf(
                    Color(0xFF35105E).copy(alpha = pulse),
                    Color(0xFF0E0A1D).copy(alpha = 0.92f),
                    AppColors.Background
                ),
                radius = size.maxDimension * 0.9f
            )
        )

        val points = listOf(
            0.08f to 0.09f, 0.77f to 0.08f, 0.93f to 0.18f,
            0.18f to 0.28f, 0.56f to 0.27f, 0.86f to 0.36f,
            0.05f to 0.58f, 0.38f to 0.51f, 0.74f to 0.58f,
            0.94f to 0.72f, 0.19f to 0.84f, 0.65f to 0.91f
        )
        points.forEachIndexed { index, (x, y) ->
            val alpha = if (index % 2 == 0) pulse else 0.25f + pulse * 0.35f
            drawCircle(
                color = AppColors.PurpleSoft.copy(alpha = alpha),
                radius = if (index % 3 == 0) 2.2f else 1.2f,
                center = androidx.compose.ui.geometry.Offset(size.width * x, size.height * y)
            )
        }
    }
}

@Composable
private fun HomeScreen(
    channels: List<Channel>,
    favorites: Set<String>,
    onOpenCategory: (String) -> Unit
) {
    val categories = remember(favorites) { buildCategories(favorites) }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tablet = maxWidth >= 700.dp
        val columns = if (tablet) 4 else 3
        val horizontal = if (tablet) 28.dp else 14.dp

        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = horizontal, vertical = 18.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item(span = { GridItemSpan(columns) }) {
                Column {
                    Header()
                    Spacer(Modifier.height(14.dp))
                    HeroBanner(tablet)
                    Spacer(Modifier.height(16.dp))
                    SectionTitle("Категории")
                    Spacer(Modifier.height(2.dp))
                }
            }

            items(categories.filter { it.id != "other" }) { category ->
                CategoryCard(
                    category = category,
                    count = channels.count { category.matcher(it) },
                    highlighted = category.id == "all",
                    onClick = { onOpenCategory(category.id) }
                )
            }

            item(span = { GridItemSpan(columns) }) {
                Spacer(Modifier.height(2.dp))
                DifferentCard(
                    count = channels.count { buildCategories(favorites).first { c -> c.id == "other" }.matcher(it) },
                    onClick = { onOpenCategory("other") }
                )
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun Header() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "IPTV",
            color = AppColors.Purple,
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold
        )
        Text(
            " Player",
            color = AppColors.White,
            fontSize = 27.sp,
            fontWeight = FontWeight.ExtraBold
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            title,
            color = AppColors.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.width(14.dp))
        Box(
            Modifier
                .weight(1f)
                .height(1.5.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(AppColors.Purple.copy(alpha = 0.9f), Color.Transparent)
                    )
                )
        )
    }
}

@Composable
private fun HeroBanner(tablet: Boolean) {
    val context = LocalContext.current
    val hero by produceState<ImageBitmap?>(initialValue = null, key1 = context) {
        value = withContext(Dispatchers.IO) { loadAssetImage(context, "hero_tv.png.b64") }
    }

    Box(
        Modifier
            .fillMaxWidth()
            .height(if (tablet) 220.dp else 196.dp)
            .clip(RoundedCornerShape(24.dp))
            .shadow(14.dp, RoundedCornerShape(24.dp))
            .background(Color(0xFF100826))
    ) {
        if (hero != null) {
            androidx.compose.foundation.Image(
                bitmap = hero!!,
                contentDescription = "TV",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                Color.Black.copy(alpha = 0.20f),
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.10f)
                            )
                        )
                    )
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF34105F), Color(0xFF071A43), Color(0xFF101027))
                        )
                    )
            )
            Text(
                "TV",
                color = AppColors.White,
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 22.dp)
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: Category,
    count: Int,
    highlighted: Boolean,
    onClick: () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "cardGlow-${category.id}")
    val glow by transition.animateFloat(
        initialValue = if (highlighted) 0.35f else 0.10f,
        targetValue = if (highlighted) 0.80f else 0.32f,
        animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Reverse),
        label = "glow"
    )

    Column(
        Modifier
            .fillMaxWidth()
            .height(134.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0x66100B20))
            .shadow(
                elevation = if (highlighted) (10f * glow).dp else 4.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = AppColors.Purple.copy(alpha = glow),
                spotColor = AppColors.Purple.copy(alpha = glow)
            )
            .clickable { onClick() }
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .size(68.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(
                    Brush.radialGradient(
                        listOf(
                            AppColors.Purple.copy(alpha = 0.15f + glow * 0.20f),
                            Color(0xFF0A0714)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(category.icon, fontSize = 36.sp)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            category.title,
            color = AppColors.White,
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            "$count каналов",
            color = AppColors.TextDim,
            fontSize = 10.5.sp,
            maxLines = 1
        )
    }
}

@Composable
private fun DifferentCard(count: Int, onClick: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "differentGlow")
    val pulse by transition.animateFloat(
        initialValue = 0.70f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "differentPulse"
    )
    val scale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1500), RepeatMode.Reverse),
        label = "logoScale"
    )

    Row(
        Modifier
            .fillMaxWidth()
            .height(104.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFF28104C), Color(0xFF0B0715), Color(0xFF28104C))
                )
            )
            .shadow(
                elevation = (13f * pulse).dp,
                shape = RoundedCornerShape(22.dp),
                ambientColor = AppColors.Purple.copy(alpha = 0.35f * pulse),
                spotColor = AppColors.Purple.copy(alpha = 0.65f * pulse)
            )
            .clickable { onClick() }
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(70.dp)
                .graphicsLayer(scaleX = scale, scaleY = scale),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0x221A0D2A))
            )
            Box(
                Modifier
                    .size(58.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        Brush.radialGradient(
                            listOf(AppColors.PurpleSoft.copy(alpha = 0.95f), AppColors.PurpleDeep)
                        )
                    )
                    .shadow(
                        16.dp,
                        RoundedCornerShape(50),
                        ambientColor = AppColors.Purple.copy(alpha = pulse),
                        spotColor = AppColors.Purple.copy(alpha = pulse)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("IPTV", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    Text("Player", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.width(18.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "Разные",
                color = AppColors.White,
                fontSize = 25.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "$count каналов",
                color = AppColors.PurpleSoft.copy(alpha = pulse),
                fontSize = 11.sp
            )
        }
        Text("›", color = AppColors.PurpleSoft.copy(alpha = pulse), fontSize = 40.sp)
    }
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
    Category("other", "Разные", "✨") { channel ->
        val known = listOf(
            "узбек", "uzbek", "uzb", "новост", "news", "спорт", "sport", "кино", "movie", "film", "фильм",
            "дет", "kids", "child", "мульт", "музык", "music", "позн", "educ", "science", "документ",
            "развлек", "entertain", "юмор", "шоу", "регион", "region", "местн", "зарубеж", "foreign",
            "international", "англ"
        )
        known.none { channel.group.contains(it, true) || channel.name.contains(it, true) }
    }
)

private fun matches(channel: Channel, vararg words: String): Boolean =
    words.any { channel.group.contains(it, true) || channel.name.contains(it, true) }

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

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) { Text("←", color = AppColors.Purple, fontSize = 26.sp) }
            Text(title, color = AppColors.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
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
            .background(Color(0x66100B20))
            .clickable { onClick() }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
            if (!channel.logo.isNullOrBlank()) {
                AsyncImage(
                    model = channel.logo,
                    contentDescription = channel.name,
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(10.dp))
                )
            } else {
                Text("TV", color = AppColors.PurpleSoft, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(channel.name, color = AppColors.White, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(channel.group, color = AppColors.TextDim, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        TextButton(onClick = onToggleFavorite) {
            Text(if (favorite) "★" else "☆", color = AppColors.PurpleSoft, fontSize = 24.sp)
        }
    }
}

@Composable
private fun PlayerScreen(channel: Channel, onBack: () -> Unit) {
    val context = LocalContext.current
    val player = remember(channel.url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(channel.url))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose { player.release() }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            Modifier.fillMaxWidth().background(Color(0xFF08050F)).padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBack) {
                Text("←", color = AppColors.PurpleSoft, fontSize = 26.sp)
            }
            Text(
                channel.name,
                color = AppColors.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = true
                    this.player = player
                }
            },
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
        )
        Text(
            channel.group,
            color = AppColors.TextDim,
            fontSize = 13.sp,
            modifier = Modifier.padding(14.dp)
        )
    }
}

private fun loadAssetImage(context: Context, assetName: String): ImageBitmap? = try {
    val encoded = context.assets.open(assetName).bufferedReader().use { it.readText() }
    val bytes = Base64.decode(encoded, Base64.DEFAULT)
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
} catch (_: Exception) {
    null
}

private fun loadFavorites(prefs: SharedPreferences): Set<String> =
    prefs.getStringSet(FAVORITES, emptySet())?.toSet() ?: emptySet()

private fun toggleFavorite(
    prefs: SharedPreferences,
    current: Set<String>,
    channel: Channel
): Set<String> {
    val updated = current.toMutableSet()
    if (!updated.add(channel.url)) updated.remove(channel.url)
    prefs.edit().putStringSet(FAVORITES, updated).apply()
    return updated.toSet()
}

private fun loadM3u(url: String): List<Channel> {
    return try {
        val result = mutableListOf<Channel>()
        val lines = URL(url).openStream().bufferedReader().use { it.readLines() }
        var currentName: String? = null
        var currentGroup = ""
        var currentLogo: String? = null
        var currentUserAgent: String? = null

        for (raw in lines) {
            val line = raw.trim()
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                currentName = line.substringAfterLast(",").trim().ifBlank { "Канал" }
                currentGroup = extractAttribute(line, "group-title")
                currentLogo = extractAttribute(line, "tvg-logo").ifBlank { null }
                currentUserAgent = extractAttribute(line, "http-user-agent").ifBlank { null }
            } else if (line.isNotBlank() && !line.startsWith("#") && currentName != null) {
                result += Channel(
                    name = currentName!!,
                    group = currentGroup.ifBlank { "Без категории" },
                    logo = currentLogo,
                    url = line,
                    userAgent = currentUserAgent
                )
                currentName = null
                currentGroup = ""
                currentLogo = null
                currentUserAgent = null
            }
        }
        result
    } catch (_: Exception) {
        emptyList()
    }
}

private fun extractAttribute(line: String, key: String): String {
    val quoted = Regex("$key=\\\"([^\\\"]*)\\\"").find(line)
    if (quoted != null) return quoted.groupValues[1]
    val unquoted = Regex("$key=([^\\s,]+)").find(line)
    return unquoted?.groupValues?.getOrNull(1).orEmpty()
}

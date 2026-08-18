from pathlib import Path
import base64
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'app/src/main/java/com/example/iptvplayer/MainActivity.kt'
RES = ROOT / 'app/src/main/res/drawable-nodpi'
ASSETS = ROOT / 'app/src/main/assets'
RES.mkdir(parents=True, exist_ok=True)

# Use the exact supplied hero artwork from the reference design.
hero_src = ASSETS / 'hero_tv.png.b64'
hero_dst = RES / 'hero_tv.png'
if hero_src.exists():
    hero_dst.write_bytes(base64.b64decode(hero_src.read_text().strip()))

text = SOURCE.read_text()


def replace_composable(name: str, replacement: str) -> None:
    global text
    pattern = re.compile(
        r'@Composable\s+private fun ' + re.escape(name) + r'\b.*?\n}\n\n(?=@Composable|private fun)',
        re.S,
    )
    new_text, count = pattern.subn(replacement + '\n\n', text, count=1)
    if count != 1:
        raise SystemExit(f'Could not replace composable {name}; count={count}')
    text = new_text


replace_composable('IPTVApp', r'''@Composable
private fun IPTVApp() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var page by remember { mutableStateOf("home") }
    var query by remember { mutableStateOf("") }
    var channels by remember { mutableStateOf<List<Channel>>(emptyList()) }
    var movies by remember { mutableStateOf<List<Movie>>(emptyList()) }
    var favorites by remember { mutableStateOf(loadFavorites(prefs)) }
    var loading by remember { mutableStateOf(true) }
    var movieLoading by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<Int?>(null) }

    fun toggleFavorite(channel: Channel) {
        favorites = favorites.toMutableSet().apply {
            if (!add(channel.url)) remove(channel.url)
        }
        saveFavorites(prefs, favorites)
    }

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
            PlayerScreen(
                channels = channels,
                index = selected!!,
                isFavorite = channels[selected!!].url in favorites,
                onToggleFavorite = { toggleFavorite(channels[selected!!]) },
                onSelect = { selected = it },
                onBack = { selected = null }
            )
            return@MaterialTheme
        }

        // Keep the existing bottom navigation. The reference screen clearly has it.
        Scaffold(
            containerColor = BG,
            bottomBar = { BottomBar(page) { page = it; query = "" } }
        ) { padding ->
            Column(Modifier.fillMaxSize().background(BG).padding(padding)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (page != "home") {
                        Text(
                            "‹",
                            color = Color.White,
                            fontSize = 40.sp,
                            modifier = Modifier.clickable { page = "home" }
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                    Text("IPTV", color = PURPLE, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(" Player", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(46.dp).background(Color(0xFF101720), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("⚙", color = Color.White, fontSize = 25.sp)
                    }
                }

                Box(Modifier.weight(1f)) {
                    when (page) {
                        "channels" -> Channels(channels, query, { query = it }, loading, favorites) { selected = it }
                        "movies" -> Movies(movies, query, { query = it }, movieLoading)
                        "favorites" -> FavoritesPage(channels, favorites, { selected = it }, ::toggleFavorite)
                        else -> Home({ page = "channels" }, { page = "movies" })
                    }
                }
            }
        }
    }
}''')

replace_composable('Home', r'''@Composable
private fun Home(tv: () -> Unit, movie: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        // Exact supplied hero artwork, not a reconstructed drawing.
        Box(
            Modifier.fillMaxWidth().height(205.dp)
                .clip(RoundedCornerShape(24.dp))
        ) {
            Image(
                painter = painterResource(R.drawable.hero_tv),
                contentDescription = "TV",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(Modifier.height(16.dp))
        HomeBtn("tv", "ТВ каналы", "Ваш M3U плейлист", tv)
        Spacer(Modifier.height(10.dp))
        HomeBtn("movie", "Фильмы", "Internet Archive · открытые лицензии", movie)
    }
}''')

replace_composable('HomeBtn', r'''@Composable
private fun HomeBtn(kind: String, title: String, sub: String, on: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .height(145.dp)
            .background(CARD, RoundedCornerShape(20.dp))
            .clickable(onClick = on)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (kind == "movie") {
            Image(
                painter = painterResource(R.drawable.home_movies_icon),
                contentDescription = title,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.width(150.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp))
            )
        } else {
            ThreeDHomeIcon(kind, Modifier.width(150.dp).fillMaxHeight())
        }

        Column(Modifier.padding(start = 20.dp).weight(1f)) {
            Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                sub,
                color = MUTED,
                fontSize = 14.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }

        Text("›", color = PURPLE, fontSize = 42.sp, modifier = Modifier.padding(end = 8.dp))
    }
}''')

# Only the home-related composables above are changed. Channels, movies, favorites and player remain untouched.
SOURCE.write_text(text)
print('Home redesigned to match the left-side reference, including hero artwork, 3D channel icon, movie artwork and bottom navigation.')

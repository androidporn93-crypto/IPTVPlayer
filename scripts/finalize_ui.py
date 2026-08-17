from pathlib import Path
import base64
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'app/src/main/java/com/example/iptvplayer/MainActivity.kt'
RES = ROOT / 'app/src/main/res/drawable-nodpi'
ASSETS = ROOT / 'app/src/main/assets'
RES.mkdir(parents=True, exist_ok=True)

# Make sure the two supplied card images are real drawable resources in the APK.
for src_name, dst_name in {
    'iptv_channels_photo.webp.b64': 'iptv_channels_photo.webp',
    'movies_photo.webp.b64': 'movies_photo.webp',
}.items():
    src = ASSETS / src_name
    dst = RES / dst_name
    if src.exists():
        dst.write_bytes(base64.b64decode(src.read_text().strip()))

text = SOURCE.read_text()

if 'import androidx.compose.ui.layout.ContentScale' not in text:
    text = text.replace('import androidx.compose.ui.input.pointer.pointerInput\n', 'import androidx.compose.ui.input.pointer.pointerInput\nimport androidx.compose.ui.layout.ContentScale\n')


def replace_composable(name: str, replacement: str) -> None:
    global text
    pattern = re.compile(r'@Composable\s+private fun ' + re.escape(name) + r'\b.*?\n}\n\n(?=@Composable|private fun)', re.S)
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

        Scaffold(containerColor = BG) { padding ->
            Column(Modifier.fillMaxSize().background(BG).padding(padding)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (page != "home") {
                        Text("‹", color = Color.White, fontSize = 40.sp, modifier = Modifier.clickable { page = "home" })
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
        Box(
            Modifier.fillMaxWidth().height(198.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF7C2FE7), Color(0xFF1E2B8D))), RoundedCornerShape(26.dp))
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Column(Modifier.align(Alignment.CenterStart)) {
                Text("TV", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Black)
                Text("Смотрите", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("любимые каналы", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("и доступное кино", color = Color.White.copy(.82f), fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Column(Modifier.align(Alignment.CenterEnd), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier.width(150.dp).height(96.dp)
                        .background(Color(0xFF11131B), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier.size(48.dp).background(Color(0xFF5B21B6), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("▶", color = Color.White, fontSize = 24.sp) }
                }
                Spacer(Modifier.height(7.dp))
                Box(Modifier.width(70.dp).height(8.dp).background(Color(0xFF171A22), RoundedCornerShape(4.dp)))
            }
        }
        Spacer(Modifier.height(16.dp))
        HomeBtn("tv", "ТВ каналы", "Ваш M3U плейлист", tv)
        HomeBtn("movie", "Фильмы", "Internet Archive · открытые лицензии", movie)
    }
}''')

replace_composable('HomeBtn', r'''@Composable
private fun HomeBtn(kind: String, title: String, sub: String, on: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(152.dp)
            .background(CARD, RoundedCornerShape(21.dp))
            .clickable(onClick = on)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(if (kind == "movie") R.drawable.movies_photo else R.drawable.iptv_channels_photo),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(150.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp))
        )
        Column(Modifier.padding(start = 20.dp).weight(1f)) {
            Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(sub, color = MUTED, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = PURPLE, fontSize = 42.sp, modifier = Modifier.padding(end = 8.dp))
    }
    Spacer(Modifier.height(10.dp))
}''')

replace_composable('PlayerScreen', r'''@Composable
private fun PlayerScreen(channels: List<Channel>, index: Int, isFavorite: Boolean, onToggleFavorite: () -> Unit, onSelect: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val channel = channels[index]
    var fullscreen by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
    var isPlaying by remember { mutableStateOf(true) }
    var showControls by remember { mutableStateOf(true) }
    var controlsTick by remember { mutableIntStateOf(0) }

    val player = remember(channel.url, channel.userAgent) {
        val httpFactory = DefaultHttpDataSource.Factory()
            .setUserAgent(channel.userAgent.ifBlank { UA })
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15000)
            .setReadTimeoutMs(20000)
        ExoPlayer.Builder(context).setMediaSourceFactory(DefaultMediaSourceFactory(httpFactory)).build().apply {
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlayerError(error: PlaybackException) { errorText = "Не удалось загрузить канал\\n${error.errorCodeName}" }
                override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
            })
            setMediaItem(MediaItem.fromUri(channel.url))
            prepare()
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
    }

    BackHandler {
        if (fullscreen) fullscreen = false else onBack()
    }

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
        showControls = true
        controlsTick++
    }

    LaunchedEffect(controlsTick) {
        delay(3000)
        showControls = false
    }

    if (fullscreen) {
        Box(
            Modifier.fillMaxSize().background(Color.Black).pointerInput(Unit) {
                detectTapGestures(onTap = { showControls = !showControls; controlsTick++ })
            }
        ) {
            VideoView(player, true, Modifier.fillMaxSize())
            if (showControls) {
                PlayCircleButton(62.dp, Modifier.align(Alignment.Center)) {
                    if (player.isPlaying) player.pause() else player.play()
                    showControls = true; controlsTick++
                }
                HeartButton(isFavorite, 44.dp, Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                    onToggleFavorite(); showControls = true; controlsTick++
                }
                FullscreenButton(46.dp, Modifier.align(Alignment.BottomEnd).padding(16.dp)) {
                    fullscreen = false
                }
            }
            errorText?.let { ErrorMessage(it, Modifier.align(Alignment.Center).padding(top = 90.dp)) }
        }
    } else {
        Column(Modifier.fillMaxSize().background(BG)) {
            Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color.Black).pointerInput(Unit) {
                    detectTapGestures(onTap = { showControls = !showControls; controlsTick++ })
                }
            ) {
                VideoView(player, false, Modifier.fillMaxSize())
                if (showControls) {
                    Box(
                        Modifier.align(Alignment.TopStart).padding(10.dp).size(42.dp)
                            .background(Color.Black.copy(.52f), CircleShape)
                            .clickable { onBack() },
                        contentAlignment = Alignment.Center
                    ) { Text("‹", color = Color.White, fontSize = 28.sp) }
                    HeartButton(isFavorite, 44.dp, Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                        onToggleFavorite(); showControls = true; controlsTick++
                    }
                    PlayCircleButton(58.dp, Modifier.align(Alignment.Center)) {
                        if (player.isPlaying) player.pause() else player.play()
                        showControls = true; controlsTick++
                    }
                    FullscreenButton(46.dp, Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                        fullscreen = true; controlsTick++
                    }
                }
                errorText?.let { ErrorMessage(it, Modifier.align(Alignment.Center)) }
            }
            Text("${index + 1}. ${channel.name}", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 3.dp))
            Text("Следующие каналы", color = MUTED, fontSize = 13.sp, modifier = Modifier.padding(horizontal = 16.dp))
            LazyColumn(contentPadding = PaddingValues(12.dp, 10.dp, 12.dp, 24.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                itemsIndexed(channels.drop(index + 1)) { offset, next ->
                    val nextIndex = index + 1 + offset
                    Row(
                        Modifier.fillMaxWidth().height(68.dp).background(CARD, RoundedCornerShape(12.dp)).clickable { onSelect(nextIndex) }.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("${nextIndex + 1}", color = PURPLE, fontSize = 16.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(36.dp))
                        ChannelAvatar(nextIndex, next, 46.dp, 34.dp)
                        Column(Modifier.padding(start = 10.dp).weight(1f)) {
                            Text(next.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(next.group.ifBlank { "ТВ канал" }, color = MUTED, fontSize = 10.sp)
                        }
                        PlayCircleButton(34.dp) { onSelect(nextIndex) }
                    }
                }
            }
        }
    }
}''')

replace_composable('PlayCircleButton', r'''@Composable
private fun PlayCircleButton(size: Dp = 42.dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.size(size).background(Color(0xFF4C1D95), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        PlayPauseIcon(false, size * .34f)
    }
}''')

replace_composable('FullscreenButton', r'''@Composable
private fun FullscreenButton(size: Dp = 46.dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.size(size).background(Color.Black.copy(.65f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * .40f)) {
            val s = this.size
            val sw = 2.2f
            drawLine(Color.White, Offset(2f, 7f), Offset(2f, 2f), sw)
            drawLine(Color.White, Offset(2f, 2f), Offset(7f, 2f), sw)
            drawLine(Color.White, Offset(s.width - 7f, 2f), Offset(s.width - 2f, 2f), sw)
            drawLine(Color.White, Offset(s.width - 2f, 2f), Offset(s.width - 2f, 7f), sw)
            drawLine(Color.White, Offset(2f, s.height - 7f), Offset(2f, s.height - 2f), sw)
            drawLine(Color.White, Offset(2f, s.height - 2f), Offset(7f, s.height - 2f), sw)
            drawLine(Color.White, Offset(s.width - 7f, s.height - 2f), Offset(s.width - 2f, s.height - 2f), sw)
            drawLine(Color.White, Offset(s.width - 2f, s.height - 2f), Offset(s.width - 2f, s.height - 7f), sw)
        }
    }
}''')

replace_composable('HeartButton', r'''@Composable
private fun HeartButton(favorite: Boolean, size: Dp = 42.dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier.size(size).background(Color.Black.copy(.62f), CircleShape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(size * .52f)) {
            val s = this.size
            val heart = Path().apply {
                moveTo(s.width * .50f, s.height * .88f)
                cubicTo(s.width * .08f, s.height * .58f, s.width * .12f, s.height * .15f, s.width * .38f, s.height * .22f)
                cubicTo(s.width * .47f, s.height * .24f, s.width * .50f, s.height * .34f, s.width * .50f, s.height * .40f)
                cubicTo(s.width * .50f, s.height * .34f, s.width * .53f, s.height * .24f, s.width * .62f, s.height * .22f)
                cubicTo(s.width * .88f, s.height * .15f, s.width * .92f, s.height * .58f, s.width * .50f, s.height * .88f)
            }
            if (favorite) drawPath(heart, PURPLE) else drawPath(heart, Color.White, style = Stroke(width = 3.2f))
        }
    }
}''')

SOURCE.write_text(text)
print('Final UI patch applied')

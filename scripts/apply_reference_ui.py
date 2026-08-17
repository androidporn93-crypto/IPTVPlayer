from pathlib import Path
import base64
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / 'app/src/main/java/com/example/iptvplayer/MainActivity.kt'
RES = ROOT / 'app/src/main/res/drawable-nodpi'
ASSETS = ROOT / 'app/src/main/assets'
RES.mkdir(parents=True, exist_ok=True)

# Copy the exact reference hero image and supplied home-card images into build resources.
for src_name, dst_name in {
    'hero_tv.png.b64': 'hero_tv.png',
    'iptv_channels_photo.webp.b64': 'iptv_channels_photo.webp',
    'movies_photo.webp.b64': 'movies_photo.webp',
}.items():
    src = ASSETS / src_name
    if src.exists():
        (RES / dst_name).write_bytes(base64.b64decode(src.read_text().strip()))

text = SOURCE.read_text()

if 'import androidx.compose.ui.layout.ContentScale' not in text:
    text = text.replace(
        'import androidx.compose.ui.input.pointer.pointerInput\n',
        'import androidx.compose.ui.input.pointer.pointerInput\nimport androidx.compose.ui.layout.ContentScale\n',
        1,
    )

# Main shell: remove bottom navigation and use reference header.
text = text.replace(
    'Scaffold(containerColor = BG, bottomBar = { BottomBar(page) { page = it; query = "" } }) { padding ->',
    'Scaffold(containerColor = BG) { padding ->',
    1,
)
text = text.replace(
    'Text("IPTV Player", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(18.dp))',
    '''Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("IPTV", color = PURPLE, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(" Player", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(46.dp).background(Color(0xFF111722), CircleShape), contentAlignment = Alignment.Center) {
                        Text("⚙", color = Color.White, fontSize = 25.sp)
                    }
                }''',
    1,
)


def replace_function(name: str, replacement: str) -> None:
    global text
    pattern = re.compile(
        r'@Composable\s+private fun ' + re.escape(name) + r'\b.*?\n}\n\n(?=@Composable|private fun)',
        re.S,
    )
    text2, count = pattern.subn(replacement + '\n\n', text, count=1)
    if count != 1:
        raise SystemExit(f'UI patch failed: {name} count={count}')
    text = text2


replace_function('Home', '''@Composable
private fun Home(tv: () -> Unit, movie: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(
            Modifier.fillMaxWidth().height(190.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF7B2CE5), Color(0xFF1F2C8D))), RoundedCornerShape(25.dp))
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Column(Modifier.align(Alignment.CenterStart)) {
                Text("TV", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Black)
                Text("Смотрите", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("любимые каналы", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("и доступное кино", color = Color.White.copy(.82f), fontSize = 16.sp, modifier = Modifier.padding(top = 3.dp))
            }
            Image(
                painter = painterResource(R.drawable.hero_tv),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.align(Alignment.CenterEnd).width(190.dp).height(166.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        HomeBtn("tv", "ТВ каналы", "Ваш M3U плейлист", tv)
        HomeBtn("movie", "Фильмы", "Internet Archive · открытые лицензии", movie)
    }
}''')

replace_function('HomeBtn', '''@Composable
private fun HomeBtn(kind: String, title: String, sub: String, on: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(150.dp)
            .background(CARD, RoundedCornerShape(20.dp))
            .clickable(onClick = on)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(if (kind == "movie") R.drawable.movies_photo else R.drawable.iptv_channels_photo),
            contentDescription = title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.width(148.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp)),
        )
        Column(Modifier.padding(start = 18.dp).weight(1f)) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(sub, color = MUTED, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = PURPLE, fontSize = 40.sp, modifier = Modifier.padding(end = 6.dp))
    }
    Spacer(Modifier.height(10.dp))
}''')

replace_function('PlayerScreen', '''@Composable
private fun PlayerScreen(channels: List<Channel>, index: Int, isFavorite: Boolean, onToggleFavorite: () -> Unit, onSelect: (Int) -> Unit, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as ComponentActivity
    val channel = channels[index]
    var fullscreen by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }
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
                PlayCircleButton(72.dp, Modifier.align(Alignment.Center)) {
                    if (player.isPlaying) player.pause() else player.play()
                    showControls = true
                    controlsTick++
                }
                HeartButton(isFavorite, 44.dp, Modifier.align(Alignment.TopEnd).padding(14.dp)) {
                    onToggleFavorite()
                    showControls = true
                    controlsTick++
                }
                FullscreenButton(46.dp, Modifier.align(Alignment.BottomEnd).padding(18.dp)) { fullscreen = false }
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
                        contentAlignment = Alignment.Center,
                    ) { Text("‹", color = Color.White, fontSize = 28.sp) }
                    HeartButton(isFavorite, 44.dp, Modifier.align(Alignment.TopEnd).padding(10.dp)) {
                        onToggleFavorite()
                        showControls = true
                        controlsTick++
                    }
                    PlayCircleButton(58.dp, Modifier.align(Alignment.Center)) {
                        if (player.isPlaying) player.pause() else player.play()
                        showControls = true
                        controlsTick++
                    }
                    FullscreenButton(46.dp, Modifier.align(Alignment.BottomEnd).padding(12.dp)) {
                        fullscreen = true
                        controlsTick++
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
                        verticalAlignment = Alignment.CenterVertically,
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

# Fix previous Compose shadow/overload problems without changing the requested layout.
text = text.replace('size.width', 'this.size.width')
text = text.replace('size.height', 'this.size.height')
text = text.replace('Box(Modifier.align(Alignment.Center), Modifier) {', 'Box(modifier = Modifier.align(Alignment.Center)) {')

required = [
    'Scaffold(containerColor = BG) { padding ->',
    'R.drawable.hero_tv',
    'R.drawable.iptv_channels_photo',
    'R.drawable.movies_photo',
    'FullscreenButton(46.dp',
]
missing = [item for item in required if item not in text]
if missing:
    raise SystemExit('Reference UI patch incomplete: ' + ', '.join(missing))

SOURCE.write_text(text)
print('Reference UI applied and validated')

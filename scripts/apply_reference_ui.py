from pathlib import Path
import base64

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "app/src/main/java/com/example/iptvplayer/MainActivity.kt"
RES = ROOT / "app/src/main/res/drawable-nodpi"
ASSETS = ROOT / "app/src/main/assets"
RES.mkdir(parents=True, exist_ok=True)

for src_name, dst_name in {
    "hero_tv.png.b64": "hero_tv.png",
    "iptv_channels_photo.webp.b64": "iptv_channels_photo.webp",
    "movies_photo.webp.b64": "movies_photo.webp",
}.items():
    src = ASSETS / src_name
    if src.exists():
        (RES / dst_name).write_bytes(base64.b64decode(src.read_text().strip()))

text = SOURCE.read_text()

# Imports needed by the reference artwork.
if "import androidx.compose.ui.layout.ContentScale" not in text:
    text = text.replace(
        "import androidx.compose.ui.input.pointer.pointerInput\n",
        "import androidx.compose.ui.input.pointer.pointerInput\nimport androidx.compose.ui.layout.ContentScale\n",
        1,
    )

# Remove the bottom navigation and replace the header.
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

old_home = '''@Composable
private fun Home(tv: () -> Unit, movie: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Box(Modifier.fillMaxWidth().height(150.dp).background(Brush.linearGradient(listOf(Color(0xFF6D28D9), Color(0xFF172B8A))), RoundedCornerShape(22.dp)).padding(20.dp)) {
            Column {
                Text("TV", color = Color.White.copy(.28f), fontSize = 54.sp, fontWeight = FontWeight.Black)
                Text("Смотрите любимые каналы", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                Text("и доступное кино", color = Color.White.copy(.8f))
            }
        }
        Spacer(Modifier.height(18.dp))
        HomeBtn("tv", "ТВ каналы", "Ваш M3U плейлист", tv)
        HomeBtn("movie", "Фильмы", "Internet Archive · открытые лицензии", movie)
    }
}
'''
new_home = '''@Composable
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
}
'''
if old_home not in text:
    raise SystemExit("Original Home block not found")
text = text.replace(old_home, new_home, 1)

old_home_btn = '''@Composable
private fun HomeBtn(kind: String, title: String, sub: String, on: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(124.dp).padding(vertical = 5.dp).background(CARD, RoundedCornerShape(16.dp)).clickable(onClick = on).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        if (kind == "movie") Image(painterResource(R.drawable.home_movies_icon), "Фильмы", Modifier.size(104.dp).clip(RoundedCornerShape(14.dp))) else ThreeDHomeIcon(kind, Modifier.size(94.dp))
        Column(Modifier.padding(start = 14.dp).weight(1f)) {
            Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(sub, color = MUTED, fontSize = 13.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = PURPLE, fontSize = 34.sp)
    }
}
'''
new_home_btn = '''@Composable
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
}
'''
if old_home_btn not in text:
    raise SystemExit("Original HomeBtn block not found")
text = text.replace(old_home_btn, new_home_btn, 1)

# Fix the fullscreen control placement only; leave the proven player implementation intact.
old_full = '''            if (showControls) {
                PlayCircleButton(58.dp) { if (player.isPlaying) player.pause() else player.play(); showControls = true; controlsTick++ }
                Box(Modifier.align(Alignment.Center)) {
                    PlayPauseIcon(isPlaying, 28.dp)
                }
                FullscreenButton(46.dp, Modifier.align(Alignment.BottomEnd).padding(18.dp)) { fullscreen = false }
                Box(Modifier.align(Alignment.Center), Modifier) {
                    PlayCircleButton(74.dp) { if (player.isPlaying) player.pause() else player.play(); controlsTick++ }
                }
            }
'''
new_full = '''            if (showControls) {
                PlayCircleButton(74.dp, Modifier.align(Alignment.Center)) {
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
'''
if old_full in text:
    text = text.replace(old_full, new_full, 1)
else:
    # At minimum, align any remaining fullscreen play button so it cannot sit top-left.
    text = text.replace('if (showControls) {\n                PlayCircleButton(58.dp) {', 'if (showControls) {\n                PlayCircleButton(74.dp, Modifier.align(Alignment.Center)) {', 1)

# Repair the Canvas scope-shadowing issue from older revisions.
text = text.replace('size.width', 'this.size.width')
text = text.replace('size.height', 'this.size.height')

required = [
    'Scaffold(containerColor = BG) { padding ->',
    'R.drawable.hero_tv',
    'R.drawable.iptv_channels_photo',
    'R.drawable.movies_photo',
    'Modifier.align(Alignment.Center)',
]
missing = [item for item in required if item not in text]
if missing:
    raise SystemExit('Reference UI patch incomplete: ' + ', '.join(missing))

SOURCE.write_text(text)
print('Reference UI applied successfully')

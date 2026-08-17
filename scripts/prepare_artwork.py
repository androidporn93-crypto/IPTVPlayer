from pathlib import Path
import base64
import re

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
DRAWABLE = RES / 'drawable-nodpi'
DRAWABLE_XML = RES / 'drawable'
DRAWABLE.mkdir(parents=True, exist_ok=True)
DRAWABLE_XML.mkdir(parents=True, exist_ok=True)

# Decode supplied card artwork at build time.
for src_name, dst_name in {
    'iptv_channels_photo.webp.b64': 'iptv_channels_photo.webp',
    'movies_photo.webp.b64': 'movies_photo.webp',
}.items():
    src = ROOT / 'app/src/main/assets' / src_name
    if src.exists():
        (DRAWABLE / dst_name).write_bytes(base64.b64decode(src.read_text().strip()))

# Small vector used inside the hero banner so the top block matches the supplied mockup.
(DRAWABLE_XML / 'hero_tv.xml').write_text('''<?xml version="1.0" encoding="utf-8"?>
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="180dp" android:height="150dp" android:viewportWidth="180" android:viewportHeight="150">
    <path android:fillColor="#161A28" android:pathData="M37,16 L151,8 Q164,8 166,21 L166,103 Q166,112 157,114 L43,114 Q30,113 30,101 L30,28 Q30,18 37,16 Z"/>
    <path android:fillColor="#080B12" android:pathData="M42,23 L153,17 Q158,17 158,23 L158,95 Q158,101 152,102 L43,102 Q36,102 36,95 L36,30 Q36,24 42,23 Z"/>
    <path android:fillColor="#A855F7" android:pathData="M96,39 A27,27 0,1 0,96,93 A27,27 0,1 0,96,39 Z"/>
    <path android:fillColor="#F7F4FF" android:pathData="M88,52 L88,80 L111,66 Z"/>
    <path android:fillColor="#161A28" android:pathData="M78,114 L116,114 L125,127 L67,127 Z"/>
    <path android:fillColor="#0F1420" android:pathData="M60,128 L132,128 L143,134 L49,134 Z"/>
    <path android:fillColor="#151A28" android:pathData="M136,112 L173,123 L169,137 L132,126 Z"/>
    <path android:fillColor="#3F2A5B" android:pathData="M141,119 L170,128 L169,134 L140,125 Z"/>
    <path android:fillColor="#FFFFFF" android:fillAlpha="0.85" android:pathData="M145,121 l7,2 l-1,3 l-7,-2 Z M155,124 l7,2 l-1,3 l-7,-2 Z M165,127 l4,1 l-1,3 l-4,-1 Z"/>
</vector>''', encoding='utf-8')

source = ROOT / 'app/src/main/java/com/example/iptvplayer/MainActivity.kt'
text = source.read_text()

# ----- Main shell: no bottom navigation + reference-style header -----
text = text.replace(
    'Scaffold(containerColor = BG, bottomBar = { BottomBar(page) { page = it; query = "" } }) { padding ->',
    'Scaffold(containerColor = BG) { padding ->',
    1,
)

old_header = 'Text("IPTV Player", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(18.dp))'
new_header = '''Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("IPTV", color = PURPLE, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(" Player", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Box(
                        Modifier.size(46.dp).background(Color(0xFF111722), CircleShape),
                        contentAlignment = Alignment.Center
                    ) { Text("⚙", color = Color.White, fontSize = 25.sp) }
                }'''
text = text.replace(old_header, new_header, 1)

# ----- Replace Home() and HomeBtn() as a single block. This is intentionally regex based,
# so every build converges to the same reference layout even if the previous APK was built
# from an older source revision. -----
home_pattern = re.compile(r'@Composable\nprivate fun Home\(tv: \(\) -> Unit, movie: \(\) -> Unit\) \{.*?\n\}\n\n@Composable\nprivate fun ThreeDHomeIcon', re.S)
new_home = '''@Composable
private fun Home(tv: () -> Unit, movie: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(
            Modifier.fillMaxWidth().height(188.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF7C2CE6), Color(0xFF20328F))), RoundedCornerShape(24.dp))
                .padding(horizontal = 22.dp, vertical = 18.dp)
        ) {
            Column(Modifier.align(Alignment.CenterStart)) {
                Text("TV", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Black)
                Text("Смотрите", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("любимые каналы", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("и доступное кино", color = Color.White.copy(.82f), fontSize = 16.sp)
            }
            Image(
                painter = painterResource(R.drawable.hero_tv),
                contentDescription = null,
                modifier = Modifier.align(Alignment.CenterEnd).width(188.dp).height(150.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        HomeBtn("tv", "ТВ каналы", "Ваш M3U плейлист", tv)
        HomeBtn("movie", "Фильмы", "Internet Archive · открытые лицензии", movie)
    }
}

@Composable
private fun HomeBtn(kind: String, title: String, sub: String, on: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(150.dp)
            .background(Color(0xFF10161F), RoundedCornerShape(20.dp))
            .clickable(onClick = on)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(if (kind == "movie") R.drawable.movies_photo else R.drawable.iptv_channels_photo),
            contentDescription = title,
            modifier = Modifier.size(148.dp).clip(RoundedCornerShape(16.dp))
        )
        Column(Modifier.padding(start = 18.dp).weight(1f)) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(sub, color = MUTED, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = PURPLE, fontSize = 40.sp)
    }
}

@Composable
private fun ThreeDHomeIcon'''
text, _ = home_pattern.subn(new_home, text, count=1)

# ----- Remove the accidental second/third play controls in fullscreen. -----
text = re.sub(r'\n\s*PlayCircleButton\(58\.dp\) \{ if \(player\.isPlaying\) player\.pause\(\) else player\.play\(\); showControls = true; controlsTick\+\+ \}', '', text, count=1)
text = re.sub(r'\n\s*Box\(Modifier\.align\(Alignment\.Center\)\) \{\n\s*PlayPauseIcon\(isPlaying, 28\.dp\)\n\s*\}', '', text, count=1)
text = text.replace(
    'Box(Modifier.align(Alignment.Center), Modifier) {',
    'Box(modifier = Modifier.align(Alignment.Center)) {',
    1,
)

# Fullscreen controls: exactly one central Play/Pause and Fullscreen bottom-right.
old_fullscreen = '''            if (showControls) {
                FullscreenButton(46.dp, Modifier.align(Alignment.BottomEnd).padding(18.dp)) { fullscreen = false }
                Box(modifier = Modifier.align(Alignment.Center)) {
                    PlayCircleButton(74.dp) { if (player.isPlaying) player.pause() else player.play(); controlsTick++ }
                }
            }'''
new_fullscreen = '''            if (showControls) {
                Box(modifier = Modifier.align(Alignment.Center)) {
                    PlayCircleButton(74.dp) { if (player.isPlaying) player.pause() else player.play(); controlsTick++ }
                }
                FullscreenButton(46.dp, Modifier.align(Alignment.BottomEnd).padding(18.dp)) { fullscreen = false }
            }'''
text = text.replace(old_fullscreen, new_fullscreen, 1)

source.write_text(text)
print('Reference home screen, hero vector, no bottom navigation, and fullscreen controls applied')

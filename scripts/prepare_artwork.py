from pathlib import Path
import base64
import re

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res/drawable-nodpi'
RES.mkdir(parents=True, exist_ok=True)

# Decode the two supplied home-card images into drawable-nodpi at build time.
for src_name, dst_name in {
    'iptv_channels_photo.webp.b64': 'iptv_channels_photo.webp',
    'movies_photo.webp.b64': 'movies_photo.webp',
}.items():
    src = ROOT / 'app/src/main/assets' / src_name
    if src.exists():
        (RES / dst_name).write_bytes(base64.b64decode(src.read_text().strip()))

source = ROOT / 'app/src/main/java/com/example/iptvplayer/MainActivity.kt'
text = source.read_text()

# ----- Main page: no bottom bar + use the supplied card artwork -----
text = text.replace(
    'Scaffold(containerColor = BG, bottomBar = { BottomBar(page) { page = it; query = "" } }) { padding ->',
    'Scaffold(containerColor = BG) { padding ->',
    1,
)

text = text.replace(
    'Text("IPTV Player", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(18.dp))',
    'Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {\n                    Text("IPTV", color = PURPLE, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)\n                    Text(" Player", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)\n                }',
    1,
)

home_btn = re.compile(r'@Composable\nprivate fun HomeBtn\(kind: String, title: String, sub: String, on: \(\) -> Unit\) \{.*?\n\}\n\n@Composable\nprivate fun ThreeDHomeIcon', re.S)
new_home_btn = '''@Composable
private fun HomeBtn(kind: String, title: String, sub: String, on: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(150.dp)
            .background(CARD, RoundedCornerShape(20.dp))
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
text, home_count = home_btn.subn(new_home_btn, text, count=1)

# If the old HomeBtn was already partially changed, also catch the direct image branch.
text = text.replace(
    'if (kind == "movie") Image(painterResource(R.drawable.home_movies_icon), "Фильмы", Modifier.size(104.dp).clip(RoundedCornerShape(14.dp))) else ThreeDHomeIcon(kind, Modifier.size(94.dp))',
    'Image(painter = painterResource(if (kind == "movie") R.drawable.movies_photo else R.drawable.iptv_channels_photo), contentDescription = title, modifier = Modifier.size(148.dp).clip(RoundedCornerShape(16.dp)))',
    1,
)

# ----- Channel logos: never replace a real playlist logo with the generic two-letter avatar. -----
# Keep playlist-provided tvg-logo images; fall back to the polished channel-specific drawing.

# ----- Fullscreen player: remove the accidental duplicate Play button in the top-left. -----
text = text.replace(
    '                PlayCircleButton(58.dp) { if (player.isPlaying) player.pause() else player.play(); showControls = true; controlsTick++ }\n',
    '',
    1,
)

# Make the fullscreen central button the only play/pause control and keep fullscreen bottom-right.
text = text.replace(
    '                Box(Modifier.align(Alignment.Center), Modifier) {\n                    PlayCircleButton(74.dp) { if (player.isPlaying) player.pause() else player.play(); controlsTick++ }\n                }',
    '                Box(modifier = Modifier.align(Alignment.Center)) {\n                    PlayCircleButton(74.dp) { if (player.isPlaying) player.pause() else player.play(); controlsTick++ }\n                }',
    1,
)

source.write_text(text)
print(f'Artwork/home patched={home_count}, fullscreen duplicate play removed')

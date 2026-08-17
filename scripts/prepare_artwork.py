from pathlib import Path
import base64

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res/drawable-nodpi'
RES.mkdir(parents=True, exist_ok=True)

for src_name, dst_name in {
    'iptv_channels_photo.webp.b64': 'iptv_channels_photo.webp',
    'movies_photo.webp.b64': 'movies_photo.webp',
}.items():
    src = ROOT / 'app/src/main/assets' / src_name
    if src.exists():
        dst = RES / dst_name
        dst.write_bytes(base64.b64decode(src.read_text().strip()))

source = ROOT / 'app/src/main/java/com/example/iptvplayer/MainActivity.kt'
text = source.read_text()

home_old = '''        if (kind == "movie") {
            Image(
                painter = painterResource(R.drawable.home_movies_icon),
                contentDescription = "Фильмы",
                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(14.dp))
            )
        } else {
            ThreeDHomeIcon(kind, Modifier.size(94.dp))
        }'''
home_new = '''        if (kind == "movie") {
            Image(
                painter = painterResource(R.drawable.movies_photo),
                contentDescription = "Фильмы",
                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(14.dp))
            )
        } else {
            Image(
                painter = painterResource(R.drawable.iptv_channels_photo),
                contentDescription = "ТВ каналы",
                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(14.dp))
            )
        }'''
if home_old in text:
    text = text.replace(home_old, home_new, 1)

fullscreen_old = '''            if (showControls) {
                PlayCircleButton(58.dp) { if (player.isPlaying) player.pause() else player.play(); showControls = true; controlsTick++ }
                Box(Modifier.align(Alignment.Center)) {
                    PlayPauseIcon(isPlaying, 28.dp)
                }
                FullscreenButton(46.dp, Modifier.align(Alignment.BottomEnd).padding(18.dp)) { fullscreen = false }
                Box(Modifier.align(Alignment.Center), Modifier) {
                    PlayCircleButton(74.dp) { if (player.isPlaying) player.pause() else player.play(); controlsTick++ }
                }
            }'''
fullscreen_new = '''            if (showControls) {
                HeartButton(isFavorite, 42.dp, Modifier.align(Alignment.TopEnd).padding(12.dp)) { onToggleFavorite(); controlsTick++ }
                Box(
                    Modifier.align(Alignment.Center)
                        .size(74.dp)
                        .background(Color.Black.copy(.68f), CircleShape)
                        .clickable { if (player.isPlaying) player.pause() else player.play(); controlsTick++ },
                    Alignment.Center
                ) {
                    PlayPauseIcon(isPlaying, 32.dp)
                }
                FullscreenButton(46.dp, Modifier.align(Alignment.BottomEnd).padding(18.dp)) { fullscreen = false; controlsTick++ }
            }'''
if fullscreen_old in text:
    text = text.replace(fullscreen_old, fullscreen_new, 1)

portrait_old = '''                if (showControls) {
                    Box(Modifier.align(Alignment.TopStart).padding(10.dp).size(40.dp).background(Color.Black.copy(.52f), CircleShape).clickable { onBack() }, Alignment.Center) { Text("‹", color = Color.White, fontSize = 26.sp) }
                    HeartButton(isFavorite, 42.dp, Modifier.align(Alignment.TopEnd).padding(10.dp)) { onToggleFavorite(); showControls = true; controlsTick++ }
                    Box(Modifier.align(Alignment.Center)) { PlayPauseIcon(isPlaying, 34.dp); Modifier.clickable { if (player.isPlaying) player.pause() else player.play(); controlsTick++ } }
                    FullscreenButton(46.dp, Modifier.align(Alignment.BottomEnd).padding(12.dp)) { fullscreen = true; controlsTick++ }
                }'''
portrait_new = '''                if (showControls) {
                    Box(Modifier.align(Alignment.TopStart).padding(10.dp).size(40.dp).background(Color.Black.copy(.52f), CircleShape).clickable { onBack() }, Alignment.Center) { Text("‹", color = Color.White, fontSize = 26.sp) }
                    HeartButton(isFavorite, 42.dp, Modifier.align(Alignment.TopEnd).padding(10.dp)) { onToggleFavorite(); controlsTick++ }
                    Box(
                        Modifier.align(Alignment.Center)
                            .size(68.dp)
                            .background(Color.Black.copy(.68f), CircleShape)
                            .clickable { if (player.isPlaying) player.pause() else player.play(); controlsTick++ },
                        Alignment.Center
                    ) {
                        PlayPauseIcon(isPlaying, 30.dp)
                    }
                    FullscreenButton(46.dp, Modifier.align(Alignment.BottomEnd).padding(12.dp)) { fullscreen = true; controlsTick++ }
                }'''
if portrait_old in text:
    text = text.replace(portrait_old, portrait_new, 1)

fullscreen_button_old_start = '@Composable\nprivate fun FullscreenButton('
heart_button_old_start = '@Composable\nprivate fun HeartButton('
fs_start = text.find(fullscreen_button_old_start)
heart_start = text.find(heart_button_old_start, fs_start)
video_start = text.find('@Composable\nprivate fun VideoView', heart_start)
if fs_start < 0 or heart_start < 0 or video_start < 0:
    raise SystemExit('Player button definitions not found')
button_defs = '''@Composable
private fun FullscreenButton(buttonSize: Dp = 46.dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(buttonSize)
            .background(Color.Black.copy(.65f), CircleShape)
            .clickable(onClick = onClick),
        Alignment.Center
    ) {
        Text("⛶", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun HeartButton(favorite: Boolean, buttonSize: Dp = 42.dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.size(buttonSize)
            .background(Color.Black.copy(.62f), CircleShape)
            .clickable(onClick = onClick),
        Alignment.Center
    ) {
        Text(
            if (favorite) "♥" else "♡",
            color = if (favorite) PURPLE else Color.White,
            fontSize = 24.sp
        )
    }
}

'''
text = text[:fs_start] + button_defs + text[video_start:]

source.write_text(text)
print('Artwork and player controls patched successfully')

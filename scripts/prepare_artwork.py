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
if fullscreen_old not in text:
    raise SystemExit('Fullscreen control block not found')
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
if portrait_old not in text:
    raise SystemExit('Portrait control block not found')
text = text.replace(portrait_old, portrait_new, 1)

fullscreen_button_old = '''@Composable
private fun FullscreenButton(size: Dp = 46.dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(size).background(Color.Black.copy(.65f), CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Canvas(Modifier.size(size * .40f)) {
            val sw = 2.2f
            drawLine(Color.White, Offset(2f, 7f), Offset(2f, 2f), sw); drawLine(Color.White, Offset(2f, 2f), Offset(7f, 2f), sw)
            drawLine(Color.White, Offset(size.width - 7f, 2f), Offset(size.width - 2f, 2f), sw); drawLine(Color.White, Offset(size.width - 2f, 2f), Offset(size.width - 2f, 7f), sw)
            drawLine(Color.White, Offset(2f, size.height - 7f), Offset(2f, size.height - 2f), sw); drawLine(Color.White, Offset(2f, size.height - 2f), Offset(7f, size.height - 2f), sw)
            drawLine(Color.White, Offset(size.width - 7f, size.height - 2f), Offset(size.width - 2f, size.height - 2f), sw); drawLine(Color.White, Offset(size.width - 2f, size.height - 2f), Offset(size.width - 2f, size.height - 7f), sw)
        }
    }
}'''
fullscreen_button_new = '''@Composable
private fun FullscreenButton(buttonSize: Dp = 46.dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(buttonSize).background(Color.Black.copy(.65f), CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Canvas(Modifier.size(buttonSize * .40f)) {
            val sw = 2.2f
            val w = this.size.width
            val h = this.size.height
            drawLine(Color.White, Offset(2f, 7f), Offset(2f, 2f), sw); drawLine(Color.White, Offset(2f, 2f), Offset(7f, 2f), sw)
            drawLine(Color.White, Offset(w - 7f, 2f), Offset(w - 2f, 2f), sw); drawLine(Color.White, Offset(w - 2f, 2f), Offset(w - 2f, 7f), sw)
            drawLine(Color.White, Offset(2f, h - 7f), Offset(2f, h - 2f), sw); drawLine(Color.White, Offset(2f, h - 2f), Offset(7f, h - 2f), sw)
            drawLine(Color.White, Offset(w - 7f, h - 2f), Offset(w - 2f, h - 2f), sw); drawLine(Color.White, Offset(w - 2f, h - 2f), Offset(w - 2f, h - 7f), sw)
        }
    }
}'''
if fullscreen_button_old not in text:
    raise SystemExit('FullscreenButton definition not found')
text = text.replace(fullscreen_button_old, fullscreen_button_new, 1)

heart_button_old = '''@Composable
private fun HeartButton(favorite: Boolean, size: Dp = 42.dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(size).background(Color.Black.copy(.62f), CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Canvas(Modifier.size(size * .48f)) {
            val w = size.width; val h = size.height
            val heart = Path().apply {
                moveTo(w * .50f, h * .88f)
                cubicTo(w * .08f, h * .58f, w * .12f, h * .15f, w * .38f, h * .22f)
                cubicTo(w * .47f, h * .24f, w * .50f, h * .34f, w * .50f, h * .40f)
                cubicTo(w * .50f, h * .34f, w * .53f, h * .24f, w * .62f, h * .22f)
                cubicTo(w * .88f, h * .15f, w * .92f, h * .58f, w * .50f, h * .88f)
            }
            drawPath(heart, if (favorite) PURPLE else Color.White, style = Stroke(width = 3.0f))
            if (favorite) drawPath(heart, PURPLE)
        }
    }
}'''
heart_button_new = '''@Composable
private fun HeartButton(favorite: Boolean, buttonSize: Dp = 42.dp, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(modifier.size(buttonSize).background(Color.Black.copy(.62f), CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Canvas(Modifier.size(buttonSize * .48f)) {
            val w = this.size.width
            val h = this.size.height
            val heart = Path().apply {
                moveTo(w * .50f, h * .88f)
                cubicTo(w * .08f, h * .58f, w * .12f, h * .15f, w * .38f, h * .22f)
                cubicTo(w * .47f, h * .24f, w * .50f, h * .34f, w * .50f, h * .40f)
                cubicTo(w * .50f, h * .34f, w * .53f, h * .24f, w * .62f, h * .22f)
                cubicTo(w * .88f, h * .15f, w * .92f, h * .58f, w * .50f, h * .88f)
            }
            drawPath(heart, if (favorite) PURPLE else Color.White, style = Stroke(width = 3.0f))
            if (favorite) drawPath(heart, PURPLE)
        }
    }
}'''
if heart_button_old not in text:
    raise SystemExit('HeartButton definition not found')
text = text.replace(heart_button_old, heart_button_new, 1)

source.write_text(text)
print('Artwork and player controls patched successfully')

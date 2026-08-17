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

home_old = '''        if (kind == "movie") Image(painterResource(R.drawable.home_movies_icon), "Фильмы", Modifier.size(104.dp).clip(RoundedCornerShape(14.dp))) else ThreeDHomeIcon(kind, Modifier.size(94.dp))'''
home_new = '''        Image(
            painter = painterResource(if (kind == "movie") R.drawable.movies_photo else R.drawable.iptv_channels_photo),
            contentDescription = title,
            modifier = Modifier.size(104.dp).clip(RoundedCornerShape(14.dp))
        )'''
if home_old in text:
    text = text.replace(home_old, home_new, 1)

home_old_multiline = '''        if (kind == "movie") {
            Image(
                painter = painterResource(R.drawable.home_movies_icon),
                contentDescription = "Фильмы",
                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(14.dp))
            )
        } else {
            ThreeDHomeIcon(kind, Modifier.size(94.dp))
        }'''
if home_old_multiline in text:
    text = text.replace(home_old_multiline, home_new, 1)

logo_start = text.find('@Composable\nprivate fun ChannelLogo(')
player_start = text.find('@Composable\nprivate fun PlayerScreen', logo_start)
if logo_start >= 0 and player_start > logo_start:
    logo_new = '''@Composable
private fun ChannelLogo(index: Int, name: String, size: Dp = 40.dp) {
    val key = name.lowercase()
    val colors = when {
        key.contains("россия 24") -> listOf(Color(0xFF3D73D8), Color(0xFF153A9A))
        key.contains("россия-к") || key.contains("россия к") -> listOf(Color(0xFF27B56B), Color(0xFF08733F))
        key.contains("россия 1") -> listOf(Color(0xFFE73538), Color(0xFF9D111C))
        key.contains("нтв хит") -> listOf(Color(0xFF5B88B8), Color(0xFF274B78))
        key.contains("нтв сериал") -> listOf(Color(0xFF4F83B9), Color(0xFF264A75))
        key.contains("нтв-право") || key.contains("нтв право") -> listOf(Color(0xFFE7442D), Color(0xFF9E2418))
        key.contains("нтв стиль") -> listOf(Color(0xFFF08A22), Color(0xFF9B4E0B))
        key.contains("первый канал") -> listOf(Color(0xFFE43B3F), Color(0xFF9D1D25))
        else -> listOf(Color(0xFF4A74A8), Color(0xFF263A57))
    }
    Box(Modifier.size(size).background(Brush.linearGradient(colors), RoundedCornerShape(10.dp)), Alignment.Center) {
        when {
            key.contains("россия 24") -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("РОССИЯ", color = Color.White, fontSize = (size.value * 0.20f).sp, fontWeight = FontWeight.Bold)
                Text("24", color = Color.White, fontSize = (size.value * 0.36f).sp, fontWeight = FontWeight.ExtraBold)
            }
            key.contains("россия-к") || key.contains("россия к") -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("РОССИЯ", color = Color.White, fontSize = (size.value * 0.18f).sp, fontWeight = FontWeight.Bold)
                Text("К", color = Color.White, fontSize = (size.value * 0.38f).sp, fontWeight = FontWeight.ExtraBold)
            }
            key.contains("россия 1") -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("РОССИЯ", color = Color.White, fontSize = (size.value * 0.17f).sp, fontWeight = FontWeight.Bold)
                Text("1", color = Color.White, fontSize = (size.value * 0.42f).sp, fontWeight = FontWeight.Black)
            }
            key.contains("нтв") -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("НТВ", color = Color.White, fontSize = (size.value * 0.28f).sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    when {
                        key.contains("хит") -> "ХИТ"
                        key.contains("сериал") -> "СЕРИАЛ"
                        key.contains("право") -> "ПРАВО"
                        key.contains("стиль") -> "СТИЛЬ"
                        else -> ""
                    },
                    color = Color.White.copy(alpha = 0.92f),
                    fontSize = (size.value * 0.13f).sp,
                    fontWeight = FontWeight.Bold
                )
            }
            key.contains("первый канал") -> Text("1", color = Color.White, fontSize = (size.value * 0.46f).sp, fontWeight = FontWeight.Black)
            else -> Text(name.take(2).uppercase(), color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = (size.value * 0.26f).sp)
        }
    }
}

'''
    text = text[:logo_start] + logo_new + text[player_start:]

source.write_text(text)
print('Artwork and channel logos patched successfully')

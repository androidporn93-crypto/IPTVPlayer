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
    dst = RES / dst_name
    dst.write_bytes(base64.b64decode(src.read_text().strip()))
    src.unlink(missing_ok=True)

source = ROOT / 'app/src/main/java/com/example/iptvplayer/MainActivity.kt'
text = source.read_text()
old = '''        if (kind == "movie") {
            Image(
                painter = painterResource(R.drawable.home_movies_icon),
                contentDescription = "Фильмы",
                modifier = Modifier.size(104.dp).clip(RoundedCornerShape(14.dp))
            )
        } else {
            ThreeDHomeIcon(kind, Modifier.size(94.dp))
        }'''
new = '''        if (kind == "movie") {
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
if old not in text:
    raise SystemExit('HomeBtn block not found')
source.write_text(text.replace(old, new, 1))
print('Artwork prepared and HomeBtn patched')

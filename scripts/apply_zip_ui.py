from pathlib import Path
import re

source = Path('app/src/main/java/com/example/iptvplayer/MainActivity.kt')
text = source.read_text()

replacement = r'''@Composable
private fun HomeBtn(kind: String, title: String, sub: String, on: () -> Unit) {
    Row(
        Modifier.fillMaxWidth()
            .height(145.dp)
            .background(CARD, RoundedCornerShape(20.dp))
            .clickable(onClick = on)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(
                if (kind == "movie") R.drawable.movies_photo else R.drawable.iptv_channels_photo
            ),
            contentDescription = title,
            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            modifier = Modifier.width(150.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp))
        )
        Column(Modifier.padding(start = 20.dp).weight(1f)) {
            Text(title, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(sub, color = MUTED, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = PURPLE, fontSize = 42.sp, modifier = Modifier.padding(end = 8.dp))
    }
}'''

pattern = re.compile(r'@Composable\s+private fun HomeBtn\b.*?\n}\n\n(?=@Composable|private fun)', re.S)
text, count = pattern.subn(replacement + '\n\n', text, count=1)
if count != 1:
    raise SystemExit(f'HomeBtn not found: {count}')

source.write_text(text)
print('ZIP UI cards applied: iptv_channels_photo.webp and movies_photo.webp')

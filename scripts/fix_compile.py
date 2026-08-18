from pathlib import Path
import re

source = Path('app/src/main/java/com/example/iptvplayer/MainActivity.kt')
text = source.read_text()

# Keep the compile fixes required by the existing Compose source.
text = text.replace('size.width', 'this.size.width')
text = text.replace('size.height', 'this.size.height')
text = text.replace(
    'Box(modifier.size(size).background(Color(0xFF4C1D95), CircleShape).clickable(onClick = onClick), Alignment.Center)',
    'Box(modifier = modifier.size(size).background(Color(0xFF4C1D95), CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center)'
)
text = text.replace(
    'Box(modifier.size(size).background(Color.Black.copy(.65f), CircleShape).clickable(onClick = onClick), Alignment.Center)',
    'Box(modifier = modifier.size(size).background(Color.Black.copy(.65f), CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center)'
)
text = text.replace(
    'Box(modifier.size(size).background(Color.Black.copy(.62f), CircleShape).clickable(onClick = onClick), Alignment.Center)',
    'Box(modifier = modifier.size(size).background(Color.Black.copy(.62f), CircleShape).clickable(onClick = onClick), contentAlignment = Alignment.Center)'
)
text = text.replace('Box(Modifier.align(Alignment.Center), Modifier) {', 'Box(modifier = Modifier.align(Alignment.Center)) {')

if 'import androidx.compose.ui.layout.ContentScale' not in text:
    text = text.replace(
        'import androidx.compose.ui.input.pointer.pointerInput\n',
        'import androidx.compose.ui.input.pointer.pointerInput\nimport androidx.compose.ui.layout.ContentScale\n'
    )

# Keep the existing navigation for channels/movies/favorites, but hide it only on Home.
old_scaffold = '''        Scaffold(containerColor = BG, bottomBar = { BottomBar(page) { page = it; query = "" } }) { padding ->
            Column(Modifier.fillMaxSize().background(BG).padding(padding)) {
                Text("IPTV Player", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(18.dp))
                Box(Modifier.weight(1f)) {'''
new_scaffold = '''        Scaffold(containerColor = BG, bottomBar = {
            if (page != "home") BottomBar(page) { page = it; query = "" }
        }) { padding ->
            Column(Modifier.fillMaxSize().background(BG).padding(padding)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("IPTV", color = PURPLE, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(" Player", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.weight(1f))
                    Box(Modifier.size(46.dp).background(Color(0xFF101720), CircleShape), contentAlignment = Alignment.Center) {
                        Text("⚙", color = Color.White, fontSize = 25.sp)
                    }
                }
                Box(Modifier.weight(1f)) {'''
if old_scaffold not in text:
    raise SystemExit('Expected working scaffold was not found')
text = text.replace(old_scaffold, new_scaffold, 1)

# Replace only Home and HomeBtn. Use only existing image resources; no new hero resource is loaded at runtime.
pattern_home = re.compile(r'@Composable\s+private fun Home\b.*?\n}\n\n@Composable\s+private fun HomeBtn\b.*?\n}\n\n(?=@Composable|private fun)', re.S)
new_home = '''@Composable
private fun Home(tv: () -> Unit, movie: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(
            Modifier.fillMaxWidth().height(205.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF7C2FE7), Color(0xFF2637A7))), RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
        ) {
            Column(
                Modifier.align(Alignment.CenterStart)
                    .padding(start = 22.dp)
                    .fillMaxWidth(.58f)
            ) {
                Text("TV", color = Color.White, fontSize = 58.sp, fontWeight = FontWeight.Black)
                Text("Смотрите", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("любимые каналы", color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                Text("и доступное кино", color = Color.White.copy(.86f), fontSize = 15.sp, modifier = Modifier.padding(top = 4.dp))
            }
            Box(
                Modifier.align(Alignment.CenterEnd)
                    .padding(end = 22.dp)
                    .size(132.dp, 96.dp)
                    .background(Color(0xFF10131A), RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(58.dp).background(Color(0xFF5B20C8), CircleShape), contentAlignment = Alignment.Center) {
                    Text("▶", color = Color.White, fontSize = 28.sp)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        HomeBtn("tv", "ТВ каналы", "Ваш M3U плейлист", tv)
        HomeBtn("movie", "Фильмы", "Internet Archive · открытые лицензии", movie)
    }
}

@Composable
private fun HomeBtn(kind: String, title: String, sub: String, on: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(146.dp)
            .background(CARD, RoundedCornerShape(20.dp))
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
        Column(Modifier.padding(start = 18.dp).weight(1f)) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(sub, color = MUTED, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = PURPLE, fontSize = 40.sp, modifier = Modifier.padding(end = 6.dp))
    }
    Spacer(Modifier.height(10.dp))
}'''
new_text, count = pattern_home.subn(new_home + '\n\n', text, count=1)
if count != 1:
    raise SystemExit(f'Could not replace Home/HomeBtn; count={count}')

source.write_text(new_text)
print('Applied runtime-safe home redesign without new image resources')
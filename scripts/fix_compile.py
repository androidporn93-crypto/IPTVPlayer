from pathlib import Path

source = Path('app/src/main/java/com/example/iptvplayer/MainActivity.kt')
text = source.read_text()

# FullscreenButton/HeartButton have a Dp parameter named `size`, which can
# shadow Canvas DrawScope.size. Always use the DrawScope receiver explicitly.
text = text.replace('size.width - 7f', 'this.size.width - 7f')
text = text.replace('size.width - 2f', 'this.size.width - 2f')
text = text.replace('size.height - 7f', 'this.size.height - 7f')
text = text.replace('size.height - 2f', 'this.size.height - 2f')
text = text.replace('val w = size.width; val h = size.height', 'val w = this.size.width; val h = this.size.height')

# Invalid Box overload: the second positional argument was a Modifier,
# but Compose expects Alignment there.
text = text.replace('Box(Modifier.align(Alignment.Center), Modifier) {', 'Box(Modifier.align(Alignment.Center)) {')

# Remove the bottom navigation bar completely and make the top area/home
# closer to the supplied reference design.
old_scaffold = '''        Scaffold(containerColor = BG, bottomBar = { BottomBar(page) { page = it; query = "" } }) { padding ->
            Column(Modifier.fillMaxSize().background(BG).padding(padding)) {
                Text("IPTV Player", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(18.dp))
                Box(Modifier.weight(1f)) {'''
new_scaffold = '''        Scaffold(containerColor = BG) { padding ->
            Column(Modifier.fillMaxSize().background(BG).padding(padding)) {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("IPTV", color = PURPLE, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                    Text(" Player", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
                Box(Modifier.weight(1f)) {'''
text = text.replace(old_scaffold, new_scaffold, 1)

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

@Composable
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
}'''
new_home = '''@Composable
private fun Home(tv: () -> Unit, movie: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Box(
            Modifier.fillMaxWidth().height(188.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF6D28D9), Color(0xFF172B8A))), RoundedCornerShape(24.dp))
                .padding(20.dp)
        ) {
            Column(Modifier.align(Alignment.CenterStart)) {
                Text("TV", color = Color.White.copy(.28f), fontSize = 60.sp, fontWeight = FontWeight.Black)
                Text("Смотрите", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("любимые каналы", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("и доступное кино", color = Color.White.copy(.82f), fontSize = 16.sp)
            }
            Box(
                Modifier.align(Alignment.CenterEnd).size(138.dp).background(Color.Black.copy(.16f), RoundedCornerShape(28.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("▶", color = Color.White, fontSize = 54.sp, fontWeight = FontWeight.Black)
            }
        }
        Spacer(Modifier.height(18.dp))
        HomeBtn("tv", "ТВ каналы", "Ваш M3U плейлист", tv)
        HomeBtn("movie", "Фильмы", "Internet Archive · открытые лицензии", movie)
    }
}

@Composable
private fun HomeBtn(kind: String, title: String, sub: String, on: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().height(150.dp).padding(vertical = 5.dp)
            .background(CARD, RoundedCornerShape(20.dp)).clickable(onClick = on)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(if (kind == "movie") R.drawable.movies_photo else R.drawable.iptv_channels_photo),
            contentDescription = title,
            modifier = Modifier.width(148.dp).fillMaxHeight().clip(RoundedCornerShape(16.dp))
        )
        Column(Modifier.padding(start = 18.dp).weight(1f)) {
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(sub, color = MUTED, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Text("›", color = PURPLE, fontSize = 40.sp)
    }
}'''
if old_home in text:
    text = text.replace(old_home, new_home, 1)

source.write_text(text)
print('Fixed compile issues and refreshed home layout without bottom navigation')

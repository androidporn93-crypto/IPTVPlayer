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

source.write_text(text)
print('Fixed Compose compile issues in MainActivity')

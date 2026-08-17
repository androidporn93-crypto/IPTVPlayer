from pathlib import Path

source = Path('app/src/main/java/com/example/iptvplayer/MainActivity.kt')
text = source.read_text()

# FullscreenButton has a Dp parameter named `size`, which shadows Canvas DrawScope.size.
# Qualify the DrawScope size so Kotlin resolves width/height correctly.
text = text.replace('size.width - 7f', 'this.size.width - 7f')
text = text.replace('size.width - 2f', 'this.size.width - 2f')
text = text.replace('size.height - 7f', 'this.size.height - 7f')
text = text.replace('size.height - 2f', 'this.size.height - 2f')

source.write_text(text)
print('Fixed FullscreenButton Canvas size shadowing')

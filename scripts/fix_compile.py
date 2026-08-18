from pathlib import Path

source = Path('app/src/main/java/com/example/iptvplayer/MainActivity.kt')
text = source.read_text()

# This script is intentionally limited to mechanical Compose compile fixes.
# The home-screen redesign is already applied by finalize_ui.py. Do not try to
# replace the old scaffold here, because that makes the workflow fail when
# finalize_ui.py has already produced the new scaffold.
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

if 'import androidx.compose.ui.layout.ContentScale' not in text and 'painterResource(R.drawable.iptv_channels_photo)' in text:
    text = text.replace(
        'import androidx.compose.ui.input.pointer.pointerInput\n',
        'import androidx.compose.ui.input.pointer.pointerInput\nimport androidx.compose.ui.layout.ContentScale\n'
    )

source.write_text(text)
print('Compile fixes applied; final UI scaffold left untouched')
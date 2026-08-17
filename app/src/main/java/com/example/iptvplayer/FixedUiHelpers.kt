package com.example.iptvplayer

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
private fun FixedHeart(favorite: Boolean, size: Dp, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.size(size).background(Color.Black.copy(.58f), CircleShape).clickable(onClick = onClick), Alignment.Center) {
        Canvas(Modifier.size(size * .48f)) {
            val w = this.size.width; val h = this.size.height
            val heart = Path().apply { moveTo(w*.5f,h*.88f); cubicTo(w*.08f,h*.58f,w*.12f,h*.15f,w*.38f,h*.22f); cubicTo(w*.47f,h*.24f,w*.5f,h*.34f,w*.5f,h*.4f); cubicTo(w*.5f,h*.34f,w*.53f,h*.24f,w*.62f,h*.22f); cubicTo(w*.88f,h*.15f,w*.92f,h*.58f,w*.5f,h*.88f) }
            drawPath(heart, if (favorite) Color(0xFFA855F7) else Color.White, style = Stroke(width = 3f))
        }
    }
}

package com.spcode.tnpscprep.ui.diary

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.domain.PhotoFrame
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

val HeartShape: Shape = GenericShape { s, _ ->
    val w = s.width; val h = s.height
    moveTo(0.5f * w, 0.92f * h)
    cubicTo(0.05f * w, 0.58f * h, -0.02f * w, 0.18f * h, 0.25f * w, 0.09f * h)
    cubicTo(0.40f * w, 0.04f * h, 0.50f * w, 0.14f * h, 0.50f * w, 0.26f * h)
    cubicTo(0.50f * w, 0.14f * h, 0.60f * w, 0.04f * h, 0.75f * w, 0.09f * h)
    cubicTo(1.02f * w, 0.18f * h, 0.95f * w, 0.58f * h, 0.5f * w, 0.92f * h)
    close()
}

val StarShape: Shape = GenericShape { s, _ ->
    val cx = s.width / 2f; val cy = s.height * 0.53f
    val outer = minOf(s.width, s.height) * 0.50f; val inner = outer * 0.42f
    for (k in 0 until 10) {
        val r = if (k % 2 == 0) outer else inner
        val a = -PI / 2 + k * PI / 5
        val x = cx + (r * cos(a)).toFloat(); val y = cy + (r * sin(a)).toFloat()
        if (k == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

fun frameShape(frame: String?, cornerDp: Int): Shape = when (PhotoFrame.of(frame)) {
    PhotoFrame.ROUNDED -> RoundedCornerShape(cornerDp.dp)
    PhotoFrame.CIRCLE -> CircleShape
    PhotoFrame.HEART -> HeartShape
    PhotoFrame.STAR -> StarShape
    PhotoFrame.ORIGINAL -> RectangleShape
}

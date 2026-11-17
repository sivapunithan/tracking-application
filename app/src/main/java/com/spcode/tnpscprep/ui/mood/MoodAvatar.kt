package com.spcode.tnpscprep.ui.mood

import android.content.Context
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.domain.Mood
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin

private const val TAU = (2 * PI).toFloat()

/** Where things are in each picture (fractions of the visible square), so effects land on the real face. */
private class Pose(
    val face: Offset, val faceRadius: Float,
    val eyes: List<Offset>, val streamLength: Float,
    val steam: List<Offset>, val sparkle: Offset,
)

private val DefaultPose = Pose(
    face = Offset(0.53f, 0.43f), faceRadius = 0.40f,
    eyes = listOf(Offset(0.41f, 0.43f), Offset(0.64f, 0.44f)), streamLength = 0.22f,
    steam = listOf(Offset(0.13f, 0.30f), Offset(0.87f, 0.30f)), sparkle = Offset(0.86f, 0.16f),
)

private fun poseFor(mood: Mood, ownPicture: Boolean): Pose = if (!ownPicture) DefaultPose else when (mood) {
    // closed eyes at the cheeks-with-fists pose: short streams, the drops fall outside the fists
    Mood.CRY -> Pose(Offset(0.52f, 0.58f), 0.42f, listOf(Offset(0.46f, 0.51f), Offset(0.68f, 0.56f)), 0.13f, DefaultPose.steam, DefaultPose.sparkle)
    // big close-up face, head thrust forward: flush centred low, steam from the top corners of the hair
    Mood.ANGRY -> Pose(Offset(0.53f, 0.53f), 0.46f, DefaultPose.eyes, 0.22f, listOf(Offset(0.16f, 0.20f), Offset(0.90f, 0.20f)), DefaultPose.sparkle)
    // arms up: put the sparkle in the free space between the raised fists
    Mood.CELEBRATE -> Pose(DefaultPose.face, DefaultPose.faceRadius, DefaultPose.eyes, 0.22f, DefaultPose.steam, Offset(0.5f, 0.12f))
    else -> DefaultPose
}

/** The picture for [mood] if it exists; otherwise 0. Pictures are optional (they are not part of the public repo). */
fun moodImageRes(context: Context, mood: Mood): Int =
    context.resources.getIdentifier(mood.imageName, "drawable", context.packageName)

fun hasMoodImage(context: Context, mood: Mood): Boolean = moodImageRes(context, mood) != 0

/**
 * The reaction avatar: a transparent picture plus effects drawn in code (so colours are right and they animate):
 *  - ANGRY  : the face flushes RED (pulsing), grey steam puffs rise from both sides, a quick shake
 *  - CRY    : BLUE tears stream down from the eyes and a BLUE water puddle ripples at the bottom, a sob wobble
 *  - LAUGH / CHEER / CELEBRATE : bounce + confetti (celebrate: more confetti, higher jump, sparkle)
 *  - PROUD : gentle breathing + sparkle;  WORRIED : a sweat drop
 * [previewWithFallback] lets the preview screen show an effect over the happy face when that mood's own picture isn't made yet.
 */
@Composable
fun MoodAvatar(mood: Mood, modifier: Modifier = Modifier, size: Dp = 96.dp, previewWithFallback: Boolean = false) {
    val ctx = LocalContext.current
    val own = remember(mood) { moodImageRes(ctx, mood) }
    val res = if (own != 0) own else if (previewWithFallback) moodImageRes(ctx, Mood.PROUD) else 0
    if (res == 0) return

    val pose = remember(mood, own) { poseFor(mood, own != 0) }
    val clock = rememberInfiniteTransition(label = "mood")
    val t by clock.animateFloat(0f, 1f, infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart), label = "t")

    Box(modifier.size(size)) {
        val s = with(androidx.compose.ui.platform.LocalDensity.current) { size.toPx() }
        Image(
            painterResource(res), contentDescription = null, contentScale = ContentScale.Crop, alignment = androidx.compose.ui.Alignment.TopCenter,
            modifier = Modifier
                .size(size)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen // so the red flush only paints where the picture is opaque
                    when (mood) {
                        Mood.ANGRY -> translationX = sin(t * TAU * 10) * 0.012f * s
                        Mood.CRY -> translationY = sin(t * TAU * 6) * 0.008f * s
                        Mood.LAUGH -> translationY = -abs(sin(t * TAU * 3)) * 0.03f * s
                        Mood.CHEER -> translationY = -abs(sin(t * TAU * 2)) * 0.05f * s
                        Mood.CELEBRATE -> translationY = -abs(sin(t * TAU * 2.5f)) * 0.07f * s
                        Mood.PROUD, Mood.WORRIED -> { val b = 1f + 0.015f * sin(t * TAU); scaleX = b; scaleY = b }
                    }
                }
                .drawWithContent {
                    drawContent()
                    if (mood == Mood.ANGRY) {
                        val a = 0.46f + 0.20f * (1f + sin(t * TAU * 3)) / 2f
                        // A wide, even flush over the whole face (a plateau that fades out at the edges), not a single spot.
                        val c = Offset(pose.face.x * this.size.width, pose.face.y * this.size.height)
                        drawRect(
                            Brush.radialGradient(
                                0f to Color(0xFFFF1F1F).copy(alpha = (a * 1.1f).coerceAtMost(0.85f)),
                                0.6f to Color(0xFFFF1F1F).copy(alpha = (a * 1.0f).coerceAtMost(0.8f)),
                                0.88f to Color(0xFFFF1F1F).copy(alpha = a * 0.45f),
                                1f to Color.Transparent,
                                center = c, radius = pose.faceRadius * this.size.width,
                            ),
                            blendMode = BlendMode.SrcAtop,
                        )
                    }
                },
        )
        Canvas(Modifier.size(size)) {
            when (mood) {
                Mood.LAUGH -> confetti(t, count = 9)
                Mood.CHEER -> confetti(t, count = 16)
                Mood.CELEBRATE -> { confetti(t, count = 30); sparkle(t, pose) }
                Mood.PROUD -> sparkle(t, pose)
                Mood.WORRIED -> sweat(t)
                Mood.ANGRY -> steam(t, pose)
                Mood.CRY -> { tears(t, pose); puddle(t) }
            }
        }
    }
}

// ------------------------------------------------------------------ effects (all transparent, drawn over the picture)

private val Blue = Color(0xFF38BDF8)
private val DeepBlue = Color(0xFF0284C7)

private fun DrawScope.steam(t: Float, pose: Pose) {
    val s = size.minDimension
    val sources = pose.steam
    sources.forEachIndexed { side, p ->
        for (k in 0..2) {
            val ph = (t * 1.5f + k / 3f + side * 0.3f) % 1f
            val dir = if (side == 0) -1f else 1f
            val c = Offset((p.x + dir * ph * 0.07f) * s, (p.y - ph * 0.26f) * s)
            val r = (0.035f + 0.05f * ph) * s
            val a = (1f - ph) * 0.8f
            drawCircle(Color(0xFFCBD5E1).copy(alpha = a), r, c)
            drawCircle(Color(0xFF64748B).copy(alpha = a * 0.8f), r, c, style = Stroke(width = 0.010f * s))
        }
    }
}

private fun DrawScope.tears(t: Float, pose: Pose) {
    val s = size.minDimension
    val eyes = pose.eyes
    eyes.forEachIndexed { idx, e ->
        // a continuous stream along the cheek
        drawLine(
            Brush.verticalGradient(listOf(Blue.copy(alpha = 0.9f), Blue.copy(alpha = 0.0f)), startY = e.y * s, endY = (e.y + pose.streamLength) * s),
            Offset(e.x * s, e.y * s), Offset((e.x - 0.01f + idx * 0.02f) * s, (e.y + pose.streamLength) * s),
            strokeWidth = 0.030f * s, cap = StrokeCap.Round,
        )
        // drops that fall off the chin and keep going
        for (k in 0..2) {
            val ph = (t * 2f + k / 3f + idx * 0.17f) % 1f
            val c = Offset((e.x + sin((ph + idx) * TAU) * 0.01f) * s, (e.y + 0.05f + ph * 0.42f) * s)
            val r = 0.023f * s * (1f + ph * 0.5f)
            drawCircle(Blue.copy(alpha = 1f - ph), r, c)
            drawCircle(Color.White.copy(alpha = (1f - ph) * 0.8f), r * 0.35f, Offset(c.x - r * 0.3f, c.y - r * 0.3f))
        }
    }
}

private fun DrawScope.puddle(t: Float) {
    val s = size.minDimension
    val base = 1f - (0.055f + 0.02f * (1f + sin(t * TAU * 2)) / 2f)
    // Rounded, rippling puddle (soft ends, not a hard rectangle)
    val path = Path().apply {
        moveTo(0.05f * s, s)
        lineTo(0.05f * s, (base + 0.03f) * s)
        quadraticBezierTo(0.05f * s, base * s, 0.11f * s, base * s)
        var x = 0.11f
        while (x <= 0.89f) { lineTo(x * s, (base + sin(x * TAU * 3 + t * TAU * 2) * 0.012f) * s); x += 0.02f }
        quadraticBezierTo(0.95f * s, base * s, 0.95f * s, (base + 0.03f) * s)
        lineTo(0.95f * s, s)
        close()
    }
    drawPath(path, Brush.verticalGradient(listOf(Blue.copy(alpha = 0.65f), DeepBlue.copy(alpha = 0.8f)), startY = base * s, endY = s))
}

private val ConfettiColors = listOf(Color(0xFFF5B301), Color(0xFFC8102E), Color(0xFF800020), Color(0xFFFB923C), Color(0xFFF472B6))

private fun DrawScope.confetti(t: Float, count: Int) {
    val s = size.minDimension
    for (i in 0 until count) {
        val x = ((i * 0.618034f) % 1f) * s
        val ph = (t * 1.2f + i * 0.137f) % 1f
        val y = (-0.05f + ph * 1.05f) * s
        rotate(ph * 360f * 2f, pivot = Offset(x, y)) {
            drawRect(ConfettiColors[i % ConfettiColors.size], Offset(x, y), androidx.compose.ui.geometry.Size(0.022f * s, 0.036f * s))
        }
    }
}

private fun DrawScope.sparkle(t: Float, pose: Pose) {
    val s = size.minDimension
    val k = 0.6f + 0.5f * (1f + sin(t * TAU * 2)) / 2f
    val c = Offset(pose.sparkle.x * s, pose.sparkle.y * s)
    val r = 0.07f * s * k
    val star = Path().apply {
        moveTo(c.x, c.y - r); quadraticBezierTo(c.x, c.y, c.x + r, c.y)
        quadraticBezierTo(c.x, c.y, c.x, c.y + r); quadraticBezierTo(c.x, c.y, c.x - r, c.y)
        quadraticBezierTo(c.x, c.y, c.x, c.y - r); close()
    }
    drawPath(star, Color(0xFFFBBF24))
}

private fun DrawScope.sweat(t: Float) {
    val s = size.minDimension
    val ph = (t * 1.3f) % 1f
    val c = Offset(0.74f * s, (0.20f + ph * 0.26f) * s)
    drawCircle(Blue.copy(alpha = 1f - ph * 0.6f), 0.022f * s, c)
    drawCircle(Color.White.copy(alpha = 0.7f), 0.008f * s, Offset(c.x - 0.007f * s, c.y - 0.007f * s))
}

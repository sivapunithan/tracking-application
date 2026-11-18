package com.spcode.tnpscprep.ui.guide

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.ui.diary.DiaryMood
import kotlin.math.roundToInt

/** One tip: [target] = the key of the element to light up (null or unknown = just the card in the middle). */
data class CoachStep(val target: String?, val title: String, val body: String)

/**
 * A first-time tour: the screen is dimmed with a rounded "spotlight" around the element being explained and a card beside it
 * (title, one or two sentences, step dots, Skip / Next). It swallows every touch while it is up (so nothing underneath, not even the tab swipe,
 * can be triggered), and it ends only on Skip or the last Done. [bounds] are in WINDOW coordinates (use boundsInWindow).
 */
@Composable
fun CoachOverlay(steps: List<CoachStep>, bounds: Map<String, Rect>, onFinish: () -> Unit) {
    if (steps.isEmpty()) return
    var index by remember { mutableIntStateOf(0) }
    val step = steps[index.coerceIn(0, steps.lastIndex)]
    var origin by remember { mutableStateOf(Offset.Zero) }
    var height by remember { mutableIntStateOf(0) }
    val d = LocalDensity.current

    // the spotlight glides from one element to the next
    val target = step.target?.let { bounds[it] }?.translate(-origin.x, -origin.y)
    val l = remember { Animatable(0f) }; val t = remember { Animatable(0f) }; val r = remember { Animatable(0f) }; val b = remember { Animatable(0f) }
    val a = remember { Animatable(0f) }
    LaunchedEffect(target, index) {
        if (target == null) { a.animateTo(0f, tween(200)); return@LaunchedEffect }
        if (a.value == 0f) { l.snapTo(target.left); t.snapTo(target.top); r.snapTo(target.right); b.snapTo(target.bottom); a.animateTo(1f, tween(300)) }
        else {
            val spec = tween<Float>(320)
            kotlinx.coroutines.coroutineScope {
                launch { l.animateTo(target.left, spec) }; launch { t.animateTo(target.top, spec) }
                launch { r.animateTo(target.right, spec) }; launch { b.animateTo(target.bottom, spec) }
            }
        }
    }
    val fade = remember { Animatable(0f) }
    LaunchedEffect(Unit) { fade.animateTo(1f, tween(250)) }
    val ring = MaterialTheme.colorScheme.primary

    Box(
        Modifier.fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow(); height = it.size.height }
            .graphicsLayer { alpha = fade.value }
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent().changes.forEach { it.consume() } } },
    ) {
        Canvas(Modifier.fillMaxSize().graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }) {
            drawRect(Color.Black.copy(alpha = 0.74f))
            if (a.value > 0.01f) {
                val pad = 8.dp.toPx()
                val tl = Offset(l.value - pad, t.value - pad); val sz = Size(r.value - l.value + 2 * pad, b.value - t.value + 2 * pad)
                val cr = CornerRadius(22.dp.toPx())
                drawRoundRect(Color.Black, tl, sz, cr, alpha = a.value, blendMode = BlendMode.Clear)
                drawRoundRect(ring.copy(alpha = a.value), tl, sz, cr, style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
            }
        }
        // the card goes below the element when it is in the upper half, above it otherwise
        val cardH = with(d) { 210.dp.toPx() }
        val gap = with(d) { 18.dp.toPx() }
        val y = when {
            target == null -> (height - cardH) / 2f
            (target.top + target.bottom) / 2f < height / 2f -> (target.bottom + gap + with(d) { 8.dp.toPx() }).coerceAtMost(height - cardH - gap)
            else -> (target.top - gap - cardH - with(d) { 8.dp.toPx() }).coerceAtLeast(gap)
        }
        Surface(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp).offset { IntOffset(0, y.roundToInt()) },
            shape = RoundedCornerShape(26.dp), color = MaterialTheme.colorScheme.surface, tonalElevation = 6.dp, shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(horizontal = 20.dp, vertical = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(46.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) { DiaryMood("PROUD", 42.dp) }
                    Text(step.title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), modifier = Modifier.weight(1f))
                }
                Text(step.body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        steps.indices.forEach { i ->
                            Box(Modifier.size(if (i == index) 9.dp else 7.dp).clip(CircleShape).background(if (i == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant))
                        }
                    }
                    if (index < steps.lastIndex) TextButton(onClick = onFinish) { Text(stringResource(R.string.guide_skip)) }
                    Button(onClick = { if (index < steps.lastIndex) index++ else onFinish() }) { Text(stringResource(if (index < steps.lastIndex) R.string.guide_next else R.string.guide_done)) }
                }
            }
        }
    }
}

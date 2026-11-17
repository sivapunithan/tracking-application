package com.spcode.tnpscprep.ui.design

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** Space the floating bottom bar occupies, so lists can scroll their last item clear of it. */
val LocalBottomBarInset = compositionLocalOf { 0.dp }

/** Big friendly heading used at the top of every screen (there are no top bars). */
@Composable
fun ScreenTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

/** Progress bar whose fill animates smoothly to every new value. */
@Composable
fun AnimatedProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp,
    track: Color = MaterialTheme.colorScheme.outlineVariant,
    fill: Brush = Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)),
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing), label = "bar")
    Box(modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(track)) {
        Box(Modifier.fillMaxWidth(p).height(height).clip(RoundedCornerShape(height / 2)).background(fill))
    }
}

/** Circular progress whose arc sweeps to each new value. */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 10.dp,
    track: Color = Color.White.copy(alpha = 0.25f),
    color: Color = Color.White,
    content: @Composable () -> Unit = {},
) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing), label = "ring")
    Box(modifier, contentAlignment = androidx.compose.ui.Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            val inset = strokeWidth.toPx() / 2
            val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
            drawArc(track, 0f, 360f, false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
            drawArc(color, -90f, 360f * p, false, topLeft = Offset(inset, inset), size = arcSize, style = stroke)
        }
        content()
    }
}

/** A number that counts up/down to its new value instead of jumping. */
@Composable
fun animatedInt(value: Int): Int {
    val v by animateIntAsState(value, tween(800, easing = FastOutSlowInEasing), label = "count")
    return v
}

/**
 * The app's pop-up: a bottom sheet that springs up from the bottom, with a drag handle and soft rounded top.
 * [content] receives `dismiss`, which animates the sheet away first and only then reports onDismiss.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.(dismiss: () -> Unit) -> Unit) {
    val state = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val dismiss: () -> Unit = { scope.launch { state.hide() }.invokeOnCompletion { onDismiss() } }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = state,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        scrimColor = Color.Black.copy(alpha = 0.35f),
        dragHandle = { BottomSheetDefaults.DragHandle() },
        contentWindowInsets = { WindowInsets.navigationBars },
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(start = 24.dp, end = 24.dp, bottom = 28.dp),
        ) { content(dismiss) }
    }
}

/** A white (surface) rounded card with the squish-on-press feel when it is clickable. */
@Composable
fun SoftCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = if (onClick != null) modifier.pressScale(onClick = onClick) else modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
    ) { Column(content = content) }
}

/** Five connected dots (Not started ... Revision 3). The node for the current level springs bigger when the level changes. */
@Composable
fun LevelTrack(level: Int, modifier: Modifier = Modifier, max: Int = 4) {
    androidx.compose.foundation.layout.Row(modifier, verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        for (i in 0..max) {
            val filled = i <= level
            val scale by animateFloatAsState(
                if (i == level) 1.35f else 1f,
                androidx.compose.animation.core.spring(dampingRatio = 0.4f, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium),
                label = "node$i",
            )
            val color by androidx.compose.animation.animateColorAsState(
                if (filled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "nodeColor$i",
            )
            Box(
                Modifier.height(14.dp).androidx_size(14.dp)
                    .graphicsLayerScale(scale).clip(androidx.compose.foundation.shape.CircleShape).background(color),
            )
            if (i < max) {
                val lineColor by androidx.compose.animation.animateColorAsState(
                    if (i < level) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "line$i",
                )
                Box(Modifier.weight(1f).height(3.dp).background(lineColor))
            }
        }
    }
}

private fun Modifier.androidx_size(d: Dp): Modifier = this.then(Modifier.height(d).then(Modifier.width(d)))
private fun Modifier.graphicsLayerScale(s: Float): Modifier = this.graphicsLayer { scaleX = s; scaleY = s }

package com.spcode.tnpscprep.ui.design

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.delay

/** True when the user turned system animations off ("Remove animations" / animator scale 0): we then skip fancy motion. */
@Composable
fun rememberReduceMotion(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return remember {
        runCatching { Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }.getOrDefault(false)
    }
}

/**
 * Click with a tactile "squish": the element shrinks a little under the finger and springs back.
 * Cheap (a graphicsLayer scale), so it is fine on a modest phone.
 */
fun Modifier.pressScale(enabled: Boolean = true, pressedScale: Float = 0.96f, onClick: () -> Unit): Modifier = composed {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessMedium),
        label = "pressScale",
    )
    this
        .graphicsLayer { scaleX = scale; scaleY = scale }
        .clickable(interactionSource = source, indication = null, enabled = enabled, onClick = onClick)
}

/**
 * Staggered entrance for the first few items of a list: a quick fade + rise, each a little later than the last.
 * [index] >= [limit] items are shown immediately (long lists must not animate while scrolling).
 */
@Composable
fun PopIn(index: Int, modifier: Modifier = Modifier, limit: Int = 8, content: @Composable () -> Unit) {
    val reduce = rememberReduceMotion()
    var shown by remember { mutableStateOf(reduce || index >= limit) }
    LaunchedEffect(Unit) {
        if (!shown) { delay(index.coerceAtLeast(0) * 40L); shown = true }
    }
    AnimatedVisibility(
        visible = shown,
        modifier = modifier,
        enter = fadeIn(tween(260)) + slideInVertically(spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)) { it / 5 },
    ) { Box { content() } }
}

package com.spcode.tnpscprep.ui.auth

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.ui.design.pressScale
import com.spcode.tnpscprep.ui.theme.LocalIsDark
import kotlinx.coroutines.delay

/** A drawable by file name if the owner has supplied it (pictures are not in the public repo), else 0. */
fun drawableId(context: Context, name: String): Int = context.resources.getIdentifier(name, "drawable", context.packageName)

/**
 * The crimson-to-maroon header with the hero picture. Pictures are looked up in order ([pictures]); if none exists
 * a book badge is shown, so the screen is complete even before the artwork arrives.
 */
@Composable
fun AuthHero(pictures: List<String>, modifier: Modifier = Modifier, height: androidx.compose.ui.unit.Dp = 300.dp) {
    val ctx = LocalContext.current
    val id = remember(pictures) { pictures.firstNotNullOfOrNull { n -> drawableId(ctx, n).takeIf { it != 0 } } ?: 0 }
    val dark = LocalIsDark.current
    val brush = Brush.verticalGradient(if (dark) listOf(Color(0xFF9B0F26), Color(0xFF4A0A14)) else listOf(Color(0xFFC8102E), Color(0xFF7A0019)))
    val scale by animateFloatAsState(1f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow), label = "heroPop")
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(100); shown = true }
    val pop by animateFloatAsState(if (shown) scale else 0.6f, spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow), label = "heroIn")
    Box(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(bottomStart = 48.dp, bottomEnd = 48.dp)).background(brush),
        contentAlignment = Alignment.BottomCenter,
    ) {
        // The brand tile (crimson rounded square + gold symbol; a thin light ring keeps it readable on the crimson header), top-left, below the status bar.
        val logoId = remember { drawableId(ctx, "app_logo") }
        if (logoId != 0) {
            Box(
                Modifier.align(Alignment.TopStart).statusBarsPadding().padding(16.dp).size(54.dp)
                    .clip(RoundedCornerShape(16.dp)).border(1.5.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
            ) { Image(painterResource(logoId), null, modifier = Modifier.fillMaxSize()) }
        }
        if (id != 0) {
            Image(
                painterResource(id), null, contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().height(height - 24.dp).padding(horizontal = 24.dp).scale(pop),
                alignment = Alignment.BottomCenter,
            )
        } else {
            Box(Modifier.fillMaxWidth().height(height), contentAlignment = Alignment.Center) {
                Box(Modifier.size(120.dp).scale(pop).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.School, null, tint = Color.White, modifier = Modifier.size(64.dp))
                }
            }
        }
    }
}

/** Four dots that fill as digits are typed; the row shakes when [errorTick] changes (wrong / mismatched PIN). */
@Composable
fun PinDots(filled: Int, errorTick: Int, modifier: Modifier = Modifier, count: Int = com.spcode.tnpscprep.domain.PinPolicy.LENGTH) {
    val shake = remember { Animatable(0f) }
    LaunchedEffect(errorTick) {
        if (errorTick > 0) {
            repeat(3) { shake.animateTo(14f, tween(45)); shake.animateTo(-14f, tween(45)) }
            shake.animateTo(0f, tween(45))
        }
    }
    Row(modifier.offset(x = shake.value.dp), horizontalArrangement = Arrangement.spacedBy(18.dp)) {
        repeat(count) { i ->
            val on = i < filled
            val s by animateFloatAsState(if (on) 1.15f else 1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium), label = "dot$i")
            val c by animateColorAsState(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant, label = "dotColor$i")
            Box(Modifier.size(18.dp).scale(s).clip(CircleShape).background(c))
        }
    }
}

/**
 * A PIN prompt: text, dots and a numeric keypad. Calls [onComplete] when the 4th digit is typed, then clears itself.
 * [busy] ignores keys while a (slow, deliberately) PIN check runs.
 */
@Composable
fun PinInput(prompt: String, message: String?, errorTick: Int, onComplete: (String) -> Unit, modifier: Modifier = Modifier, busy: Boolean = false, enabled: Boolean = true) {
    var digits by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(digits) {
        if (digits.length == com.spcode.tnpscprep.domain.PinPolicy.LENGTH) { delay(140); onComplete(digits); digits = "" }
    }
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(prompt, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
        Box(Modifier.height(20.dp))
        PinDots(digits.length, errorTick)
        Box(Modifier.height(10.dp))
        Text(
            message.orEmpty(), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error, modifier = Modifier.height(24.dp),
        )
        Box(Modifier.height(8.dp))
        val rows = listOf(listOf('1', '2', '3'), listOf('4', '5', '6'), listOf('7', '8', '9'), listOf(' ', '0', '<'))
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.padding(vertical = 6.dp)) {
                row.forEach { k ->
                    when (k) {
                        ' ' -> Box(Modifier.size(68.dp))
                        '<' -> Key(onClick = { if (enabled && !busy && digits.isNotEmpty()) digits = digits.dropLast(1) }) {
                            Icon(Icons.AutoMirrored.Rounded.Backspace, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        else -> Key(onClick = {
                            if (enabled && !busy && digits.length < com.spcode.tnpscprep.domain.PinPolicy.LENGTH) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); digits += k
                            }
                        }) { Text(k.toString(), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Medium)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun Key(onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        Modifier.size(68.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceContainer).pressScale(pressedScale = 0.9f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { content() }
}

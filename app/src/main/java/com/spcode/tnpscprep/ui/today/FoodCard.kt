package com.spcode.tnpscprep.ui.today

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.data.FoodRepository
import com.spcode.tnpscprep.data.SettingsStore
import com.spcode.tnpscprep.domain.FoodCheckin
import com.spcode.tnpscprep.domain.FoodState
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class FoodViewModel(private val repo: FoodRepository, private val settings: SettingsStore) : ViewModel() {
    val days: StateFlow<Map<LocalDate, FoodState>> =
        repo.observe(LocalDate.now().minusDays(130)).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    val strict: StateFlow<Boolean> = settings.strictFood
    val lastAlertStart: StateFlow<String> = settings.lastFoodAlertStart

    fun set(day: LocalDate, state: FoodState?) { viewModelScope.launch { repo.set(day, state) } }
    fun markAlerted(start: LocalDate) = settings.setLastFoodAlertStart(start.toString())
}

// ---------------------------------------------------------------- emoji + labels

/**
 * Emoji lookup order: her own food picture (drawable-nodpi/food_<name>.webp) -> the matching mood avatar -> the marked placeholder vector.
 * Mood stand-ins: healthy = cheer, mixed = proud, junk = worried (the mildest one: no guilt), angry = angry.
 * The tall mood pictures are shown as their top square (the face).
 */
@Composable
fun FoodEmoji(name: String, size: Dp, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val id = remember(name) {
        val own = ctx.resources.getIdentifier("food_$name", "drawable", ctx.packageName)
        val mood = when (name) {
            "healthy" -> com.spcode.tnpscprep.domain.Mood.CHEER
            "mixed" -> com.spcode.tnpscprep.domain.Mood.PROUD
            "junk" -> com.spcode.tnpscprep.domain.Mood.WORRIED
            else -> com.spcode.tnpscprep.domain.Mood.ANGRY
        }
        when {
            own != 0 -> own
            com.spcode.tnpscprep.ui.mood.moodImageRes(ctx, mood) != 0 -> com.spcode.tnpscprep.ui.mood.moodImageRes(ctx, mood)
            else -> ctx.resources.getIdentifier("food_ph_$name", "drawable", ctx.packageName)
        }
    }
    Image(
        painterResource(id), contentDescription = null, modifier = modifier.size(size),
        contentScale = androidx.compose.ui.layout.ContentScale.Crop, alignment = Alignment.TopCenter,
    )
}

private fun emojiName(s: FoodState) = s.name.lowercase()

@Composable
private fun label(s: FoodState) = stringResource(
    when (s) { FoodState.HEALTHY -> R.string.food_healthy; FoodState.MIXED -> R.string.food_mixed; FoodState.JUNK -> R.string.food_junk },
)

@Composable
private fun message(s: FoodState) = stringResource(
    when (s) { FoodState.HEALTHY -> R.string.food_msg_healthy; FoodState.MIXED -> R.string.food_msg_mixed; FoodState.JUNK -> R.string.food_msg_junk },
)

/** One accent: gold for healthy, a soft crimson for mixed, grey for junk. Never the only signal: every dot also has its emoji. */
@Composable
fun foodColor(s: FoodState?): Color {
    val c = MaterialTheme.colorScheme
    return when (s) {
        FoodState.HEALTHY -> c.tertiary
        FoodState.MIXED -> c.primary.copy(alpha = 0.55f)
        FoodState.JUNK -> c.onSurface.copy(alpha = 0.35f)
        null -> Color.Transparent
    }
}

// ---------------------------------------------------------------- the card

private enum class Mode { PICK, RESULT, CHIP }

@Composable
fun FoodSection(today: LocalDate, days: Map<LocalDate, FoodState>, evening: Boolean, onSet: (LocalDate, FoodState?) -> Unit, onDay: (String) -> Unit) {
    val state = days[today]
    var justLogged by remember(today) { mutableStateOf<Pair<FoodState, FoodState?>?>(null) }
    var editing by remember(today) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(justLogged) { if (justLogged != null) { kotlinx.coroutines.delay(5_000); justLogged = null } }

    val mode = when {
        state == null || editing -> Mode.PICK
        justLogged != null -> Mode.RESULT
        else -> Mode.CHIP
    }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        AnimatedContent(
            targetState = mode,
            transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
            label = "food-card",
        ) { m ->
            when (m) {
                Mode.PICK -> PickCard(evening && state == null) { s ->
                    justLogged = s to state
                    editing = false
                    onSet(today, s)
                    if (s == FoodState.HEALTHY) haptic.performHapticFeedback(HapticFeedbackType.Confirm)
                }
                Mode.RESULT -> {
                    val r = justLogged
                    if (r != null) ResultCard(r.first, onUndo = { justLogged = null; onSet(today, r.second) })
                }
                Mode.CHIP -> if (state != null) Chip(state, onChange = { editing = true })
            }
        }
        Strip(today, days, onDay)
        Progress(today, days)
    }
}

@Composable
private fun PickCard(evening: Boolean, onPick: (FoodState) -> Unit) {
    val c = MaterialTheme.colorScheme
    Surface(
        Modifier.fillMaxWidth().then(if (evening) Modifier.border(BorderStroke(2.dp, c.primary), MaterialTheme.shapes.large) else Modifier),
        shape = MaterialTheme.shapes.large, color = if (evening) c.primaryContainer else c.surface, tonalElevation = 1.dp, shadowElevation = 2.dp,
    ) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(stringResource(R.string.food_question), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            if (evening) Text(stringResource(R.string.food_evening), style = MaterialTheme.typography.bodySmall, color = c.onSurfaceVariant)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FoodState.entries.forEach { s ->
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(20.dp)).background(c.surfaceVariant.copy(alpha = 0.5f)).clickable { onPick(s) }.padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        FoodEmoji(emojiName(s), 56.dp)
                        Text(label(s), style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultCard(s: FoodState, onUndo: () -> Unit) {
    val scale = remember { Animatable(0.6f) }
    LaunchedEffect(Unit) { scale.animateTo(1f, spring(dampingRatio = if (s == FoodState.HEALTHY) 0.35f else 0.7f, stiffness = Spring.StiffnessMedium)) }
    Surface(Modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, shadowElevation = 2.dp) {
        Box {
            if (s == FoodState.HEALTHY) ConfettiBurst(Modifier.matchParentSizeCompat())
            Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                FoodEmoji(emojiName(s), 64.dp, Modifier.scale(scale.value))
                Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                    Text(label(s), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                    Text(message(s), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onUndo) { Text(stringResource(R.string.food_undo)) }
            }
        }
    }
}

private fun Modifier.matchParentSizeCompat(): Modifier = this.fillMaxWidth().height(110.dp)

@Composable
private fun Chip(s: FoodState, onChange: () -> Unit) {
    Surface(
        Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onChange), shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surface, tonalElevation = 1.dp, shadowElevation = 1.dp,
    ) {
        Row(Modifier.padding(horizontal = 14.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FoodEmoji(emojiName(s), 28.dp)
            Text(stringResource(R.string.food_today_chip, label(s)), style = MaterialTheme.typography.labelLarge)
            Text(stringResource(R.string.food_change), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

// ---------------------------------------------------------------- weekly strip + progress

@Composable
private fun Strip(today: LocalDate, days: Map<LocalDate, FoodState>, onDay: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        FoodCheckin.weekOf(today).forEach { d ->
            val s = days[d]
            val future = d.isAfter(today)
            val desc = d.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()) + ": " + (s?.let { label(it) } ?: stringResource(R.string.food_none))
            Column(
                Modifier.semantics { contentDescription = desc }.then(if (!future) Modifier.clickable { onDay(d.toString()) } else Modifier),
                horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Box(
                    Modifier.size(38.dp).clip(CircleShape)
                        .background(if (s != null) foodColor(s).copy(alpha = 0.25f) else Color.Transparent)
                        .border(BorderStroke(1.5.dp, if (s != null) foodColor(s) else MaterialTheme.colorScheme.outlineVariant), CircleShape),
                    contentAlignment = Alignment.Center,
                ) { if (s != null) FoodEmoji(emojiName(s), 24.dp) }
                Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun Progress(today: LocalDate, days: Map<LocalDate, FoodState>) {
    val n = FoodCheckin.healthyThisWeek(days, today)
    val streak = FoodCheckin.streak(days, today)
    Text(
        stringResource(R.string.food_week, n) + if (streak >= 2) "  ·  " + stringResource(R.string.food_streak, streak) else "",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

// ---------------------------------------------------------------- strict-mode sheet content

@Composable
fun FoodAlertSheet(dismiss: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        FoodEmoji("angry", 88.dp)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.food_alert), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold), textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Button(onClick = dismiss, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.food_dismiss)) }
    }
}

// ---------------------------------------------------------------- tiny confetti (built-in Canvas, no library)

@Composable
fun ConfettiBurst(modifier: Modifier, originX: Float = 0.17f) {
    val t = remember { Animatable(0f) }
    LaunchedEffect(Unit) { t.animateTo(1f, tween(800)) }
    val colors = listOf(Color(0xFFF5B301), Color(0xFFC8102E), Color(0xFF800020), Color(0xFFFB923C))
    Canvas(modifier) {
        if (t.value >= 1f) return@Canvas
        val origin = Offset(size.width * originX, size.height * 0.5f)
        for (i in 0 until 14) {
            val a = (i / 14f) * 6.2832f
            val dist = (40f + (i % 3) * 22f) * t.value * density
            val x = origin.x + cos(a) * dist
            val y = origin.y + sin(a) * dist + 30f * t.value * t.value * density
            drawCircle(colors[i % colors.size].copy(alpha = 1f - t.value), radius = 3.5f * density, center = Offset(x, y))
        }
    }
}

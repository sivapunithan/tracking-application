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

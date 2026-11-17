package com.spcode.tnpscprep.ui.today

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.data.MoodRepository
import com.spcode.tnpscprep.domain.FoodState
import com.spcode.tnpscprep.domain.Mood
import com.spcode.tnpscprep.ui.design.ProgressRing
import com.spcode.tnpscprep.ui.design.SoftCard
import com.spcode.tnpscprep.ui.design.animatedInt
import com.spcode.tnpscprep.ui.diary.DiaryMood
import com.spcode.tnpscprep.ui.mood.MoodAvatar
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** The daily mood check-ins (one per day, replaceable). Stored as MOOD_CHECKIN events, see data/MoodRepository. */
class MoodViewModel(private val repo: MoodRepository) : ViewModel() {
    val days: StateFlow<Map<LocalDate, Mood>> = repo.observe(LocalDate.now().minusDays(130)).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())
    fun set(day: LocalDate, mood: Mood?) { viewModelScope.launch { repo.set(day, mood) } }
}

/** Display name of a mood (English / Tamil), in the order of [Mood.entries]. */
@Composable
fun moodLabel(m: Mood): String = stringArrayResource(R.array.mood_labels)[m.ordinal]

/**
 * "How are you today?": three equal tiles, one tap each. Mood (the picture plays its own reaction), Food (its picture), Water (a ring that fills,
 * tap = one glass). Nothing is a score: an empty tile is just empty.
 */
@Composable
fun CheckinCard(
    mood: Mood?, food: FoodState?, foodPrompt: Boolean, waterMl: Int, waterTarget: Int,
    onMood: () -> Unit, onFood: () -> Unit, onWaterAdd: () -> Unit, onWaterUndo: (() -> Unit)?,
) {
    val haptic = LocalHapticFeedback.current
    SoftCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column {
                Text(stringResource(R.string.checkin_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                Text(stringResource(R.string.checkin_sub), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                // ---- mood ----
                Tile(Modifier.weight(1f), stringResource(R.string.checkin_mood), mood?.let { moodLabel(it) }, onMood) {
                    val pop by animateFloatAsState(1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium), label = "moodPop")
                    if (mood != null) Box(Modifier.size(64.dp).graphicsLayer { scaleX = pop; scaleY = pop }) { MoodAvatar(mood, size = 64.dp) } else EmptyDot()
                }
                // ---- food ----
                Tile(Modifier.weight(1f), stringResource(R.string.checkin_food), food?.let { stringResource(foodLabel(it)) }, onFood, highlight = foodPrompt && food == null) {
                    var first by remember { mutableStateOf(true) }
                    var burst by remember { mutableIntStateOf(0) }
                    LaunchedEffect(food) { if (first) first = false else if (food == FoodState.HEALTHY) { burst++; haptic.performHapticFeedback(HapticFeedbackType.Confirm) } }
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        if (food != null) {
                            Box(Modifier.size(64.dp).clip(CircleShape).background(foodColor(food).copy(alpha = 0.25f)).border(BorderStroke(2.dp, foodColor(food)), CircleShape), contentAlignment = Alignment.Center) {
                                FoodEmoji(food.name.lowercase(), 54.dp)
                            }
                        } else EmptyDot()
                        if (burst > 0) androidx.compose.runtime.key(burst) { ConfettiBurst(Modifier.size(96.dp), originX = 0.5f) }
                    }
                }
                // ---- water ----
                Tile(Modifier.weight(1f), stringResource(R.string.checkin_water), stringResource(R.string.water_of, waterTarget), onWaterAdd) {
                    Box(Modifier.size(64.dp), contentAlignment = Alignment.Center) {
                        ProgressRing(
                            (waterMl.toFloat() / waterTarget.coerceAtLeast(1)).coerceIn(0f, 1f), Modifier.size(64.dp), strokeWidth = 7.dp,
                            track = MaterialTheme.colorScheme.outlineVariant, color = MaterialTheme.colorScheme.primary,
                        ) { Text(animatedInt(waterMl).toString(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), maxLines = 1) }
                        if (onWaterUndo != null) {
                            val undo = stringResource(R.string.water_undo)
                            Box(
                                Modifier.align(Alignment.TopEnd).size(22.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surface).border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline), CircleShape)
                                    .semantics { contentDescription = undo }.clickable(onClick = onWaterUndo),
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Rounded.Remove, null, Modifier.size(14.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun foodLabel(s: FoodState): Int = when (s) { FoodState.HEALTHY -> R.string.food_healthy; FoodState.MIXED -> R.string.food_mixed; FoodState.JUNK -> R.string.food_junk }

@Composable
private fun EmptyDot() {
    Box(Modifier.size(64.dp).clip(CircleShape).border(BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline), CircleShape), contentAlignment = Alignment.Center) {
        Icon(Icons.Rounded.Add, null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(26.dp))
    }
}

@Composable
private fun Tile(modifier: Modifier, title: String, sub: String?, onClick: () -> Unit, highlight: Boolean = false, visual: @Composable () -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.28f))
            .then(if (highlight) Modifier.border(BorderStroke(2.dp, MaterialTheme.colorScheme.primary), RoundedCornerShape(20.dp)) else Modifier)
            .clickable(onClick = onClick).padding(vertical = 12.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        visual()
        Text(title, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center, maxLines = 1)
        Text(sub ?: " ", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, maxLines = 1)
    }
}

// ---------------------------------------------------------------- pick sheets

/** The seven mood pictures. Picking one sets today's mood (the latest pick wins); picking the chosen one again, or "Clear", removes it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodPickSheet(current: Mood?, onPick: (Mood?) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.checkin_mood_title), style = MaterialTheme.typography.titleLarge)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Mood.entries.forEach { m ->
                val sel = current == m
                val pop by animateFloatAsState(if (sel) 1.12f else 1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium), label = "pickPop")
                Column(
                    Modifier.clip(RoundedCornerShape(18.dp)).clickable { haptic.performHapticFeedback(HapticFeedbackType.Confirm); onPick(if (sel) null else m) }.padding(6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        Modifier.size(72.dp).graphicsLayer { scaleX = pop; scaleY = pop }.clip(CircleShape)
                            .border(BorderStroke(if (sel) 3.dp else 1.dp, if (sel) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { DiaryMood(m.name, 64.dp) }
                    Text(moodLabel(m), style = MaterialTheme.typography.labelMedium, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        if (current != null) TextButton(onClick = { onPick(null) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.checkin_clear)) }
        Spacer(Modifier.height(12.dp))
    }
}

/** The three food states, as their pictures. Not a diet tool: no calories, no scores. */
@Composable
fun FoodPickSheet(current: FoodState?, onPick: (FoodState?) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.food_question), style = MaterialTheme.typography.titleLarge)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            FoodState.entries.forEach { s ->
                val sel = current == s
                val pop by animateFloatAsState(if (sel) 1.12f else 1f, spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium), label = "foodPop")
                Column(Modifier.clip(RoundedCornerShape(18.dp)).clickable { haptic.performHapticFeedback(HapticFeedbackType.Confirm); onPick(if (sel) null else s) }.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(84.dp).graphicsLayer { scaleX = pop; scaleY = pop }.clip(CircleShape).background(foodColor(s).copy(alpha = 0.22f))
                            .border(BorderStroke(if (sel) 3.dp else 1.5.dp, foodColor(s)), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { FoodEmoji(s.name.lowercase(), 70.dp) }
                    Text(stringResource(foodLabel(s)), style = MaterialTheme.typography.labelLarge, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        if (current != null) TextButton(onClick = { onPick(null) }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.checkin_clear)) }
        Spacer(Modifier.height(12.dp))
    }
}

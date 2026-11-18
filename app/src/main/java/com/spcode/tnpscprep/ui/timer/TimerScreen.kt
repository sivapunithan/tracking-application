package com.spcode.tnpscprep.ui.timer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.spcode.tnpscprep.ui.design.PopSheet
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.spcode.tnpscprep.ui.guide.guideTarget
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.Phase
import com.spcode.tnpscprep.domain.TimerLimits
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.design.ScreenTitle
import com.spcode.tnpscprep.ui.localized
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimerScreen(vm: TimerViewModel, onClose: (() -> Unit)? = null) {
    val ui by vm.ui.collectAsStateWithLifecycle()

    val fb = com.spcode.tnpscprep.ui.guide.rememberGuideBounds()
    val focusScroll = rememberScrollState()
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().verticalScroll(focusScroll).padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp + LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (onClose != null) IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, stringResource(R.string.sheet_close)) }
            ScreenTitle(stringResource(if (onClose != null) R.string.focus_title else R.string.timer_title))
        }
        // --- clock ---
        Column(Modifier.fillMaxWidth().guideTarget(fb, "clock"), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                stringResource(if (ui.phase == Phase.FOCUS) R.string.phase_focus else R.string.phase_break),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                String.format(Locale.ROOT, "%02d:%02d", ui.remainingSeconds / 60, ui.remainingSeconds % 60),
                style = MaterialTheme.typography.displayLarge,
                textAlign = TextAlign.Center,
            )
            LinearProgressIndicator(
                progress = { ui.progress },
                modifier = Modifier.fillMaxWidth(),
                trackColor = MaterialTheme.colorScheme.outlineVariant,
            )
        }

        Row(Modifier.fillMaxWidth().guideTarget(fb, "buttons"), horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally)) {
            Button(onClick = vm::onStartPause) {
                Text(stringResource(if (ui.running) R.string.pause else R.string.start))
            }
            OutlinedButton(onClick = vm::onReset) { Text(stringResource(R.string.reset)) }
            TextButton(onClick = vm::onSkip) { Text(stringResource(R.string.skip)) }
        }

        val nudge by vm.breakNudge.collectAsStateWithLifecycle()
        if (nudge) Text(stringResource(R.string.break_nudge), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.bodyMedium)

        when (val n = ui.notice) {
            TimerNotice.None -> Unit
            TimerNotice.LeftApp -> Text(stringResource(R.string.notice_left_app), color = MaterialTheme.colorScheme.error)
            is TimerNotice.Logged -> Text(stringResource(R.string.notice_logged, n.minutes), color = MaterialTheme.colorScheme.primary)
        }

        // --- optional subject tag: ONE chip that opens a sheet (not a chip per subject) ---
        var pickSubject by rememberSaveable { mutableStateOf(false) }
        val chosen = ui.subjects.firstOrNull { it.id == ui.selectedSubjectId }?.let { localized(it.nameEn, it.nameTa) } ?: stringResource(R.string.subject_none)
        Row(Modifier.guideTarget(fb, "subject"), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.subject_tag_label), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
            AssistChip(
                onClick = { pickSubject = true }, enabled = !ui.running, label = { Text(chosen, maxLines = 1) },
                trailingIcon = { Icon(Icons.Rounded.ArrowDropDown, null) },
            )
        }
        if (pickSubject) {
            PopSheet(onDismiss = { pickSubject = false }) { dismiss ->
                Text(stringResource(R.string.focus_subject_pick), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                Spacer(Modifier.height(12.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = ui.selectedSubjectId == null, onClick = { vm.selectSubject(null); dismiss() }, label = { Text(stringResource(R.string.subject_none)) })
                    ui.subjects.forEach { s ->
                        FilterChip(selected = ui.selectedSubjectId == s.id, onClick = { vm.selectSubject(s.id); dismiss() }, label = { Text(localized(s.nameEn, s.nameTa)) })
                    }
                }
            }
        }
        FocusMusicSlot()

        // --- today + streak ---
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.today_label, ui.todayMinutes, ui.goalMinutes), style = MaterialTheme.typography.titleMedium)
                LinearProgressIndicator(
                    progress = { (ui.todayMinutes.toFloat() / ui.goalMinutes).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    trackColor = MaterialTheme.colorScheme.outlineVariant,
                )
                Text(
                    pluralStringResource(R.plurals.streak_days, ui.streak, ui.streak),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        // --- settings ---
        Card(Modifier.fillMaxWidth().guideTarget(fb, "settings")) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Stepper(stringResource(R.string.focus_minutes_label), ui.focusMinutes, !ui.running,
                    { vm.changeFocusMinutes(-TimerLimits.FOCUS_STEP) }, { vm.changeFocusMinutes(TimerLimits.FOCUS_STEP) })
                Stepper(stringResource(R.string.break_minutes_label), ui.breakMinutes, !ui.running,
                    { vm.changeBreakMinutes(-TimerLimits.BREAK_STEP) }, { vm.changeBreakMinutes(TimerLimits.BREAK_STEP) })
                Stepper(stringResource(R.string.goal_label), ui.goalMinutes, true,
                    { vm.changeDailyGoal(-TimerLimits.GOAL_STEP) }, { vm.changeDailyGoal(TimerLimits.GOAL_STEP) })
            }
        }

        Text(
            stringResource(R.string.timer_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    com.spcode.tnpscprep.ui.guide.PageTour(
        "focus",
        listOf(
            com.spcode.tnpscprep.ui.guide.CoachStep("clock", stringResource(R.string.guide_f1_title), stringResource(R.string.guide_f1_body)),
            com.spcode.tnpscprep.ui.guide.CoachStep("subject", stringResource(R.string.guide_f2_title), stringResource(R.string.guide_f2_body)),
            com.spcode.tnpscprep.ui.guide.CoachStep("settings", stringResource(R.string.guide_f3_title), stringResource(R.string.guide_f3_body)),
        ),
        fb, scrollable = focusScroll,
    )
    }
}

/** SLOT (future): the "Start with music" button goes here. Intentionally empty for now. */
@Composable
private fun FocusMusicSlot() = Unit

@Composable
private fun Stepper(label: String, value: Int, enabled: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) {
    val dec = stringResource(R.string.a11y_decrease)
    val inc = stringResource(R.string.a11y_increase)
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
        OutlinedButton(
            onClick = onMinus, enabled = enabled, contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(48.dp).semantics { contentDescription = "$dec $label" },
        ) { Text("−") }
        Text(value.toString(), Modifier.width(52.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
        OutlinedButton(
            onClick = onPlus, enabled = enabled, contentPadding = PaddingValues(0.dp),
            modifier = Modifier.size(48.dp).semantics { contentDescription = "$inc $label" },
        ) { Text("+") }
    }
}

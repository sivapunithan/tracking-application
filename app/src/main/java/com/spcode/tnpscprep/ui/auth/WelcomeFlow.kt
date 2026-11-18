package com.spcode.tnpscprep.ui.auth

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.AccountForm
import com.spcode.tnpscprep.domain.TimerLimits
import com.spcode.tnpscprep.ui.design.DatePickerContent
import com.spcode.tnpscprep.ui.design.PopSheet
import com.spcode.tnpscprep.ui.syllabus.ExamPicker
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.launch

/** First launch: welcome -> about you -> optional PIN. Creates the LOCAL account (nothing leaves the phone). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeFlow(
    onCreate: suspend (name: String, exam: String, examEpochDay: Long?, goalMinutes: Int, pin: String?) -> Unit,
    onRestore: suspend (android.net.Uri) -> Boolean? = { null },
) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var name by rememberSaveable { mutableStateOf("") }
    var examMask by rememberSaveable { mutableIntStateOf(com.spcode.tnpscprep.domain.Exam.ALL_MASK) }
    var examDay by rememberSaveable { mutableStateOf<Long?>(null) }
    var goal by rememberSaveable { mutableIntStateOf(60) }
    var showDate by rememberSaveable { mutableStateOf(false) }
    var busy by rememberSaveable { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    BackHandler(step > 0) { step -= 1 }

    fun finish(pin: String?) {
        if (busy) return
        busy = true
        scope.launch { onCreate(name, com.spcode.tnpscprep.domain.ExamSelection.format(examMask), examDay, goal, pin) }
    }

    var restoreNote by rememberSaveable { mutableStateOf<String?>(null) }
    if (step == 0) {
        WelcomeLanding(onStart = { step = 1 }, onRestore = onRestore, onNeedName = { restoreNote = it; step = 1 })
        return
    }
    Column(
        Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).verticalScroll(rememberScrollState()).navigationBarsPadding(),
    ) {
        val tamil = androidx.compose.ui.platform.LocalConfiguration.current.locales[0].language == "ta"
        AuthHero(
            if (tamil) listOf("login_vanakkam", "login_wave", "mood_proud") else listOf("login_wave", "login_vanakkam", "mood_proud"),
            height = if (step == 0) 340.dp else 190.dp,
        )
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.Center) {
            repeat(3) { i ->
                Box(
                    Modifier.padding(horizontal = 4.dp).size(if (i == step) 22.dp else 8.dp, 8.dp).clip(CircleShape)
                        .background(if (i == step) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (fadeIn(tween(260, delayMillis = 60)) + slideInHorizontally(spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)) { dir * it / 6 }) togetherWith
                    (fadeOut(tween(120)) + slideOutHorizontally(tween(200)) { -dir * it / 6 })
            },
            label = "welcomeStep",
        ) { s ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                when (s) {
                    0 -> Unit
                    1 -> Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Text(stringResource(R.string.about_you), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
                        restoreNote?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
                        OutlinedTextField(
                            value = name, onValueChange = { name = it.take(AccountForm.MAX_NAME) },
                            label = { Text(stringResource(R.string.account_name_label)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                        )
                        Text(stringResource(R.string.target_exam_label), style = MaterialTheme.typography.titleSmall)
                        ExamPicker(examMask, { examMask = it })
                        Text(stringResource(R.string.onb_exams_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = { showDate = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                            Icon(Icons.Rounded.CalendarMonth, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                examDay?.let { LocalDate.ofEpochDay(it).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)) }
                                    ?: stringResource(R.string.exam_date_optional),
                            )
                        }
                        // gentle nudge, never a blocker: the date can be added later from Today
                        if (examDay == null) Text(stringResource(R.string.onb_date_nudge), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(stringResource(R.string.goal_label), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                            OutlinedButton(onClick = { goal = TimerLimits.step(goal, -TimerLimits.GOAL_STEP, TimerLimits.GOAL) }, modifier = Modifier.size(48.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("−") }
                            Text(goal.toString(), Modifier.width(56.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.titleMedium)
                            OutlinedButton(onClick = { goal = TimerLimits.step(goal, TimerLimits.GOAL_STEP, TimerLimits.GOAL) }, modifier = Modifier.size(48.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) { Text("+") }
                        }
                        Button(onClick = { step = 2 }, enabled = AccountForm.isValidName(name), modifier = Modifier.fillMaxWidth().height(56.dp)) {
                            Text(stringResource(R.string.continue_btn), style = MaterialTheme.typography.titleMedium)
                        }
                    }
                    else -> PinCreateStep(busy = busy, onPin = { finish(it) }, onSkip = { finish(null) })
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (showDate) {
        PopSheet(onDismiss = { showDate = false }) { dismiss ->
            DatePickerContent(
                title = stringResource(R.string.set_exam_date), initial = examDay?.let { LocalDate.ofEpochDay(it) },
                onSave = { examDay = it; dismiss() }, onClear = if (examDay != null) ({ examDay = null; dismiss() }) else null, onCancel = dismiss,
            )
        }
    }
}

/** Choose a 4-digit PIN, then type it again. "Skip" creates the account with no PIN. */
@Composable
private fun PinCreateStep(busy: Boolean, onPin: (String) -> Unit, onSkip: () -> Unit) {
    var first by rememberSaveable { mutableStateOf("") }
    var err by rememberSaveable { mutableIntStateOf(0) }
    var msg by rememberSaveable { mutableStateOf<String?>(null) }
    val mismatch = stringResource(R.string.pin_mismatch)
    Text(stringResource(R.string.pin_title_create), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
    Spacer(Modifier.height(4.dp))
    Text(stringResource(R.string.pin_sub), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    Spacer(Modifier.height(16.dp))
    PinInput(
        prompt = stringResource(if (first.isEmpty()) R.string.pin_choose else R.string.pin_confirm), message = msg, errorTick = err, busy = busy,
        onComplete = { pin ->
            if (first.isEmpty()) { first = pin; msg = null }
            else if (pin == first) onPin(pin)
            else { first = ""; err += 1; msg = mismatch }
        },
    )
    TextButton(onClick = onSkip, enabled = !busy) { Text(stringResource(R.string.pin_skip)) }
}

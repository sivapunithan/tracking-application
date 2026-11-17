package com.spcode.tnpscprep.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.data.HealthKind
import com.spcode.tnpscprep.data.HealthProfile
import com.spcode.tnpscprep.domain.ActivityLevel
import com.spcode.tnpscprep.domain.BmiBand
import com.spcode.tnpscprep.domain.Cycle
import com.spcode.tnpscprep.domain.BodyCycle
import com.spcode.tnpscprep.domain.CyclePhase
import com.spcode.tnpscprep.domain.Health
import com.spcode.tnpscprep.domain.LogicalDay
import com.spcode.tnpscprep.ui.design.DatePickerContent
import com.spcode.tnpscprep.ui.design.PopSheet
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.runtime.saveable.rememberSaveable
import com.spcode.tnpscprep.domain.Sex
import com.spcode.tnpscprep.ui.design.SoftCard
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private fun f1(d: Double) = String.format(Locale.US, "%.1f", d)
private fun f2(d: Double) = String.format(Locale.US, "%.2f", d)
private fun day(d: LocalDate) = d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HealthSheet(vm: HealthViewModel, dismiss: (() -> Unit)?, showTitle: Boolean = true) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    // the app's day changes at 3 AM, like water/food/mood everywhere else
    val today = remember { LogicalDay.of(System.currentTimeMillis(), java.time.ZoneId.systemDefault()) }
    fun latest(kind: String) = entries.firstOrNull { it.kind == kind }?.value
    val weight = latest(HealthKind.WEIGHT); val waist = latest(HealthKind.WAIST); val hip = latest(HealthKind.HIP)
    val sex = if (profile.sex == "MALE") Sex.MALE else Sex.FEMALE
    val level = ActivityLevel.entries.firstOrNull { it.name == profile.activity } ?: ActivityLevel.LIGHT
    val ready = profile.heightCm > 0 && profile.age > 0
    var editProfile by rememberSaveable { mutableStateOf(false) }

    if (showTitle) Text(stringResource(R.string.health_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
    Text(stringResource(R.string.health_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

    // ---- 1. set up once (height, age, sex, usual cycle length) ----
    if (!ready || editProfile) {
        var height by remember(profile) { mutableStateOf(if (profile.heightCm > 0) profile.heightCm.toString() else "") }
        var age by remember(profile) { mutableStateOf(if (profile.age > 0) profile.age.toString() else "") }
        var cycle by remember(profile) { mutableStateOf(profile.cycleDays.toString()) }
        Section(stringResource(R.string.bc_setup_title)) {
            Text(stringResource(R.string.bc_setup_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Num(height, { height = it }, stringResource(R.string.health_height), Modifier.weight(1f))
                Num(age, { age = it }, stringResource(R.string.health_age), Modifier.weight(1f))
                if (sex == Sex.FEMALE) Num(cycle, { cycle = it }, stringResource(R.string.health_cycle_len), Modifier.weight(1f))
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Sex.entries.forEach { s ->
                    FilterChip(selected = s == sex, onClick = { vm.saveProfile(profile.copy(sex = s.name)) },
                        label = { Text(stringResource(if (s == Sex.MALE) R.string.health_male else R.string.health_female)) })
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ActivityLevel.entries.forEach { a ->
                    FilterChip(selected = a == level, onClick = { vm.saveProfile(profile.copy(activity = a.name)) }, label = { Text(stringResource(activityRes(a))) })
                }
            }
            Button(
                onClick = {
                    vm.saveProfile(profile.copy(heightCm = height.toIntOrNull() ?: 0, age = age.toIntOrNull() ?: 0, cycleDays = (cycle.toIntOrNull() ?: 28).coerceIn(Cycle.MIN_DAYS, Cycle.MAX_DAYS)))
                    editProfile = false
                },
                enabled = (height.toIntOrNull() ?: 0) > 0 && (age.toIntOrNull() ?: 0) > 0,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.health_save_profile)) }
        }
    } else {
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Text(
                stringResource(R.string.bc_profile_line, profile.heightCm, profile.age, stringResource(if (sex == Sex.MALE) R.string.health_male else R.string.health_female)),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { editProfile = true }) { Text(stringResource(R.string.bc_edit)) }
        }
    }

    // ---- 2. body: weight, BMI, 30-day trend, quick log ----
    val weightRows = entries.filter { it.kind == HealthKind.WEIGHT }.mapNotNull { e -> runCatching { LocalDate.parse(e.date) }.getOrNull()?.let { it to e.value } }
    Section(stringResource(R.string.bc_body_title)) {
        var w by remember { mutableStateOf("") }
        if (weight == null) {
            Text(stringResource(R.string.bc_no_weight), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            val bmi = if (profile.heightCm > 0) Health.bmi(weight, profile.heightCm.toDouble()) else null
            Row(verticalAlignment = androidx.compose.ui.Alignment.Bottom) {
                Text(stringResource(R.string.bc_kg, f1(weight)), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                if (bmi != null) Text("BMI " + f1(bmi) + " · " + stringResource(bandRes(Health.bmiBand(bmi))), style = MaterialTheme.typography.bodyMedium)
            }
            BodyCycle.weightChange(weightRows, today)?.let { ch ->
                val s = (if (ch > 0) "+" else "") + f1(ch)
                Text(stringResource(R.string.bc_change_30, s), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            WeightTrend(weightRows, today)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
            Num(w, { w = it }, stringResource(R.string.health_weight), Modifier.weight(1f), decimal = true)
            Button(onClick = { w.toDoubleOrNull()?.let { vm.add(HealthKind.WEIGHT, it) }; w = "" }, enabled = w.toDoubleOrNull() != null) { Text(stringResource(R.string.health_add)) }
        }
        val bmr = if (weight != null && ready) Health.bmr(weight, profile.heightCm.toDouble(), profile.age, sex) else null
        if (bmr != null) {
            Line(stringResource(R.string.health_bmr), stringResource(R.string.health_kcal, Math.round(bmr).toInt()))
            Line(stringResource(R.string.health_tdee), stringResource(R.string.health_kcal, Health.tdee(bmr, level)))
        }
        var more by rememberSaveable { mutableStateOf(false) }
        TextButton(onClick = { more = !more }) { Text(stringResource(R.string.bc_more)) }
        if (more) {
            var wa by remember { mutableStateOf("") }; var hi by remember { mutableStateOf("") }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Num(wa, { wa = it }, stringResource(R.string.health_waist), Modifier.weight(1f), decimal = true)
                Num(hi, { hi = it }, stringResource(R.string.health_hip), Modifier.weight(1f), decimal = true)
            }
            OutlinedButton(
                onClick = { wa.toDoubleOrNull()?.let { vm.add(HealthKind.WAIST, it) }; hi.toDoubleOrNull()?.let { vm.add(HealthKind.HIP, it) }; wa = ""; hi = "" },
                enabled = wa.toDoubleOrNull() != null || hi.toDoubleOrNull() != null, modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.health_add)) }
            val whr = if (waist != null && hip != null) Health.waistHipRatio(waist, hip) else null
            if (whr != null) Line(stringResource(R.string.health_whr), f2(whr) + "  ·  " + stringResource(if (Health.whrRaised(whr, sex)) R.string.health_whr_raised else R.string.health_whr_ok))
        }
    }

    // ---- 3. cycle: today's day, next period, fertile window, average length ----
    if (sex == Sex.FEMALE) {
        val startEntries = entries.filter { it.kind == HealthKind.PERIOD }
        val starts = startEntries.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
        val st = BodyCycle.cycleStatus(starts, profile.cycleDays, today)
        var pickDate by rememberSaveable { mutableStateOf(false) }
        Section(stringResource(R.string.bc_cycle_title)) {
            if (st == null) {
                Text(stringResource(R.string.bc_first_period), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                Text(stringResource(R.string.bc_cycle_day, st.dayOfCycle, st.cycleLength), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                CycleBar(st)
                Text(
                    stringResource(when (st.phase) { CyclePhase.PERIOD -> R.string.bc_phase_period; CyclePhase.FERTILE -> R.string.bc_phase_fertile; CyclePhase.LATER -> R.string.bc_phase_later }),
                    style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary,
                )
                if (st.daysToNext >= 0) Line(stringResource(R.string.health_next_period), day(st.estimate.nextPeriod) + " · " + pluralStringResource(R.plurals.bc_days_to_go, st.daysToNext, st.daysToNext))
                else Text(stringResource(R.string.bc_cycle_late), style = MaterialTheme.typography.bodyMedium)
                Line(stringResource(R.string.health_fertile), day(st.estimate.fertileFrom) + " – " + day(st.estimate.fertileTo))
                Text(
                    if (st.cyclesUsed > 0) stringResource(R.string.bc_avg, st.cycleLength, st.cyclesUsed) else stringResource(R.string.bc_avg_default, st.cycleLength),
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { vm.add(HealthKind.PERIOD, 1.0) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.health_period_today)) }
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.bc_period_other)) }
            }
            startEntries.maxByOrNull { it.date }?.let { last ->
                TextButton(onClick = { vm.delete(last.id) }) { Text(stringResource(R.string.bc_period_undo, day(LocalDate.parse(last.date)))) }
            }
        }
        if (pickDate) PopSheet(onDismiss = { pickDate = false }) { close ->
            DatePickerContent(stringResource(R.string.health_period_short), today, onSave = { ed -> vm.addOn(HealthKind.PERIOD, 1.0, LocalDate.ofEpochDay(ed).coerceAtMost(today)); close() }, onClear = null, onCancel = close)
        }
    }

    // ---- history + erase ----
    if (entries.isNotEmpty()) {
        Section(stringResource(R.string.health_history)) {
            entries.filter { it.kind != HealthKind.WATER }.take(12).forEach { e ->
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Text(day(LocalDate.parse(e.date)) + "  ·  " + kindLabel(e.kind) + " " + (if (e.kind == HealthKind.PERIOD) "" else f1(e.value)), style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.delete(e.id) }) { Text(stringResource(R.string.health_delete)) }
                }
            }
        }
    }
    var confirm by remember { mutableStateOf(false) }
    OutlinedButton(
        onClick = { if (confirm) { vm.deleteAll(); confirm = false } else confirm = true },
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
    ) { Text(stringResource(if (confirm) R.string.health_delete_all_confirm else R.string.health_delete_all), color = MaterialTheme.colorScheme.error) }
    if (dismiss != null) TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sheet_close)) }
}

/** 30-day weight line (one point per reading, no axes: the change is stated in words above). */
@Composable
private fun WeightTrend(rows: List<Pair<LocalDate, Double>>, today: LocalDate) {
    val pts = rows.filter { !it.first.isBefore(today.minusDays(30)) && !it.first.isAfter(today) }.sortedBy { it.first }
    if (pts.size < 2) return
    val lo = pts.minOf { it.second }; val hi = pts.maxOf { it.second }; val span = (hi - lo).coerceAtLeast(0.5)
    val color = MaterialTheme.colorScheme.primary; val track = MaterialTheme.colorScheme.outlineVariant
    Canvas(Modifier.fillMaxWidth().height(56.dp)) {
        val x = { d: LocalDate -> (java.time.temporal.ChronoUnit.DAYS.between(today.minusDays(30), d) / 30f) * size.width }
        val y = { v: Double -> size.height - 6f - ((v - lo) / span).toFloat() * (size.height - 12f) }
        drawLine(track, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), strokeWidth = 2f)
        pts.zipWithNext { a, b -> drawLine(color, Offset(x(a.first), y(a.second)), Offset(x(b.first), y(b.second)), strokeWidth = 5f, cap = StrokeCap.Round) }
        pts.forEach { drawCircle(color, 6f, Offset(x(it.first), y(it.second))) }
    }
}

/** The cycle as a bar: period days (1-5) and the fertile window tinted, a dot for today. Estimates only. */
@Composable
private fun CycleBar(st: com.spcode.tnpscprep.domain.CycleStatus) {
    val primary = MaterialTheme.colorScheme.primary; val track = MaterialTheme.colorScheme.outlineVariant; val gold = MaterialTheme.colorScheme.tertiary
    Canvas(Modifier.fillMaxWidth().height(22.dp)) {
        val n = st.cycleLength.toFloat(); val h = 10f; val top = (size.height - h) / 2
        fun seg(from: Float, to: Float, c: androidx.compose.ui.graphics.Color) = drawRect(c, Offset(from / n * size.width, top), androidx.compose.ui.geometry.Size((to - from) / n * size.width, h))
        drawRoundRect(track, Offset(0f, top), androidx.compose.ui.geometry.Size(size.width, h), androidx.compose.ui.geometry.CornerRadius(5f))
        seg(0f, 5f, primary.copy(alpha = 0.7f))
        val fs = java.time.temporal.ChronoUnit.DAYS.between(st.lastStart, st.estimate.fertileFrom).toFloat().coerceAtLeast(5f)
        val fe = (java.time.temporal.ChronoUnit.DAYS.between(st.lastStart, st.estimate.fertileTo).toFloat() + 1f).coerceAtMost(n)
        if (fe > fs) seg(fs, fe, gold.copy(alpha = 0.75f))
        val cx = ((st.dayOfCycle - 0.5f).coerceIn(0f, n) / n) * size.width
        drawCircle(androidx.compose.ui.graphics.Color.White, 11f, Offset(cx, size.height / 2)); drawCircle(primary, 8f, Offset(cx, size.height / 2))
    }
}

@Composable
private fun kindLabel(kind: String) = stringResource(
    when (kind) {
        HealthKind.WEIGHT -> R.string.health_weight; HealthKind.WAIST -> R.string.health_waist; HealthKind.HIP -> R.string.health_hip
        HealthKind.WATER -> R.string.health_water; else -> R.string.health_period_short
    },
)

private fun activityRes(a: ActivityLevel) = when (a) {
    ActivityLevel.SEDENTARY -> R.string.act_sedentary; ActivityLevel.LIGHT -> R.string.act_light; ActivityLevel.MODERATE -> R.string.act_moderate
    ActivityLevel.ACTIVE -> R.string.act_active; ActivityLevel.VERY_ACTIVE -> R.string.act_very
}

private fun bandRes(b: BmiBand) = when (b) {
    BmiBand.UNDER -> R.string.bmi_under; BmiBand.NORMAL -> R.string.bmi_normal; BmiBand.OVER -> R.string.bmi_over; BmiBand.OBESE -> R.string.bmi_obese
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
        content()
    }
}

@Composable
private fun Line(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun Num(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier, decimal: Boolean = false) {
    OutlinedTextField(
        value = value, onValueChange = { onChange(it.filter { c -> c.isDigit() || (decimal && c == '.') }.take(6)) },
        label = { Text(label, maxLines = 1) }, singleLine = true, modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
    )
}

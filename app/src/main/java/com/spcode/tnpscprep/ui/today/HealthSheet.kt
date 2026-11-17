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
import com.spcode.tnpscprep.domain.Health
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
fun HealthSheet(vm: HealthViewModel, dismiss: () -> Unit) {
    val entries by vm.entries.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val today = remember { LocalDate.now() }
    fun latest(kind: String) = entries.firstOrNull { it.kind == kind }?.value
    val weight = latest(HealthKind.WEIGHT); val waist = latest(HealthKind.WAIST); val hip = latest(HealthKind.HIP)
    val sex = if (profile.sex == "MALE") Sex.MALE else Sex.FEMALE
    val level = ActivityLevel.entries.firstOrNull { it.name == profile.activity } ?: ActivityLevel.LIGHT
    val waterToday = entries.filter { it.kind == HealthKind.WATER && it.date == today.toString() }.sumOf { it.value }.toInt()

    Text(stringResource(R.string.health_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
    Text(stringResource(R.string.health_disclaimer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
    Spacer(Modifier.height(14.dp))

    // ---- profile ----
    var height by remember(profile) { mutableStateOf(if (profile.heightCm > 0) profile.heightCm.toString() else "") }
    var age by remember(profile) { mutableStateOf(if (profile.age > 0) profile.age.toString() else "") }
    var cycle by remember(profile) { mutableStateOf(profile.cycleDays.toString()) }
    Section(stringResource(R.string.health_profile)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Num(height, { height = it }, stringResource(R.string.health_height), Modifier.weight(1f))
            Num(age, { age = it }, stringResource(R.string.health_age), Modifier.weight(1f))
            Num(cycle, { cycle = it }, stringResource(R.string.health_cycle_len), Modifier.weight(1f))
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Sex.entries.forEach { s ->
                FilterChip(selected = s == sex, onClick = { vm.saveProfile(profile.copy(sex = s.name)) },
                    label = { Text(stringResource(if (s == Sex.MALE) R.string.health_male else R.string.health_female)) })
            }
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ActivityLevel.entries.forEach { a ->
                FilterChip(selected = a == level, onClick = { vm.saveProfile(profile.copy(activity = a.name)) },
                    label = { Text(stringResource(activityRes(a))) })
            }
        }
        OutlinedButton(
            onClick = { vm.saveProfile(profile.copy(heightCm = height.toIntOrNull() ?: 0, age = age.toIntOrNull() ?: 0, cycleDays = (cycle.toIntOrNull() ?: 28).coerceIn(Cycle.MIN_DAYS, Cycle.MAX_DAYS))) },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.health_save_profile)) }
    }

    // ---- log ----
    var w by remember { mutableStateOf("") }; var wa by remember { mutableStateOf("") }; var hi by remember { mutableStateOf("") }
    Section(stringResource(R.string.health_log_today)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Num(w, { w = it }, stringResource(R.string.health_weight), Modifier.weight(1f), decimal = true)
            Num(wa, { wa = it }, stringResource(R.string.health_waist), Modifier.weight(1f), decimal = true)
            Num(hi, { hi = it }, stringResource(R.string.health_hip), Modifier.weight(1f), decimal = true)
        }
        Button(
            onClick = {
                w.toDoubleOrNull()?.let { vm.add(HealthKind.WEIGHT, it) }
                wa.toDoubleOrNull()?.let { vm.add(HealthKind.WAIST, it) }
                hi.toDoubleOrNull()?.let { vm.add(HealthKind.HIP, it) }
                w = ""; wa = ""; hi = ""
            },
            enabled = w.toDoubleOrNull() != null || wa.toDoubleOrNull() != null || hi.toDoubleOrNull() != null,
            modifier = Modifier.fillMaxWidth(),
        ) { Text(stringResource(R.string.health_add)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { vm.add(HealthKind.WATER, 250.0) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.health_water_add)) }
            OutlinedButton(onClick = { vm.add(HealthKind.PERIOD, 1.0) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.health_period_today)) }
        }
    }

    // ---- results (estimates) ----
    Section(stringResource(R.string.health_results)) {
        val bmi = if (weight != null && profile.heightCm > 0) Health.bmi(weight, profile.heightCm.toDouble()) else null
        if (bmi != null) {
            val band = Health.bmiBand(bmi)
            Line(stringResource(R.string.health_bmi), f1(bmi) + "  ·  " + stringResource(bandRes(band)))
        }
        val bmr = if (weight != null && profile.heightCm > 0 && profile.age > 0) Health.bmr(weight, profile.heightCm.toDouble(), profile.age, sex) else null
        if (bmr != null) {
            Line(stringResource(R.string.health_bmr), stringResource(R.string.health_kcal, Math.round(bmr).toInt()))
            Line(stringResource(R.string.health_tdee), stringResource(R.string.health_kcal, Health.tdee(bmr, level)))
        }
        val whr = if (waist != null && hip != null) Health.waistHipRatio(waist, hip) else null
        if (whr != null) Line(stringResource(R.string.health_whr), f2(whr) + "  ·  " + stringResource(if (Health.whrRaised(whr, sex)) R.string.health_whr_raised else R.string.health_whr_ok))
        val target = weight?.let { Health.waterMl(it) }
        if (target != null) Line(stringResource(R.string.health_water), stringResource(R.string.health_water_val, waterToday, target))
        else if (waterToday > 0) Line(stringResource(R.string.health_water), stringResource(R.string.health_ml, waterToday))

        val starts = entries.filter { it.kind == HealthKind.PERIOD }.mapNotNull { runCatching { LocalDate.parse(it.date) }.getOrNull() }
        val lastStart = starts.maxOrNull()
        val len = Cycle.averageLength(starts) ?: profile.cycleDays
        val est = lastStart?.let { Cycle.estimate(it, len) }
        if (est != null) {
            Line(stringResource(R.string.health_next_period), day(est.nextPeriod))
            Line(stringResource(R.string.health_fertile), day(est.fertileFrom) + " – " + day(est.fertileTo))
        }
        if (bmi == null && bmr == null && whr == null && est == null && target == null) {
            Text(stringResource(R.string.health_empty), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }

    // ---- history + erase ----
    if (entries.isNotEmpty()) {
        Section(stringResource(R.string.health_history)) {
            entries.take(15).forEach { e ->
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
    TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sheet_close)) }
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

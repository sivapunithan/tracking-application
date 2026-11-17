package com.spcode.tnpscprep.ui.today

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.net.Uri
import android.os.Build
import android.os.SystemClock
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.TnpscApp
import com.spcode.tnpscprep.data.StepRepository
import com.spcode.tnpscprep.ui.design.PopSheet
import com.spcode.tnpscprep.ui.design.SoftCard
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.coroutines.resume
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull

/** True when the phone has a step counter; otherwise the whole feature stays hidden. */
fun hasStepSensor(ctx: Context): Boolean =
    (ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager)?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null

fun stepsAllowed(ctx: Context) = permissionGranted(ctx)

private fun permissionGranted(ctx: Context) =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED

/** One-shot read: register, take the first value, unregister. No service, nothing keeps running. */
private suspend fun readOnce(ctx: Context): Long? {
    val sm = ctx.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return null
    val sensor = sm.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return null
    return withTimeoutOrNull(3_000) {
        suspendCancellableCoroutine { cont ->
            val l = object : SensorEventListener {
                override fun onSensorChanged(e: SensorEvent) { sm.unregisterListener(this); if (cont.isActive) cont.resume(e.values[0].toLong()) }
                override fun onAccuracyChanged(s: Sensor?, a: Int) = Unit
            }
            sm.registerListener(l, sensor, SensorManager.SENSOR_DELAY_NORMAL)
            cont.invokeOnCancellation { sm.unregisterListener(l) }
        }
    }
}

class StepsViewModel(private val repo: StepRepository) : ViewModel() {
    val week: StateFlow<List<com.spcode.tnpscprep.data.db.StepDayEntity>> =
        repo.observeRecent(LocalDate.now().minusDays(6)).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun record(reading: Long) {
        viewModelScope.launch { repo.record(reading, System.currentTimeMillis() - SystemClock.elapsedRealtime(), LocalDate.now()) }
    }
}

/** Takes one step-counter reading whenever the app comes to the front (if the sensor exists and she allowed it). Draws nothing. */
@Composable
fun StepsAutoRead() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as TnpscApp
    val vm = viewModel<StepsViewModel>(factory = viewModelFactory { initializer { StepsViewModel(app.container.stepRepository) } })
    val scope = rememberCoroutineScope()
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        if (hasStepSensor(ctx) && permissionGranted(ctx)) scope.launch { readOnce(ctx)?.let(vm::record) }
    }
}

@Composable
fun StepsCard() {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as TnpscApp
    val vm = viewModel<StepsViewModel>(factory = viewModelFactory { initializer { StepsViewModel(app.container.stepRepository) } })
    val week by vm.week.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var granted by remember { mutableStateOf(permissionGranted(ctx)) }
    var denied by remember { mutableStateOf(false) }
    var explain by remember { mutableStateOf(false) }

    fun read() { scope.launch { readOnce(ctx)?.let(vm::record) } }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        granted = ok; denied = !ok; if (ok) read()
    }
    // Every time the app comes to the front: take one reading (this is the ONLY moment steps are counted).
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        granted = permissionGranted(ctx)
        if (granted) read()
    }

    val today = LocalDate.now()
    val todaySteps = week.firstOrNull { it.date == today.toString() }?.steps

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.steps_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 6.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!granted) {
                    Text(stringResource(R.string.steps_off), style = MaterialTheme.typography.bodyMedium)
                    if (denied) {
                        OutlinedButton(
                            onClick = { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))) },
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(stringResource(R.string.steps_open_settings)) }
                    } else {
                        Button(onClick = { explain = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.steps_turn_on)) }
                    }
                } else {
                    Text("~" + (todaySteps ?: 0), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Text(stringResource(R.string.steps_approx), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    WeekBars(week, today)
                }
            }
        }
    }

    if (explain) {
        PopSheet(onDismiss = { explain = false }) { dismiss ->
            Text(stringResource(R.string.steps_explain_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.steps_explain_body), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            Button(
                onClick = {
                    explain = false
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) launcher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) { Text(stringResource(R.string.steps_continue)) }
            TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.not_now)) }
        }
    }
}

@Composable
private fun WeekBars(week: List<com.spcode.tnpscprep.data.db.StepDayEntity>, today: LocalDate) {
    val byDate = week.associateBy { it.date }
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val max = (days.maxOf { byDate[it.toString()]?.steps ?: 0 }).coerceAtLeast(1000)
    Row(Modifier.fillMaxWidth().height(86.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        days.forEach { d ->
            val steps = byDate[d.toString()]?.steps ?: 0
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f))
                Box(
                    Modifier.fillMaxWidth().height((60.dp * steps / max).coerceAtLeast(3.dp)).background(
                        if (d == today) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.4f), RoundedCornerShape(4.dp),
                    ),
                )
                Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

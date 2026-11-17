package com.spcode.tnpscprep.ui.today

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import com.spcode.tnpscprep.data.SettingsStore
import com.spcode.tnpscprep.data.UsageRepository
import com.spcode.tnpscprep.data.db.UsageDayEntity
import com.spcode.tnpscprep.data.db.UsageTotalEntity
import com.spcode.tnpscprep.domain.PhoneUsage
import com.spcode.tnpscprep.ui.design.PopSheet
import com.spcode.tnpscprep.ui.design.SoftCard
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class UsageViewModel(private val repo: UsageRepository, private val settings: SettingsStore) : ViewModel() {
    private val from = LocalDate.now().minusDays(6)
    val enabled: StateFlow<Boolean> = settings.usageEnabled
    val totals: StateFlow<List<UsageTotalEntity>> = repo.observeTotals(from).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val apps: StateFlow<List<UsageDayEntity>> = repo.observeApps(from).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun hasAccess() = repo.hasAccess()
    fun settingsIntent() = repo.settingsIntent()
    fun setEnabled(v: Boolean) = settings.setUsageEnabled(v)
    fun refresh() { viewModelScope.launch { repo.snapshot() } }
    fun deleteAll() { viewModelScope.launch { repo.deleteAll(); settings.setUsageEnabled(false) } }
}

@Composable
fun UsageCard(studyMinutesByDate: Map<LocalDate, Int>) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as TnpscApp
    val vm = viewModel<UsageViewModel>(factory = viewModelFactory { initializer { UsageViewModel(app.container.usageRepository, app.container.settingsStore) } })
    val enabled by vm.enabled.collectAsStateWithLifecycle()
    val totals by vm.totals.collectAsStateWithLifecycle()
    val apps by vm.apps.collectAsStateWithLifecycle()
    var access by remember { mutableStateOf(vm.hasAccess()) }
    var explain by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    // The only moment usage is read: when the app comes to the front (and she has opted in and allowed access).
    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        access = vm.hasAccess()
        if (enabled && access) vm.refresh()
    }

    val today = LocalDate.now()
    val live = enabled && access
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.usage_title), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 6.dp))
        SoftCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!live) {
                    Text(stringResource(R.string.usage_off), style = MaterialTheme.typography.bodyMedium)
                    Button(onClick = { explain = true }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.usage_turn_on)) }
                } else {
                    val byDate = totals.associate { it.date to it.minutes }
                    val todayMin = byDate[today.toString()] ?: 0
                    Text(PhoneUsage.label(todayMin), style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
                    Text(
                        stringResource(R.string.usage_today_vs_study, studyMinutesByDate[today] ?: 0),
                        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    apps.filter { it.date == today.toString() }.take(5).forEach { a ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(a.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f), maxLines = 1)
                            Text(PhoneUsage.label(a.minutes), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                        }
                    }
                    UsageBars(byDate, studyMinutesByDate, today)
                    Text(stringResource(R.string.usage_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { vm.setEnabled(false) }, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.usage_turn_off)) }
                        OutlinedButton(
                            onClick = { if (confirmDelete) { vm.deleteAll(); confirmDelete = false } else confirmDelete = true },
                            modifier = Modifier.weight(1f),
                        ) { Text(stringResource(if (confirmDelete) R.string.usage_delete_confirm else R.string.usage_delete), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }

    if (explain) {
        PopSheet(onDismiss = { explain = false }) { dismiss ->
            Text(stringResource(R.string.usage_explain_title), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.usage_explain_body), style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(18.dp))
            val intent = vm.settingsIntent()
            if (intent == null) {
                Text(stringResource(R.string.usage_unavailable), color = MaterialTheme.colorScheme.error)
            } else {
                Button(
                    onClick = { vm.setEnabled(true); explain = false; ctx.startActivity(intent) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                ) { Text(stringResource(R.string.usage_open_settings)) }
            }
            TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.not_now)) }
        }
    }
}

@Composable
private fun UsageBars(phone: Map<String, Int>, study: Map<LocalDate, Int>, today: LocalDate) {
    val days = (6 downTo 0).map { today.minusDays(it.toLong()) }
    val max = days.maxOf { phone[it.toString()] ?: 0 }.coerceAtLeast(60)
    Row(Modifier.fillMaxWidth().height(110.dp), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.Bottom) {
        days.forEach { d ->
            val m = phone[d.toString()] ?: 0
            Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.weight(1f))
                Text(if (m > 0) PhoneUsage.label(m) else "", style = MaterialTheme.typography.labelSmall, maxLines = 1)
                Box(
                    Modifier.fillMaxWidth().height((60.dp * m / max).coerceAtLeast(3.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = if (d == today) 1f else 0.45f), RoundedCornerShape(4.dp)),
                )
                Text(d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()), style = MaterialTheme.typography.labelSmall)
                Text("${study[d] ?: 0}'", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

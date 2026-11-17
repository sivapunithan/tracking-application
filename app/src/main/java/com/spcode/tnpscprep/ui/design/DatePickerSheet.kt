package com.spcode.tnpscprep.ui.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.R
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Pop-up content for picking a date. Returns the chosen day as an epoch day (the picker works in UTC midnight; converted back here). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerContent(title: String, initial: LocalDate?, onSave: (Long) -> Unit, onClear: (() -> Unit)?, onCancel: () -> Unit) {
    val state = rememberDatePickerState(initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli())
    Text(title, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
    DatePicker(state = state, showModeToggle = false, title = null, headline = null)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        if (onClear != null) OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f).height(52.dp)) { Text(stringResource(R.string.clear)) }
        Button(
            onClick = { state.selectedDateMillis?.let { onSave(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) } ?: onCancel() },
            modifier = Modifier.weight(1f).height(52.dp),
        ) { Text(stringResource(R.string.ok)) }
    }
    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
}

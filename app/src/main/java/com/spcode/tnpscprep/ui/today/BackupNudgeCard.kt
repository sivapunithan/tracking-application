package com.spcode.tnpscprep.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.R

/** A soft reminder, no guilt: just the facts and where the button is. [days] = null when she has never backed up. */
@Composable
fun BackupNudgeCard(days: Int?, onOpen: () -> Unit, onLater: () -> Unit, onTurnOff: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(R.string.bkn_title), style = MaterialTheme.typography.titleMedium)
            Text(
                if (days == null) stringResource(R.string.bkn_never) else pluralStringResource(R.plurals.bkn_days, days, days),
                style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(stringResource(R.string.bkn_where), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onOpen) { Text(stringResource(R.string.bkn_open)) }
                TextButton(onClick = onLater) { Text(stringResource(R.string.bkn_later)) }
                TextButton(onClick = onTurnOff) { Text(stringResource(R.string.bkn_off)) }
            }
        }
    }
}

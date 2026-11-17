package com.spcode.tnpscprep.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.R

@Composable
fun SettingsScreen() {
    var selected by remember { mutableStateOf(AppLocale.current()) }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.language_label), style = MaterialTheme.typography.titleMedium)
        Column(Modifier.selectableGroup()) {
            AppLanguage.entries.forEach { lang ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp)
                        .selectable(
                            selected = lang == selected,
                            role = Role.RadioButton,
                            onClick = {
                                if (lang != selected) {
                                    selected = lang
                                    AppLocale.apply(lang) // recreates the Activity
                                }
                            },
                        ),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    RadioButton(selected = lang == selected, onClick = null) // click handled by the row
                    Text(
                        // Language names are shown in their own language so they're always findable.
                        when (lang) {
                            AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                            AppLanguage.ENGLISH -> "English"
                            AppLanguage.TAMIL -> "தமிழ்"
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }
            }
        }
        Text(
            stringResource(R.string.language_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

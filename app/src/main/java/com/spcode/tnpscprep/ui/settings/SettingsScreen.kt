package com.spcode.tnpscprep.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import android.text.format.DateUtils
import com.spcode.tnpscprep.ui.affairs.HeadlinesViewModel
import com.spcode.tnpscprep.ui.auth.AccountSection
import com.spcode.tnpscprep.ui.auth.AuthViewModel
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.design.ScreenTitle
import com.spcode.tnpscprep.ui.mood.MoodLabSheet
import com.spcode.tnpscprep.ui.theme.ThemeMode
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.spcode.tnpscprep.R

@Composable
fun SettingsScreen(headlines: HeadlinesViewModel, auth: AuthViewModel) {
    var selected by remember { mutableStateOf(AppLocale.current()) }
    val newsOn by headlines.enabled.collectAsStateWithLifecycle()
    val lastMs by headlines.lastRefreshMs.collectAsStateWithLifecycle()

    val themeMode by headlines.themeMode.collectAsStateWithLifecycle()
    val name by headlines.userName.collectAsStateWithLifecycle()
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp + LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ScreenTitle(stringResource(R.string.settings_title))
        Text(stringResource(R.string.appearance_title), style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ThemeMode.entries.forEach { m ->
                FilterChip(
                    selected = themeMode == m.name,
                    onClick = { headlines.setThemeMode(m.name) },
                    label = {
                        Text(
                            stringResource(
                                when (m) {
                                    ThemeMode.LIGHT -> R.string.theme_light
                                    ThemeMode.DARK -> R.string.theme_dark
                                    ThemeMode.SYSTEM -> R.string.theme_system
                                },
                            ),
                        )
                    },
                )
            }
        }
        OutlinedTextField(
            value = name,
            onValueChange = headlines::setUserName,
            label = { Text(stringResource(R.string.profile_name_label)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        AccountSection(auth)
        Spacer(Modifier.height(8.dp))
        var showMoods by rememberSaveable { mutableStateOf(false) }
        OutlinedButton(onClick = { showMoods = true }) { Text(stringResource(R.string.mood_lab_button)) }
        if (showMoods) MoodLabSheet(onDismiss = { showMoods = false })
        Spacer(Modifier.height(8.dp))
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
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.settings_news_title), style = MaterialTheme.typography.titleMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_news_switch), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = newsOn, onCheckedChange = headlines::setEnabled)
        }
        Text(
            stringResource(R.string.settings_news_note) + if (lastMs > 0) "\n" + stringResource(
                R.string.settings_news_last,
                DateUtils.getRelativeTimeSpanString(lastMs, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
            ) else "",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        val foodSettings = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.spcode.tnpscprep.TnpscApp).container.settingsStore
        val strictFood by foodSettings.strictFood.collectAsStateWithLifecycle()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.settings_strict), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Switch(checked = strictFood, onCheckedChange = foodSettings::setStrictFood)
        }
        Text(stringResource(R.string.settings_strict_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.syllabus_source_title), style = MaterialTheme.typography.titleMedium)
        Text(
            stringResource(R.string.syllabus_source_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

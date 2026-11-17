package com.spcode.tnpscprep.ui.settings

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.ExamSelection
import com.spcode.tnpscprep.ui.affairs.HeadlinesViewModel
import com.spcode.tnpscprep.ui.auth.AccountSection
import com.spcode.tnpscprep.ui.auth.AuthViewModel
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.design.ScreenTitle
import com.spcode.tnpscprep.ui.mood.MoodLabSheet
import com.spcode.tnpscprep.ui.theme.ThemeMode

/**
 * Profile = a hub. The top card holds who she is (name + exams); each row below opens ONE short page
 * (study, evening, food, look, privacy, data). Why: the old page was one long scroll of eight unrelated sections.
 * The open page is saved (rememberSaveable) and Back returns to the hub before it leaves Profile.
 */
@Composable
fun ProfileScreen(headlines: HeadlinesViewModel, auth: AuthViewModel, studyByDate: Map<java.time.LocalDate, Int>, onBack: () -> Unit) {
    val settings = (androidx.compose.ui.platform.LocalContext.current.applicationContext as com.spcode.tnpscprep.TnpscApp).container.settingsStore
    var page by rememberSaveable { mutableStateOf("hub") }
    BackHandler(enabled = page != "hub") { page = "hub" }
    val titles = mapOf(
        "study" to R.string.profile_pg_study, "evening" to R.string.profile_pg_evening, "food" to R.string.profile_pg_food,
        "look" to R.string.profile_pg_look, "privacy" to R.string.profile_pg_privacy, "data" to R.string.profile_pg_data,
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp + LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = { if (page == "hub") onBack() else page = "hub" }) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.sheet_close)) }
            ScreenTitle(stringResource(titles[page] ?: R.string.profile_title))
        }
        when (page) {
            "study" -> StudyPage(settings)
            "evening" -> EveningPage(settings)
            "food" -> FoodPage(settings)
            "look" -> LookPage(headlines)
            "privacy" -> PrivacyPage(headlines, auth, studyByDate)
            "data" -> DataPage(auth)
            else -> Hub(headlines, settings) { page = it }
        }
    }
}

@Composable
private fun Hub(headlines: HeadlinesViewModel, settings: com.spcode.tnpscprep.data.SettingsStore, open: (String) -> Unit) {
    val name by headlines.userName.collectAsStateWithLifecycle()
    val mask by settings.examMask.collectAsStateWithLifecycle()
    val goal by settings.dailyGoalMinutes.collectAsStateWithLifecycle()
    val wrapHour by settings.wrapupHour.collectAsStateWithLifecycle()
    val reminderOn by settings.reminderEnabled.collectAsStateWithLifecycle()
    val strict by settings.strictFood.collectAsStateWithLifecycle()
    val themeMode by headlines.themeMode.collectAsStateWithLifecycle()
    val exams = ExamSelection.codes(mask).joinToString(" · ")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).background(Color.White.copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                    Text(name.trim().take(1).uppercase().ifEmpty { "•" }, style = MaterialTheme.typography.headlineSmall, color = Color.White)
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(name.ifBlank { stringResource(R.string.profile_title) }, style = MaterialTheme.typography.titleLarge, color = Color.White)
                    Text(exams.ifEmpty { stringResource(R.string.exam_all) }, style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.85f))
                }
            }
            OutlinedTextField(
                value = name, onValueChange = headlines::setUserName, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_name_label)) },
                colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedLabelColor = Color.White, unfocusedLabelColor = Color.White.copy(alpha = 0.8f),
                    focusedBorderColor = Color.White, unfocusedBorderColor = Color.White.copy(alpha = 0.5f), cursorColor = Color.White,
                ),
            )
        }
    }
    val on = stringResource(R.string.state_on); val off = stringResource(R.string.state_off)
    HubRow("📚", stringResource(R.string.profile_pg_study), stringResource(R.string.profile_sub_study, exams.ifEmpty { stringResource(R.string.exam_all) }, goal)) { open("study") }
    HubRow("🌙", stringResource(R.string.profile_pg_evening), stringResource(R.string.profile_sub_evening, wrapHour, if (reminderOn) on else off)) { open("evening") }
    HubRow("🥗", stringResource(R.string.profile_pg_food), stringResource(R.string.profile_sub_food, if (strict) on else off)) { open("food") }
    HubRow("🎨", stringResource(R.string.profile_pg_look), stringResource(R.string.profile_sub_look, themeLabel(themeMode), languageLabel(AppLocale.current()))) { open("look") }
    HubRow("🔒", stringResource(R.string.profile_pg_privacy), stringResource(R.string.profile_sub_privacy)) { open("privacy") }
    HubRow("🗂", stringResource(R.string.profile_pg_data), stringResource(R.string.profile_sub_data)) { open("data") }
}

@Composable
private fun themeLabel(mode: String): String = stringResource(
    when (ThemeMode.fromName(mode)) { ThemeMode.LIGHT -> R.string.theme_light; ThemeMode.DARK -> R.string.theme_dark; ThemeMode.SYSTEM -> R.string.theme_system },
)

@Composable
private fun languageLabel(l: AppLanguage): String = when (l) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system); AppLanguage.ENGLISH -> "English"; AppLanguage.TAMIL -> "தமிழ்"
}

@Composable
private fun HubRow(icon: String, title: String, summary: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(icon, style = MaterialTheme.typography.headlineSmall)
            Column(Modifier.weight(1f).padding(horizontal = 14.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
            Text("›", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StudyPage(settings: com.spcode.tnpscprep.data.SettingsStore) {
    val examMask by settings.examMask.collectAsStateWithLifecycle()
    val goal by settings.dailyGoalMinutes.collectAsStateWithLifecycle()
    Text(stringResource(R.string.my_exams), style = MaterialTheme.typography.titleMedium)
    com.spcode.tnpscprep.ui.syllabus.ExamPicker(examMask, settings::setExamMask)
    Text(stringResource(R.string.my_exams_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.profile_goal), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { settings.setDailyGoalMinutes(goal - 15) }) { Text("−") }
        Text(stringResource(R.string.profile_min_fmt, goal), Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { settings.setDailyGoalMinutes(goal + 15) }) { Text("+") }
    }
}

@Composable
private fun EveningPage(settings: com.spcode.tnpscprep.data.SettingsStore) {
    val wrapHour by settings.wrapupHour.collectAsStateWithLifecycle()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.profile_wrap_time), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { settings.setWrapupHour(wrapHour - 1) }, enabled = wrapHour > 15) { Text("−") }
        Text(stringResource(R.string.wrap_hour_fmt, wrapHour), Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { settings.setWrapupHour(wrapHour + 1) }, enabled = wrapHour < 23) { Text("+") }
    }
    ReminderSection(settings)
}

@Composable
private fun FoodPage(settings: com.spcode.tnpscprep.data.SettingsStore) {
    val strictFood by settings.strictFood.collectAsStateWithLifecycle()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.settings_strict), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = strictFood, onCheckedChange = settings::setStrictFood)
    }
    Text(stringResource(R.string.settings_strict_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun LookPage(headlines: HeadlinesViewModel) {
    var selected by remember { mutableStateOf(AppLocale.current()) }
    val themeMode by headlines.themeMode.collectAsStateWithLifecycle()
    SectionLabel(stringResource(R.string.appearance_title))
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ThemeMode.entries.forEach { m ->
            FilterChip(selected = themeMode == m.name, onClick = { headlines.setThemeMode(m.name) }, label = { Text(themeLabel(m.name)) })
        }
    }
    Spacer(Modifier.height(8.dp))
    SectionLabel(stringResource(R.string.language_label))
    Column(Modifier.selectableGroup()) {
        AppLanguage.entries.forEach { lang ->
            Row(
                Modifier.fillMaxWidth().heightIn(min = 56.dp).selectable(selected = lang == selected, role = Role.RadioButton, onClick = {
                    if (lang != selected) { selected = lang; AppLocale.apply(lang) } // recreates the Activity
                }),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = lang == selected, onClick = null)
                Text(languageLabel(lang), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 16.dp))
            }
        }
    }
    Text(stringResource(R.string.language_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    // Developer tool: only in debug builds, never in the APK she gets.
    val ctx = androidx.compose.ui.platform.LocalContext.current
    if ((ctx.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
        var showMoods by rememberSaveable { mutableStateOf(false) }
        OutlinedButton(onClick = { showMoods = true }) { Text(stringResource(R.string.mood_lab_button)) }
        if (showMoods) MoodLabSheet(onDismiss = { showMoods = false })
    }
}

@Composable
private fun PrivacyPage(headlines: HeadlinesViewModel, auth: AuthViewModel, studyByDate: Map<java.time.LocalDate, Int>) {
    val newsOn by headlines.enabled.collectAsStateWithLifecycle()
    val lastMs by headlines.lastRefreshMs.collectAsStateWithLifecycle()
    AccountSection(auth, showErase = false)
    Spacer(Modifier.height(8.dp))
    SectionLabel(stringResource(R.string.settings_news_title))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.settings_news_switch), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = newsOn, onCheckedChange = headlines::setEnabled)
    }
    Text(
        stringResource(R.string.settings_news_note) + if (lastMs > 0) "\n" + stringResource(
            R.string.settings_news_last,
            DateUtils.getRelativeTimeSpanString(lastMs, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
        ) else "",
        style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))
    SectionLabel(stringResource(R.string.section_insights))
    com.spcode.tnpscprep.ui.today.UsageCard(studyByDate, showTitle = false)
}

@Composable
private fun DataPage(auth: AuthViewModel) {
    Text(stringResource(R.string.syllabus_source_title), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.syllabus_source_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Spacer(Modifier.height(8.dp))
    AccountSection(auth, showSecurity = false)
}


@Composable
private fun SectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), color = MaterialTheme.colorScheme.primary)
}

/** Optional evening reminder: OFF by default, asks for the notification permission only when she switches it on. */
@Composable
private fun ReminderSection(settings: com.spcode.tnpscprep.data.SettingsStore) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val on by settings.reminderEnabled.collectAsStateWithLifecycle()
    val minutes by settings.reminderMinutes.collectAsStateWithLifecycle()
    var denied by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    var guide by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    fun apply(enabled: Boolean, m: Int) {
        settings.setReminderEnabled(enabled); settings.setReminderMinutes(m)
        com.spcode.tnpscprep.reminder.ReminderScheduler.sync(ctx.applicationContext, enabled, m)
    }
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) { denied = false; apply(true, minutes) } else denied = true
    }
    fun turnOn() {
        val needs = android.os.Build.VERSION.SDK_INT >= 33 &&
            androidx.core.content.ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED
        if (needs) ask.launch(android.Manifest.permission.POST_NOTIFICATIONS) else { denied = false; apply(true, minutes) }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.reminder_switch), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = on, onCheckedChange = { if (it) turnOn() else apply(false, minutes) })
    }
    if (on) Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.reminder_time), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { apply(true, minutes - com.spcode.tnpscprep.domain.ReminderTime.STEP) }) { Text("−") }
        Text(String.format("%02d:%02d", minutes / 60, minutes % 60), Modifier.padding(horizontal = 12.dp), style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { apply(true, minutes + com.spcode.tnpscprep.domain.ReminderTime.STEP) }) { Text("+") }
    }
    if (denied) {
        Text(stringResource(R.string.reminder_denied), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
        TextButton(onClick = {
            ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
        }) { Text(stringResource(R.string.reminder_open_settings)) }
    }
    Text(stringResource(R.string.reminder_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = { guide = true }) { Text(stringResource(R.string.reminder_guide_link)) }
    if (guide) com.spcode.tnpscprep.ui.design.PopSheet(onDismiss = { guide = false }) { dismiss ->
        Column(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.reminder_guide_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.reminder_guide_1)); Text(stringResource(R.string.reminder_guide_2))
            Text(stringResource(R.string.reminder_guide_3)); Text(stringResource(R.string.reminder_guide_4))
            OutlinedButton(onClick = {
                val i = android.content.Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { ctx.startActivity(i) }.onFailure {
                    ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:" + ctx.packageName)).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
                }
            }, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.reminder_open_battery)) }
            TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sheet_close)) }
        }
    }
}

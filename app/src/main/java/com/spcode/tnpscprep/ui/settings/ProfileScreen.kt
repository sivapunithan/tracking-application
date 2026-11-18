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
import com.spcode.tnpscprep.ui.guide.guideTarget
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Storage
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
    val pb = com.spcode.tnpscprep.ui.guide.rememberGuideBounds()
    val profileScroll = rememberScrollState()
    androidx.compose.foundation.layout.Box(Modifier.fillMaxSize()) {
    Column(
        Modifier.fillMaxSize().verticalScroll(profileScroll)
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
            else -> Hub(headlines, settings, pb) { page = it }
        }
    }
    com.spcode.tnpscprep.ui.guide.PageTour(
        "profile",
        listOf(
            com.spcode.tnpscprep.ui.guide.CoachStep("top", stringResource(R.string.guide_p1_title), stringResource(R.string.guide_p1_body)),
            com.spcode.tnpscprep.ui.guide.CoachStep("rows", stringResource(R.string.guide_p2_title), stringResource(R.string.guide_p2_body)),
            com.spcode.tnpscprep.ui.guide.CoachStep("data", stringResource(R.string.guide_p3_title), stringResource(R.string.guide_p3_body)),
        ),
        pb, scrollable = profileScroll, enabled = page == "hub",
    )
    }
}

@Composable
private fun Hub(headlines: HeadlinesViewModel, settings: com.spcode.tnpscprep.data.SettingsStore, pb: MutableMap<String, androidx.compose.ui.geometry.Rect>, open: (String) -> Unit) {
    val name by headlines.userName.collectAsStateWithLifecycle()
    val mask by settings.examMask.collectAsStateWithLifecycle()
    val goal by settings.dailyGoalMinutes.collectAsStateWithLifecycle()
    val wrapHour by settings.wrapupHour.collectAsStateWithLifecycle()
    val reminderOn by settings.reminderEnabled.collectAsStateWithLifecycle()
    val strict by settings.strictFood.collectAsStateWithLifecycle()
    val themeMode by headlines.themeMode.collectAsStateWithLifecycle()
    val exams = ExamSelection.codes(mask).joinToString(" · ")
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)), modifier = Modifier.fillMaxWidth().guideTarget(pb, "top")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp).background(MaterialTheme.colorScheme.primaryContainer, CircleShape), contentAlignment = Alignment.Center) {
                    // the thumbs-up picture when it is on this phone, else the initial
                    if (com.spcode.tnpscprep.ui.mood.hasMoodImage(androidx.compose.ui.platform.LocalContext.current, com.spcode.tnpscprep.domain.Mood.PROUD)) {
                        Box(Modifier.size(64.dp).clip(CircleShape)) { com.spcode.tnpscprep.ui.diary.DiaryMood("PROUD", 64.dp) }
                    } else Text(name.trim().take(1).uppercase().ifEmpty { "•" }, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                }
                Column(Modifier.padding(start = 14.dp)) {
                    Text(name.ifBlank { stringResource(R.string.profile_title) }, style = MaterialTheme.typography.titleLarge)
                    Text(exams.ifEmpty { stringResource(R.string.exam_all) }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(
                value = name, onValueChange = headlines::setUserName, singleLine = true, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.profile_name_label)) },
            )
        }
    }
    val on = stringResource(R.string.state_on); val off = stringResource(R.string.state_off)
    Column(Modifier.guideTarget(pb, "rows"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    HubRow(Icons.Rounded.School, stringResource(R.string.profile_pg_study), stringResource(R.string.profile_sub_study, exams.ifEmpty { stringResource(R.string.exam_all) }, goal)) { open("study") }
    HubRow(Icons.Rounded.NightsStay, stringResource(R.string.profile_pg_evening), stringResource(R.string.profile_sub_evening, wrapHour, if (reminderOn) on else off)) { open("evening") }
    HubRow(Icons.Rounded.Restaurant, stringResource(R.string.profile_pg_food), stringResource(R.string.profile_sub_food, if (strict) on else off)) { open("food") }
    HubRow(Icons.Rounded.Palette, stringResource(R.string.profile_pg_look), stringResource(R.string.profile_sub_look, themeLabel(themeMode), languageLabel(AppLocale.current()))) { open("look") }
    HubRow(Icons.Rounded.Lock, stringResource(R.string.profile_pg_privacy), stringResource(R.string.profile_sub_privacy)) { open("privacy") }
    Box(Modifier.guideTarget(pb, "data")) { HubRow(Icons.Rounded.Storage, stringResource(R.string.profile_pg_data), stringResource(R.string.profile_sub_data)) { open("data") } }
    }
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
private fun HubRow(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, summary: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
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
    SoundsAndPraise()
    GuideAgain()
    WidgetSection()
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
    PhotoStorageSection()
    Spacer(Modifier.height(8.dp))
    BackupSection()
    Spacer(Modifier.height(8.dp))
    AccountSection(auth, showSecurity = false)
}

/** Full backup / restore through Android's file pickers (no permission). The result of a restore is all-or-nothing. */
@Composable
private fun BackupSection() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val container = (ctx.applicationContext as com.spcode.tnpscprep.TnpscApp).container
    val repo = container.backupRepository
    val last by container.settingsStore.lastBackupMs.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var notice by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf<String?>(null) }
    var busy by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val saved = stringResource(R.string.bk_saved); val saveFail = stringResource(R.string.bk_save_failed)
    val bad = stringResource(R.string.bk_bad); val restoredFmt = stringResource(R.string.bk_restored)
    var bundle by androidx.compose.runtime.saveable.rememberSaveable { androidx.compose.runtime.mutableStateOf(false) }
    val restoredPhotosFmt = stringResource(R.string.bk_restored_photos)
    val save = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) { busy = true; scope.launch {
            val ok = runCatching {
                if (bundle) withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri, "wt")!!.use { repo.exportBundle(it) } }
                else {
                    val text = repo.exportJson()
                    withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) } }
                }
            }.isSuccess
            if (ok) repo.markBackedUp()
            notice = if (ok) saved else saveFail; busy = false
        } }
    }
    val restore = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) { busy = true; scope.launch {
            val r = repo.restoreFromUri(ctx.contentResolver, uri).getOrNull()
            notice = if (r == null) bad else if (r.photos > 0) String.format(restoredPhotosFmt, r.added, r.updated, r.photos) else String.format(restoredFmt, r.added, r.updated)
            busy = false
        } }
    }
    val lastText = if (last <= 0L) stringResource(R.string.bk_never)
        else stringResource(R.string.bk_last, java.time.Instant.ofEpochMilli(last).atZone(java.time.ZoneId.systemDefault()).toLocalDate().format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)))
    Text(stringResource(R.string.bk_title), style = MaterialTheme.typography.titleMedium)
    Text(stringResource(R.string.bk_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Text(lastText, style = MaterialTheme.typography.bodyMedium)
    val remind by container.settingsStore.backupReminder.collectAsStateWithLifecycle()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.bk_remind_switch), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = remind, onCheckedChange = { container.settingsStore.setBackupReminder(it) })
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        androidx.compose.material3.Button(enabled = !busy, onClick = {
            scope.launch {
                // with photos it is ONE .mathi file (text + pictures); without, the plain text file as before
                bundle = repo.photoCount() > 0
                save.launch("mathi-backup-" + java.time.LocalDate.now() + if (bundle) ".mathi" else ".json")
            }
        }) { Text(stringResource(R.string.bk_save)) }
        androidx.compose.material3.OutlinedButton(enabled = !busy, onClick = { restore.launch(arrayOf("*/*")) }) { Text(stringResource(R.string.bk_restore)) }
    }
    if (busy) Text(stringResource(R.string.bk_busy), style = MaterialTheme.typography.bodySmall)
    notice?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary) }
}


/** Asks Android to place a widget through its own confirm dialog (no permission). The launcher may still say no: then she adds it by hand. */
@Composable
private fun WidgetSection() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    var msg by rememberSaveable { mutableStateOf<String?>(null) }
    val unsupported = stringResource(R.string.widget_pin_unsupported)
    val asked = stringResource(R.string.widget_pin_asked)
    Spacer(Modifier.height(8.dp))
    SectionLabel(stringResource(R.string.widget_section))
    Text(stringResource(R.string.widget_section_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { msg = if (com.spcode.tnpscprep.widget.WidgetUpdater.requestPin(ctx, com.spcode.tnpscprep.widget.TodayWidget::class.java)) asked else unsupported }) { Text(stringResource(R.string.widget_add_today)) }
        OutlinedButton(onClick = { msg = if (com.spcode.tnpscprep.widget.WidgetUpdater.requestPin(ctx, com.spcode.tnpscprep.widget.PlanWidget::class.java)) asked else unsupported }) { Text(stringResource(R.string.widget_add_plan)) }
    }
    msg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
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


/** Mood sounds + kind notifications. */
@Composable
private fun SoundsAndPraise() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val settings = remember { (ctx.applicationContext as com.spcode.tnpscprep.TnpscApp).container.settingsStore }
    val sounds by settings.moodSounds.collectAsStateWithLifecycle()
    val praise by settings.praiseNotifs.collectAsStateWithLifecycle()
    var allowed by remember { mutableStateOf(com.spcode.tnpscprep.notify.Praise.canPost(ctx)) }
    val ask = androidx.activity.compose.rememberLauncherForActivityResult(androidx.activity.result.contract.ActivityResultContracts.RequestPermission()) { allowed = com.spcode.tnpscprep.notify.Praise.canPost(ctx) }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.profile_sounds), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = sounds, onCheckedChange = settings::setMoodSounds)
    }
    Text(stringResource(R.string.profile_sounds_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (sounds) OutlinedButton(onClick = { com.spcode.tnpscprep.ui.mood.MoodSounds.play(ctx, "fail", force = true) }) { Text(stringResource(R.string.profile_sounds_test)) }
    Spacer(Modifier.height(12.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.profile_praise), style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = praise, onCheckedChange = { settings.setPraiseNotifs(it) })
    }
    Text(stringResource(R.string.profile_praise_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (praise && !allowed) OutlinedButton(onClick = {
        if (android.os.Build.VERSION.SDK_INT >= 33) ask.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        else ctx.startActivity(android.content.Intent(android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, ctx.packageName).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
    }) { Text(stringResource(R.string.profile_praise_allow)) }
}


/** How much space diary photos use, and a button that removes files nothing refers to any more. */
@Composable
private fun PhotoStorageSection() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val container = (ctx.applicationContext as com.spcode.tnpscprep.TnpscApp).container
    val dao = container.database.diaryPhotoDao()
    val count by dao.observeCount().collectAsStateWithLifecycle(0)
    val bytes by dao.observeTotalBytes().collectAsStateWithLifecycle(0L)
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var msg by remember { mutableStateOf<String?>(null) }
    val cleaned = stringResource(R.string.photos_cleaned)
    Text(stringResource(R.string.photos_title), style = MaterialTheme.typography.titleMedium)
    Text(
        if (count == 0) stringResource(R.string.photos_none) else stringResource(R.string.photos_used, count, if (bytes < 1_048_576L) "${maxOf(1L, bytes / 1024)} KB" else String.format("%.1f MB", bytes / 1_048_576.0)),
        style = MaterialTheme.typography.bodyMedium,
    )
    Text(stringResource(R.string.photos_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedButton(onClick = {
        scope.launch {
            val removed = withContext(Dispatchers.IO) { container.photoStore.deleteExcept(container.diaryRepository.sweepPhotoRows()) }
            msg = String.format(cleaned, removed)
        }
    }) { Text(stringResource(R.string.photos_clean)) }
    msg?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
}


/** Plays every first-time tour again (Today, Study, Care, Focus, Profile and the Journal ones). */
@Composable
private fun GuideAgain() {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val settings = remember { (ctx.applicationContext as com.spcode.tnpscprep.TnpscApp).container.settingsStore }
    var done by remember { mutableStateOf(false) }
    Spacer(Modifier.height(12.dp))
    OutlinedButton(onClick = { settings.resetGuides(); done = true }) { Text(stringResource(R.string.guide_again)) }
    if (done) Text(stringResource(R.string.guide_again_done), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
}

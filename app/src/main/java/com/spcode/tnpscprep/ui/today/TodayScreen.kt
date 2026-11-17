package com.spcode.tnpscprep.ui.today

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.initializer
import com.spcode.tnpscprep.R
import com.spcode.tnpscprep.domain.MoodEngine
import com.spcode.tnpscprep.domain.MoodInput
import com.spcode.tnpscprep.domain.RevisionLevel
import com.spcode.tnpscprep.domain.RevisionSchedule
import com.spcode.tnpscprep.ui.affairs.categoryLabelRes
import com.spcode.tnpscprep.ui.design.AnimatedProgressBar
import com.spcode.tnpscprep.ui.design.LevelTrack
import com.spcode.tnpscprep.ui.design.LocalBottomBarInset
import com.spcode.tnpscprep.ui.design.PopIn
import com.spcode.tnpscprep.ui.design.PopSheet
import com.spcode.tnpscprep.ui.design.ProgressRing
import com.spcode.tnpscprep.ui.design.SoftCard
import com.spcode.tnpscprep.ui.design.animatedInt
import com.spcode.tnpscprep.ui.design.pressScale
import com.spcode.tnpscprep.ui.localized
import com.spcode.tnpscprep.ui.mood.MoodAvatar
import com.spcode.tnpscprep.ui.mood.hasMoodImage
import com.spcode.tnpscprep.ui.theme.LocalIsDark
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.collectAsState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodayScreen(vm: TodayViewModel, onOpenProfile: () -> Unit, onOpenDiary: (String) -> Unit, onOpenFocus: () -> Unit, onOpenStudy: () -> Unit, onOpenWrapup: () -> Unit) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val name by vm.userName.collectAsStateWithLifecycle()
    val heat by vm.heat.collectAsStateWithLifecycle()
    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.spcode.tnpscprep.TnpscApp
    val fvm = androidx.lifecycle.viewmodel.compose.viewModel<FoodViewModel>(
        factory = androidx.lifecycle.viewmodel.viewModelFactory { initializer { FoodViewModel(app.container.foodRepository, app.container.settingsStore) } },
    )
    val food by fvm.days.collectAsStateWithLifecycle()
    val strict by fvm.strict.collectAsStateWithLifecycle()
    val lastAlert by fvm.lastAlertStart.collectAsStateWithLifecycle()
    // The food day changes at 3 AM; re-check every minute so an app left open rolls over by itself.
    val foodDay by androidx.compose.runtime.produceState(com.spcode.tnpscprep.domain.FoodCheckin.foodDay(System.currentTimeMillis(), java.time.ZoneId.systemDefault())) {
        while (true) { value = com.spcode.tnpscprep.domain.FoodCheckin.foodDay(System.currentTimeMillis(), java.time.ZoneId.systemDefault()); kotlinx.coroutines.delay(60_000) }
    }
    val appCtx = androidx.compose.ui.platform.LocalContext.current.applicationContext
    val stepSensor = remember { hasStepSensor(appCtx) }
    val extras by vm.extras.collectAsStateWithLifecycle()
    val hvm = androidx.lifecycle.viewmodel.compose.viewModel<HealthViewModel>(
        factory = androidx.lifecycle.viewmodel.viewModelFactory { initializer { HealthViewModel(app.container.healthRepository, app.container.settingsStore) } },
    )
    val healthEntries by hvm.entries.collectAsStateWithLifecycle()
    val stepsVm = androidx.lifecycle.viewmodel.compose.viewModel<StepsViewModel>(
        factory = androidx.lifecycle.viewmodel.viewModelFactory { initializer { StepsViewModel(app.container.stepRepository) } },
    )
    val stepsWeek by stepsVm.week.collectAsStateWithLifecycle()
    val settings = app.container.settingsStore
    val wvm = androidx.lifecycle.viewmodel.compose.viewModel<com.spcode.tnpscprep.ui.wrapup.WrapupStatusViewModel>(
        factory = androidx.lifecycle.viewmodel.viewModelFactory { initializer { com.spcode.tnpscprep.ui.wrapup.WrapupStatusViewModel(app.container.diaryRepository, app.container.plannedTaskRepository) } },
    )
    LaunchedEffect(foodDay) { wvm.setDay(foodDay) }
    val wrapped by wvm.wrappedToday.collectAsStateWithLifecycle()
    val lastSeven by wvm.lastSeven.collectAsStateWithLifecycle()
    val plannedToday by wvm.todayTasks.collectAsStateWithLifecycle()
    val plannedTomorrow by wvm.tomorrowTasks.collectAsStateWithLifecycle()
    val wrapHour by settings.wrapupHour.collectAsStateWithLifecycle()
    val gsDone by settings.gettingStartedDone.collectAsStateWithLifecycle()
    val diaryItems by remember { app.container.diaryRepository.observe("") }.collectAsState(emptyList())
    StepsAutoRead() // steps are counted when the app comes to the front, whichever screen she is on
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    // Which pop-up is open: null, "date", "due:<id>" or "star:<id>" (saveable, so rotation keeps it).
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }

    // ---- what the guide cards need ----
    val today = java.time.LocalDate.now()
    val hour = LocalTime.now().hour
    val foodLogged = food[foodDay] != null
    val action = com.spcode.tnpscprep.domain.TodayGuide.nextAction(
        com.spcode.tnpscprep.domain.NextActionInput(ui.examDate != null, ui.progress.due.size, ui.todayMinutes, hour, foodLogged, extras.nextTopic != null,
            plannedToday.firstOrNull { !it.done && (it.refId == null || it.refId !in extras.doneRefs) }?.let { com.spcode.tnpscprep.domain.PlannedTaskRef(it.id, it.title) }),
    )
    val gsItems = com.spcode.tnpscprep.domain.GettingStarted.items(ui.examDate != null, extras.hasAnySession, food.isNotEmpty(), diaryItems.isNotEmpty())
    val gsComplete = com.spcode.tnpscprep.domain.GettingStarted.isComplete(gsItems)
    LaunchedEffect(gsComplete) { if (gsComplete && !gsDone) settings.setGettingStartedDone(true) } // once done, never shown again
    val showGs = !gsDone && !gsComplete
    // ---- today's plan: built ONCE per day from existing data, then stored (so it is finite and a ticked line stays) ----
    val dueNow = ui.progress.due
    val nextTopicName = extras.nextTopic?.let { localized(it.nameEn, it.nameTa) }
    val affairName = extras.affairToReview?.let { localized(it.titleEn, it.titleTa) }
    val names = HashMap<String, String>().apply {
        dueNow.forEach { put(it.topicId, localized(it.nameEn, it.nameTa)) }
        extras.nextTopic?.let { t -> nextTopicName?.let { put(t.id, it) } }
        extras.affairToReview?.let { a -> affairName?.let { put(a.id, it) } }
    }
    val picks = com.spcode.tnpscprep.domain.PlanBuilder.build(dueNow.map { com.spcode.tnpscprep.domain.DuePick(it.topicId, it.affairCategory != null) }, extras.nextTopic?.id, extras.affairToReview?.id)
    val planDraft = picks.mapNotNull { p ->
        names[p.refId]?.let { n ->
            com.spcode.tnpscprep.data.PlanDraftItem(n, when (p.kind) {
                com.spcode.tnpscprep.domain.PlanKind.REVISION -> com.spcode.tnpscprep.data.PlanRefType.REVISION
                com.spcode.tnpscprep.domain.PlanKind.READ -> com.spcode.tnpscprep.data.PlanRefType.TOPIC
                com.spcode.tnpscprep.domain.PlanKind.NEWS -> com.spcode.tnpscprep.data.PlanRefType.CURRENT_AFFAIRS
            }, p.refId)
        }
    }
    // wait until the data has settled (several flows feed it), then store; a no-op when the day already has a plan
    LaunchedEffect(foodDay, planDraft.map { it.refId }) { delay(900); if (planDraft.isNotEmpty()) wvm.generate(foodDay, planDraft) }
    val tickPlan: (com.spcode.tnpscprep.data.db.PlannedTaskEntity) -> Unit = { t ->
        wvm.setDone(t.id, true)
        t.refId?.let { id -> when (t.refType) { "TOPIC", "REVISION" -> vm.readTopic(id); "CURRENT_AFFAIRS" -> vm.reviseAffair(id) } }
    }
    val waterToday = healthEntries.filter { it.kind == com.spcode.tnpscprep.data.HealthKind.WATER && it.date == today.toString() }.sumOf { it.value }.toInt()
    val stepsToday = if (stepSensor && stepsAllowed(appCtx)) stepsWeek.firstOrNull { it.date == today.toString() }?.steps ?: 0 else null
    val wrapState = com.spcode.tnpscprep.domain.WrapupCard.state(hour, wrapHour, wrapped)
    val checkinsIndex = 2 + (if (showGs) 1 else 0) + (if (wrapState != com.spcode.tnpscprep.domain.WrapupCardState.HIDDEN) 1 else 0) + (if (plannedToday.isNotEmpty()) 1 else 0)
    fun toCheckins() { scope.launch { listState.animateScrollToItem(checkinsIndex) } }

    LazyColumn(
        Modifier.fillMaxSize(),
        state = listState,
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp + LocalBottomBarInset.current),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item { Header(name, ui, onOpenProfile) }

        item {
            NextActionCard(action, ui.progress.due.firstOrNull(), extras.nextTopic?.let { localized(it.nameEn, it.nameTa) }) {
                when (action) {
                    com.spcode.tnpscprep.domain.NextAction.SetExamDate -> sheet = "date"
                    is com.spcode.tnpscprep.domain.NextAction.Revise -> ui.progress.due.firstOrNull()?.let { sheet = "due:" + it.topicId }
                    com.spcode.tnpscprep.domain.NextAction.StartFocus -> onOpenFocus()
                    com.spcode.tnpscprep.domain.NextAction.LogFood -> toCheckins()
                    is com.spcode.tnpscprep.domain.NextAction.PlannedTask -> plannedToday.firstOrNull { it.id == action.taskId }?.let(tickPlan) ?: Unit
                    com.spcode.tnpscprep.domain.NextAction.NewTopic -> onOpenStudy()
                    com.spcode.tnpscprep.domain.NextAction.AllCaughtUp -> Unit
                }
            }
        }

        if (showGs) item {
            GettingStartedCard(gsItems) { step ->
                when (step) {
                    com.spcode.tnpscprep.domain.GettingStartedStep.EXAM_DATE -> sheet = "date"
                    com.spcode.tnpscprep.domain.GettingStartedStep.FIRST_FOCUS -> onOpenFocus()
                    com.spcode.tnpscprep.domain.GettingStartedStep.FIRST_FOOD -> toCheckins()
                    com.spcode.tnpscprep.domain.GettingStartedStep.FIRST_DIARY -> onOpenDiary("new:$foodDay")
                }
            }
        }

        if (wrapState != com.spcode.tnpscprep.domain.WrapupCardState.HIDDEN) item(key = "wrapup") { com.spcode.tnpscprep.ui.wrapup.WrapupCardView(wrapState, onOpenWrapup, lastSeven, plannedTomorrow.map { it.title }.take(3)) }

        if (plannedToday.isNotEmpty()) item(key = "plan") { PlanCard(plannedToday, extras.doneRefs, tickPlan) }

        item(key = "checkins") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SectionTitle(stringResource(R.string.checkin_title))
                val evening = com.spcode.tnpscprep.domain.FoodCheckin.eveningPrompt(System.currentTimeMillis(), java.time.ZoneId.systemDefault(), foodLogged)
                FoodSection(foodDay, food, evening, onSet = fvm::set, onDay = { sheet = "day:" + it })
                WaterRow(waterToday) { hvm.add(com.spcode.tnpscprep.data.HealthKind.WATER, 250.0) }
            }
        }

        item {
            // last 7 days of study minutes (oldest first) for the study tile's mini chart, from the same heatmap data
            val week = remember(heat, foodDay) {
                val byDate = heat.weeks.flatten().filterNotNull().associate { it.date to it.minutes }
                (6 downTo 0).map { byDate[foodDay.minusDays(it.toLong())] ?: 0 }
            }
            TileRow(ui, stepsToday, week)
        }

        item { HeatmapSection(heat, food, onDay = { sheet = "day:" + it }) }
    }

    // Strict mode: shown ONCE per chain of 3 junk days (the chain's start date is remembered), and only when nothing else is open.
    val chain = com.spcode.tnpscprep.domain.FoodCheckin.junkChain(food, foodDay)
    LaunchedEffect(chain, strict, sheet) {
        val start = chain.start
        if (sheet == null && start != null && com.spcode.tnpscprep.domain.FoodCheckin.shouldAlert(chain, strict, lastAlert.takeIf { it.isNotEmpty() }?.let { java.time.LocalDate.parse(it) })) {
            fvm.markAlerted(start); sheet = "foodalert"
        }
    }

    // ---------------- pop-ups ----------------
    val current = sheet
    if (current != null) {
        PopSheet(onDismiss = { sheet = null }) { dismiss ->
            when {
                current == "date" -> ExamDateSheet(ui, onSave = { vm.setExamDate(it); dismiss() }, onClear = { vm.clearExamDate(); dismiss() }, onCancel = dismiss)
                current == "foodalert" -> FoodAlertSheet(dismiss)
                current == "health" -> {
                    val app = androidx.compose.ui.platform.LocalContext.current.applicationContext as com.spcode.tnpscprep.TnpscApp
                    val hvm = androidx.lifecycle.viewmodel.compose.viewModel<HealthViewModel>(
                        factory = androidx.lifecycle.viewmodel.viewModelFactory {
                            initializer { HealthViewModel(app.container.healthRepository, app.container.settingsStore) }
                        },
                    )
                    HealthSheet(hvm, dismiss)
                }
                current.startsWith("day:") -> {
                    val date = runCatching { java.time.LocalDate.parse(current.removePrefix("day:")) }.getOrNull()
                    if (date == null) LaunchedEffect(Unit) { dismiss() } else DaySheet(vm, date, dismiss, onDiary = { sheet = null; onOpenDiary(it) })
                }
                current.startsWith("due:") -> {
                    val item = ui.progress.due.firstOrNull { it.topicId == current.removePrefix("due:") }
                    if (item == null) LaunchedEffect(Unit) { dismiss() } else DueSheet(item, onRevised = { vm.markRevised(item) }, dismiss = dismiss)
                }
                current.startsWith("star:") -> {
                    val item = ui.progress.starred.firstOrNull { it.topicId == current.removePrefix("star:") }
                    if (item == null) LaunchedEffect(Unit) { dismiss() } else StarSheet(item, onUnstar = { vm.toggleStar(item.topicId); dismiss() }, dismiss = dismiss)
                }
            }
        }
    }
}

@Composable
private fun Header(name: String, ui: TodayUiState, onProfile: () -> Unit) {
    val hour = remember { LocalTime.now().hour }
    val greeting = stringResource(
        when {
            hour < 12 -> R.string.greet_morning
            hour < 17 -> R.string.greet_afternoon
            else -> R.string.greet_evening
        },
    )
    // The avatar pops in with a springy overshoot.
    val avatarScale by animateFloatAsState(1f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow), label = "avatar")
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { delay(80); started = true }
    val scale by animateFloatAsState(if (started) avatarScale else 0.5f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow), label = "avatarIn")

    val profileDesc = stringResource(R.string.profile_open)
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(greeting, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (name.isBlank()) stringResource(R.string.welcome_back) else stringResource(R.string.hello_name, com.spcode.tnpscprep.ui.properName(name)),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
            )
            Text(
                stringResource(R.string.today_tagline),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier
                .size(68.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.secondaryContainer)))
                .semantics { contentDescription = profileDesc }
                .clickable(role = androidx.compose.ui.semantics.Role.Button, onClick = onProfile),
        ) {
            val ctx = androidx.compose.ui.platform.LocalContext.current
            val mood = remember(ui.streak, ui.todayMinutes, ui.goalMinutes, ui.daysSinceStudy, ui.progress.due.size, hour) {
                MoodEngine.pick(MoodInput(ui.streak, ui.todayMinutes >= ui.goalMinutes, ui.daysSinceStudy, ui.progress.due.size, hour))
            }
            // The avatar follows her streak and revision, but only once she has studied something and only when THAT mood's own
            // picture exists; otherwise (new user, or picture not made yet) a plain initial badge.
            if (ui.daysSinceStudy != null && hasMoodImage(ctx, mood)) {
                MoodAvatar(mood, size = 68.dp)
            } else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                if (name.isBlank()) {
                    Icon(Icons.Rounded.School, null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(34.dp))
                } else {
                    Text(
                        name.first().uppercase(),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }
        }

    }
}



@Composable
internal fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 6.dp))
}

@Composable
internal fun EmptyHint(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

// ---------------------------------------------------------------- pop-up contents

@Composable
internal fun DueSheet(item: DueItem, onRevised: () -> Unit, dismiss: () -> Unit) {
    var done by remember { mutableStateOf(false) }
    val levelNames = stringArrayResource(R.array.revision_levels)
    val nextLevel = (item.level + 1).coerceAtMost(RevisionLevel.MAX)
    val nextDays = RevisionSchedule.intervalDays(nextLevel) ?: 0
    val tick by animateFloatAsState(if (done) 1f else 0f, spring(dampingRatio = 0.35f, stiffness = Spring.StiffnessMedium), label = "tick")
    LaunchedEffect(done) { if (done) { delay(750); dismiss() } }

    if (done) {
        Box(Modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.CheckCircle, null, tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(96.dp).graphicsLayer { scaleX = tick; scaleY = tick },
            )
        }
        return
    }
    Text(localized(item.nameEn, item.nameTa), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
    Text(
        if (item.affairCategory != null) stringResource(R.string.tab_affairs) + "  ·  " + stringResource(categoryLabelRes(item.affairCategory))
        else localized(item.subjectEn, item.subjectTa),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
    Spacer(Modifier.height(20.dp))
    LevelTrack(item.level, Modifier.fillMaxWidth())
    Text(
        levelNames[RevisionLevel.clamp(item.level)],
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp),
    )
    Text(
        if (item.daysOverdue == 0) stringResource(R.string.due_today)
        else pluralStringResource(R.plurals.overdue_days, item.daysOverdue, item.daysOverdue),
        style = MaterialTheme.typography.bodyMedium,
        color = if (item.daysOverdue > 0) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 2.dp),
    )
    Text(
        pluralStringResource(R.plurals.sheet_next_review, nextDays, nextDays),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp),
    )
    Spacer(Modifier.height(20.dp))
    Button(onClick = { onRevised(); done = true }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
        Text(stringResource(R.string.revised), style = MaterialTheme.typography.titleMedium)
    }
    TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sheet_close)) }
}

@Composable
internal fun StarSheet(item: StarredItem, onUnstar: () -> Unit, dismiss: () -> Unit) {
    val levelNames = stringArrayResource(R.array.revision_levels)
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Rounded.Star, null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(30.dp))
        Spacer(Modifier.width(10.dp))
        Text(localized(item.nameEn, item.nameTa), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
    }
    Text(
        localized(item.subjectEn, item.subjectTa),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 4.dp),
    )
    Spacer(Modifier.height(18.dp))
    LevelTrack(item.level, Modifier.fillMaxWidth())
    Text(
        levelNames[RevisionLevel.clamp(item.level)],
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 10.dp),
    )
    Spacer(Modifier.height(16.dp))
    SoftCard(Modifier.fillMaxWidth()) {
        Text(
            if (item.note.isBlank()) stringResource(R.string.sheet_no_note) else item.note,
            style = MaterialTheme.typography.bodyMedium,
            color = if (item.note.isBlank()) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(16.dp),
        )
    }
    Spacer(Modifier.height(20.dp))
    OutlinedButton(onClick = onUnstar, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text(stringResource(R.string.sheet_remove_star)) }
    TextButton(onClick = dismiss, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.sheet_close)) }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExamDateSheet(ui: TodayUiState, onSave: (Long) -> Unit, onClear: () -> Unit, onCancel: () -> Unit) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = ui.examDate?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
    )
    Text(stringResource(R.string.set_exam_date), style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
    ui.examDate?.let {
        Text(
            it.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    DatePicker(state = state, showModeToggle = false, title = null, headline = null)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
        if (ui.examDate != null) OutlinedButton(onClick = onClear, modifier = Modifier.weight(1f).height(52.dp)) { Text(stringResource(R.string.clear)) }
        Button(
            onClick = {
                // The picker works in UTC midnight; convert back to a plain calendar date.
                state.selectedDateMillis?.let { onSave(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()) } ?: onCancel()
            },
            modifier = Modifier.weight(1f).height(52.dp),
        ) { Text(stringResource(R.string.ok)) }
    }
    TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.cancel)) }
}

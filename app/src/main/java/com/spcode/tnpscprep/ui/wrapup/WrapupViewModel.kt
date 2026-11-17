package com.spcode.tnpscprep.ui.wrapup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spcode.tnpscprep.data.ActivityRepository
import com.spcode.tnpscprep.data.DiaryRepository
import com.spcode.tnpscprep.data.PlanDraftItem
import com.spcode.tnpscprep.data.PlanRefType
import com.spcode.tnpscprep.data.PlannedTaskRepository
import com.spcode.tnpscprep.data.SettingsStore
import com.spcode.tnpscprep.data.SyllabusRepository
import com.spcode.tnpscprep.data.db.PlannedTaskEntity
import com.spcode.tnpscprep.domain.DayRecap
import com.spcode.tnpscprep.domain.Exam
import com.spcode.tnpscprep.domain.RecapBuilder
import com.spcode.tnpscprep.domain.Wrapup
import com.spcode.tnpscprep.domain.WrapupContent
import com.spcode.tnpscprep.domain.WrapupDraft
import com.spcode.tnpscprep.domain.WrapupDrafts
import com.spcode.tnpscprep.domain.WrapupHeadings
import com.spcode.tnpscprep.domain.PlanOrder
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything the wrap-up pager edits. [step]: 0 recap, 1 mood, 2 well, 3 hard, 4 tomorrow, 5 finish. */
data class WrapupState(
    val loaded: Boolean = false,
    val day: LocalDate = LocalDate.now(),
    val recap: DayRecap? = null,
    val step: Int = 0,
    val mood: String? = null,
    val intensity: Int = 0,
    val well: String = "",
    val hard: String = "",
    val tomorrow: List<PlanDraftItem> = emptyList(),
    /** True when today's wrap-up already exists (we are editing it). */
    val editing: Boolean = false,
    val savedSomething: Boolean = false,
    /** What was loaded (saved wrap-up or draft): leaving only asks "save draft?" when she changed something since. */
    val baseContent: WrapupContent = WrapupContent(),
    val baseMood: String? = null,
) {
    val dirty get() = content != baseContent || mood != baseMood
    val content get() = WrapupContent(well = well, hard = hard, intensity = intensity, tomorrow = tomorrow.map { it.title })
    /** Did she write or choose anything (so leaving should offer to keep the draft)? */
    val hasContent get() = mood != null || !Wrapup.isBlank(content)
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class WrapupViewModel(
    private val activity: ActivityRepository,
    private val syllabus: SyllabusRepository,
    private val diary: DiaryRepository,
    private val planned: PlannedTaskRepository,
    private val settings: SettingsStore,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {
    private val _state = MutableStateFlow(WrapupState())
    val state: StateFlow<WrapupState> = _state.asStateFlow()
    private var finished = false

    init {
        // Draft autosave: every change (after a short pause) is kept outside the diary, so nothing is lost if she leaves midway.
        viewModelScope.launch {
            _state.drop(1).debounce(400).collect { s -> if (s.loaded && !finished) saveDraft(s) }
        }
    }

    private fun saveDraft(s: WrapupState) {
        settings.setWrapupDraft(if (s.hasContent) WrapupDrafts.encode(WrapupDraft(s.day.toString(), s.step, s.mood, s.content)) else null)
    }

    /** Loads the recap and either today's saved wrap-up (to edit) or an unfinished draft of today. */
    fun load(day: LocalDate) {
        if (_state.value.loaded && _state.value.day == day) return
        viewModelScope.launch {
            val tamil = Locale.getDefault().language == "ta"
            val names = syllabus.observeTopics(Exam.ALL_MASK).first().associate { it.id to (if (tamil && it.nameTa.isNotBlank()) it.nameTa else it.nameEn) }
            val recap = RecapBuilder.build(day, activity.recapEvents(day), settings.dailyGoalMinutes.value, names, zone)
            val saved = diary.wrapupFor(day)
            val content = Wrapup.decode(saved?.wrapup)
            if (saved != null && content != null) {
                val tomorrowTasks = planned.get(day.plusDays(1)).map { PlanDraftItem(it.title, runCatching { PlanRefType.valueOf(it.refType) }.getOrDefault(PlanRefType.CUSTOM), it.refId) }
                _state.value = WrapupState(true, day, recap, 0, saved.moodEmojiId, content.intensity, content.well, content.hard,
                    tomorrowTasks.ifEmpty { content.tomorrow.map { PlanDraftItem(it, PlanRefType.CUSTOM, null) } }, editing = true).let { it.copy(baseContent = it.content, baseMood = it.mood) }
            } else {
                val d = WrapupDrafts.decodeFor(day, settings.wrapupDraft())
                _state.value = if (d != null) WrapupState(true, day, recap, d.step.coerceIn(0, 4), d.mood, d.content.intensity, d.content.well, d.content.hard,
                    d.content.tomorrow.map { PlanDraftItem(it, PlanRefType.CUSTOM, null) })
                else WrapupState(true, day, recap)
            }
        }
    }

    fun go(step: Int) = _state.update { it.copy(step = step.coerceIn(0, 5)) }
    fun setMood(m: String?) = _state.update { it.copy(mood = m) }
    fun setIntensity(i: Int) = _state.update { it.copy(intensity = if (it.intensity == i) 0 else i) }
    fun setWell(t: String) = _state.update { it.copy(well = t.take(2000)) }
    fun setHard(t: String) = _state.update { it.copy(hard = t.take(2000)) }

    fun addTomorrow(item: PlanDraftItem) = _state.update {
        if (item.title.isBlank() || it.tomorrow.size >= MAX_TOMORROW || it.tomorrow.any { x -> x.title == item.title || (item.refId != null && x.refId == item.refId && x.refType == item.refType) }) it
        else it.copy(tomorrow = it.tomorrow + item.copy(title = item.title.trim().take(120)))
    }
    fun removeTomorrow(i: Int) = _state.update { it.copy(tomorrow = it.tomorrow.filterIndexed { n, _ -> n != i }) }
    fun moveTomorrow(from: Int, to: Int) = _state.update { it.copy(tomorrow = PlanOrder.move(it.tomorrow, from, to)) }

    /** Saves the wrap-up (diary entry + tomorrow's plan) and clears the draft. Writes nothing if she chose and wrote nothing. */
    fun finish(headings: WrapupHeadings, then: () -> Unit) {
        val s = _state.value
        viewModelScope.launch {
            finished = true
            if (s.hasContent) {
                diary.saveWrapup(s.day, s.content, s.mood, headings)
                planned.replaceFor(s.day.plusDays(1), s.tomorrow)
            }
            settings.setWrapupDraft(null)
            _state.update { it.copy(savedSomething = s.hasContent) }
            then()
        }
    }

    fun discardDraft() { finished = true; settings.setWrapupDraft(null) }

    companion object { const val MAX_TOMORROW = 3 }
}

/** Small read-side holder for Today and Journal: is today's wrap-up done, and the planned tasks for today / tomorrow. */
@OptIn(ExperimentalCoroutinesApi::class)
class WrapupStatusViewModel(private val diary: DiaryRepository, private val planned: PlannedTaskRepository) : ViewModel() {
    private val day = MutableStateFlow(LocalDate.now())
    fun setDay(d: LocalDate) { day.value = d }

    val wrappedToday: StateFlow<Boolean> = combine(day, diary.observeWrapupDates(LocalDate.now().minusDays(400))) { d, dates -> d.toString() in dates }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)
    /** "Wrapped up N of the last 7 evenings" (a plain count; days without one are just not counted). */
    val lastSeven: StateFlow<Int> = combine(day, diary.observeWrapupDates(LocalDate.now().minusDays(400))) { d, dates ->
        com.spcode.tnpscprep.domain.WrapupConsistency.lastDays(dates.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet(), d)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)
    val todayTasks: StateFlow<List<PlannedTaskEntity>> = day.flatMapLatest { planned.observe(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tomorrowTasks: StateFlow<List<PlannedTaskEntity>> = day.flatMapLatest { planned.observe(it.plusDays(1)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setDone(id: Long, done: Boolean) { viewModelScope.launch { planned.setDone(id, done) } }
    /** Stores today's plan once (does nothing if the day already has one, e.g. from last night's wrap-up). */
    fun generate(day: LocalDate, items: List<PlanDraftItem>) { viewModelScope.launch { planned.generateIfEmpty(day, items) } }
}

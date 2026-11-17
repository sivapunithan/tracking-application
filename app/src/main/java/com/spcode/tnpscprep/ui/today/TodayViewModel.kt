package com.spcode.tnpscprep.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spcode.tnpscprep.data.ActivityRepository
import com.spcode.tnpscprep.data.AffairRepository
import com.spcode.tnpscprep.data.db.TopicWithLevel
import com.spcode.tnpscprep.domain.ActivityType
import com.spcode.tnpscprep.domain.DaySummary
import com.spcode.tnpscprep.ui.affairs.AffairUi
import com.spcode.tnpscprep.domain.Heatmap
import com.spcode.tnpscprep.data.NewsRepository
import com.spcode.tnpscprep.data.SessionRepository
import com.spcode.tnpscprep.data.SettingsStore
import com.spcode.tnpscprep.data.SyllabusRepository
import com.spcode.tnpscprep.ui.affairs.AffairFilter
import com.spcode.tnpscprep.ui.affairs.buildAffairList
import com.spcode.tnpscprep.ui.affairs.newsToUi
import com.spcode.tnpscprep.ui.timer.buildDayStats
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TodayUiState(
    val examMask: Int = com.spcode.tnpscprep.domain.Exam.ALL_MASK,
    val examDate: LocalDate? = null,
    val daysToExam: Long? = null,
    val progress: TodayProgress = TodayProgress(0, 0, 0, emptyList()),
    val todayMinutes: Int = 0,
    val goalMinutes: Int = 60,
    val streak: Int = 0,
    val daysSinceStudy: Int? = null,
)

/** Extra facts for the Today guide cards: derived from existing data only. */
data class TodayExtras(
    val hasAnySession: Boolean = false,
    /** Revisions / first reads done today, counted from the activity log. */
    val doneToday: Int = 0,
    /** The next topic she has not started (first in syllabus order for the chosen exams), or null. */
    val nextTopic: TopicWithLevel? = null,
    /** The newest curated current-affairs item she has not revised yet, or null. */
    val affairToReview: AffairUi? = null,
)

const val HEAT_WEEKS = 26
const val YEAR_WEEKS = 53

data class HeatState(val weeks: List<List<DaySummary?>> = emptyList(), val noteDates: Set<LocalDate> = emptySet())

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repo: SyllabusRepository,
    private val affairs: AffairRepository,
    news: NewsRepository,
    sessions: SessionRepository,
    private val activity: ActivityRepository,
    private val settings: SettingsStore,
    today: () -> LocalDate = LocalDate::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    // Re-emits when the date changes (app left open past midnight) so "due" stays correct.
    private val dayFlow = flow { while (true) { emit(today()); delay(60_000) } }.distinctUntilChanged()

    private val target = settings.examMask

    private val progress = target.flatMapLatest { f ->
        combine(
            repo.observeStudied(f), repo.observeTopics(f), repo.observeSubjects(),
            repo.observeStarred(f), dayFlow,
        ) { studied, all, subjects, starred, day -> buildTodayProgress(studied, all, subjects, day, zone, starred) }
    }

    private val affairDue = combine(
        combine(affairs.observePack(), affairs.observeUser(), affairs.observeStates(), ::Triple),
        news.observeRevised(), dayFlow,
    ) { (pack, user, states), revisedNews, day ->
        val stateById = states.associateBy { it.itemId }
        val newsUi = revisedNews.map { newsToUi(it, stateById["news-" + it.id], zone) }
        buildAffairDue(buildAffairList(pack, user, states, AffairFilter()).items + newsUi, day, zone)
    }

    private val stats = combine(sessions.observeDayTotals(), dayFlow, settings.dailyGoalMinutes) { totals, day, goal ->
        Pair(buildDayStats(totals, goal, day), goal)
    }

    /** Heatmap: last [HEAT_WEEKS] weeks, derived from activity_event (never from stored daily totals). */
    val heat: StateFlow<HeatState> = dayFlow.flatMapLatest { day ->
        combine(
            activity.observeHeatEvents(Heatmap.firstDate(day, HEAT_WEEKS)), settings.dailyGoalMinutes,
            activity.observeNoteDates(Heatmap.firstDate(day, HEAT_WEEKS)),
        ) { events, goal, notes -> HeatState(Heatmap.grid(Heatmap.summarise(events, goal), day, HEAT_WEEKS), notes) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeatState())

    /** Journal calendar: the last [YEAR_WEEKS] weeks (study levels derived from activity_event, same rules as the Today heatmap). */
    val yearHeat: StateFlow<HeatState> = dayFlow.flatMapLatest { day ->
        combine(
            activity.observeHeatEvents(Heatmap.firstDate(day, YEAR_WEEKS)), settings.dailyGoalMinutes,
            activity.observeNoteDates(Heatmap.firstDate(day, YEAR_WEEKS)),
        ) { events, goal, notes -> HeatState(Heatmap.grid(Heatmap.summarise(events, goal), day, YEAR_WEEKS), notes) }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HeatState())

    fun dayEvents(date: LocalDate) = activity.observeDay(date)
    fun dayNote(date: LocalDate) = activity.observeNote(date)
    fun saveDayNote(date: LocalDate, text: String) { viewModelScope.launch { activity.saveNote(date, text) } }

    private val latestAffair = combine(affairs.observePack(), affairs.observeUser(), affairs.observeStates()) { pack, user, states ->
        buildAffairList(pack, user, states, AffairFilter()).items.firstOrNull { !it.isUser && it.level == 0 }
    }

    val extras: StateFlow<TodayExtras> = combine(
        sessions.observeDayTotals().map { it.isNotEmpty() },
        dayFlow.flatMapLatest { d -> activity.observeDay(d) }.map { events ->
            // distinct items, so tapping the same topic several times still counts as one piece of work
            events.filter { it.type == ActivityType.AFFAIR_REVISED.name || (it.type == ActivityType.TOPIC_PROGRESS.name && !it.details.contains("\"to\":0")) }
                .map { it.refId }.distinct().size
        },
        target.flatMapLatest { f -> repo.observeTopics(f) }.map { all -> all.firstOrNull { it.level == 0 } },
        latestAffair,
    ) { any, done, topic, affair -> TodayExtras(any, done, topic, affair) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayExtras())

    val userName: StateFlow<String> = settings.userName

    val ui: StateFlow<TodayUiState> = combine(target, combine(progress, affairDue) { p, a -> p.copy(due = mergeDue(p.due, a)) }, stats, settings.examEpochDay, dayFlow) { t, p, (st, goal), examDay, day ->
        val date = if (examDay == SettingsStore.NO_DATE) null else LocalDate.ofEpochDay(examDay)
        TodayUiState(
            examMask = t,
            examDate = date,
            daysToExam = date?.let { ChronoUnit.DAYS.between(day, it) },
            progress = p,
            todayMinutes = st.todayMinutes,
            goalMinutes = goal,
            streak = st.streak,
            // Studying = a timer session OR touching a topic: either keeps her "active" for the mood.
            daysSinceStudy = listOfNotNull(st.daysSinceStudy, p.daysSinceTopicActivity).minOrNull(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun setExamDate(epochDay: Long) = settings.setExamEpochDay(epochDay)
    fun clearExamDate() = settings.setExamEpochDay(SettingsStore.NO_DATE)

    /** "Revised": raises the level by one (or just refreshes the date at the top level), restarting the interval. */
    fun markRevised(item: DueItem) {
        viewModelScope.launch { if (item.affairCategory != null) affairs.revise(item.topicId) else repo.advance(item.topicId) }
    }

    /** Plan items: read a new topic (raises it one level) / review a current-affairs item (marks it revised). */
    fun readTopic(topicId: String) { viewModelScope.launch { repo.advance(topicId) } }
    fun reviseAffair(id: String) { viewModelScope.launch { affairs.revise(id) } }

    fun toggleStar(topicId: String) { viewModelScope.launch { repo.toggleStar(topicId) } }
}

package com.spcode.tnpscprep.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spcode.tnpscprep.data.AffairRepository
import com.spcode.tnpscprep.data.NewsRepository
import com.spcode.tnpscprep.data.SessionRepository
import com.spcode.tnpscprep.data.SettingsStore
import com.spcode.tnpscprep.data.SyllabusRepository
import com.spcode.tnpscprep.ui.affairs.AffairFilter
import com.spcode.tnpscprep.ui.affairs.buildAffairList
import com.spcode.tnpscprep.ui.affairs.newsToUi
import com.spcode.tnpscprep.ui.syllabus.ExamFilter
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
    val target: ExamFilter = ExamFilter.ALL,
    val examDate: LocalDate? = null,
    val daysToExam: Long? = null,
    val progress: TodayProgress = TodayProgress(0, 0, 0, emptyList()),
    val todayMinutes: Int = 0,
    val goalMinutes: Int = 60,
    val streak: Int = 0,
    val daysSinceStudy: Int? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repo: SyllabusRepository,
    private val affairs: AffairRepository,
    news: NewsRepository,
    sessions: SessionRepository,
    private val settings: SettingsStore,
    today: () -> LocalDate = LocalDate::now,
    private val zone: ZoneId = ZoneId.systemDefault(),
) : ViewModel() {

    // Re-emits when the date changes (app left open past midnight) so "due" stays correct.
    private val dayFlow = flow { while (true) { emit(today()); delay(60_000) } }.distinctUntilChanged()

    private val target = settings.targetExam.map { name -> ExamFilter.entries.firstOrNull { it.name == name } ?: ExamFilter.ALL }

    private val progress = target.flatMapLatest { f ->
        combine(
            repo.observeStudied(f.mask), repo.observeTopics(f.mask), repo.observeSubjects(),
            repo.observeStarred(f.mask), dayFlow,
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

    val userName: StateFlow<String> = settings.userName

    val ui: StateFlow<TodayUiState> = combine(target, combine(progress, affairDue) { p, a -> p.copy(due = mergeDue(p.due, a)) }, stats, settings.examEpochDay, dayFlow) { t, p, (st, goal), examDay, day ->
        val date = if (examDay == SettingsStore.NO_DATE) null else LocalDate.ofEpochDay(examDay)
        TodayUiState(
            target = t,
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

    fun setTarget(f: ExamFilter) = settings.setTargetExam(f.name)
    fun setExamDate(epochDay: Long) = settings.setExamEpochDay(epochDay)
    fun clearExamDate() = settings.setExamEpochDay(SettingsStore.NO_DATE)

    /** "Revised": raises the level by one (or just refreshes the date at the top level), restarting the interval. */
    fun markRevised(item: DueItem) {
        viewModelScope.launch { if (item.affairCategory != null) affairs.revise(item.topicId) else repo.advance(item.topicId) }
    }

    fun toggleStar(topicId: String) { viewModelScope.launch { repo.toggleStar(topicId) } }
}

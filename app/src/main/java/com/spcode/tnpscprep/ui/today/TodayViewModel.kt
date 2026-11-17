package com.spcode.tnpscprep.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spcode.tnpscprep.data.SessionRepository
import com.spcode.tnpscprep.data.SettingsStore
import com.spcode.tnpscprep.data.SyllabusRepository
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
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(
    private val repo: SyllabusRepository,
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

    private val stats = combine(sessions.observeDayTotals(), dayFlow, settings.dailyGoalMinutes) { totals, day, goal ->
        Pair(buildDayStats(totals, goal, day), goal)
    }

    val ui: StateFlow<TodayUiState> = combine(target, progress, stats, settings.examEpochDay, dayFlow) { t, p, (st, goal), examDay, day ->
        val date = if (examDay == SettingsStore.NO_DATE) null else LocalDate.ofEpochDay(examDay)
        TodayUiState(
            target = t,
            examDate = date,
            daysToExam = date?.let { ChronoUnit.DAYS.between(day, it) },
            progress = p,
            todayMinutes = st.todayMinutes,
            goalMinutes = goal,
            streak = st.streak,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun setTarget(f: ExamFilter) = settings.setTargetExam(f.name)
    fun setExamDate(epochDay: Long) = settings.setExamEpochDay(epochDay)
    fun clearExamDate() = settings.setExamEpochDay(SettingsStore.NO_DATE)

    /** "Revised": raises the level by one (or just refreshes the date at the top level), restarting the interval. */
    fun markRevised(topicId: String) { viewModelScope.launch { repo.advance(topicId) } }

    fun toggleStar(topicId: String) { viewModelScope.launch { repo.toggleStar(topicId) } }
}

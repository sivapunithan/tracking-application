package com.spcode.tnpscprep.ui.timer

import android.os.SystemClock
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spcode.tnpscprep.data.SessionRepository
import com.spcode.tnpscprep.data.SettingsStore
import com.spcode.tnpscprep.data.SyllabusRepository
import com.spcode.tnpscprep.domain.Phase
import com.spcode.tnpscprep.domain.PomodoroLogic
import com.spcode.tnpscprep.domain.TimerConfig
import com.spcode.tnpscprep.domain.TimerLimits
import com.spcode.tnpscprep.domain.TimerState
import java.time.LocalDate
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface TimerNotice {
    data object None : TimerNotice
    data object LeftApp : TimerNotice
    data class Logged(val minutes: Int) : TimerNotice
}

data class SubjectOption(val id: String, val nameEn: String, val nameTa: String)

data class TimerUiState(
    val phase: Phase = Phase.FOCUS,
    val running: Boolean = false,
    val remainingSeconds: Int = 0,
    val progress: Float = 0f,
    val subjects: List<SubjectOption> = emptyList(),
    val selectedSubjectId: String? = null,
    val todayMinutes: Int = 0,
    val goalMinutes: Int = 60,
    val streak: Int = 0,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val notice: TimerNotice = TimerNotice.None,
)

/**
 * Lives at Activity level (not per tab) so the timer keeps running while she looks at the syllabus.
 * Everything here runs on the main thread (viewModelScope), so no locking is needed.
 * Clocks are injected so the pure logic stays testable.
 */
class TimerViewModel(
    private val sessions: SessionRepository,
    private val settings: SettingsStore,
    syllabus: SyllabusRepository,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val today: () -> LocalDate = LocalDate::now,
) : ViewModel() {

    private val config get() = TimerConfig(settings.focusMinutes.value, settings.breakMinutes.value)

    private val _state = MutableStateFlow(PomodoroLogic.idle(Phase.FOCUS, config))
    private val _subjectId = MutableStateFlow<String?>(null)
    private val _notice = MutableStateFlow<TimerNotice>(TimerNotice.None)
    private val _completions = MutableStateFlow(0)
    private var focusStartedAt = 0L
    private var tickerJob: Job? = null

    /** Cheap, separate flows for the app root (keep-screen-on, haptic) so it doesn't recompose every tick. */
    val running: StateFlow<Boolean> = _state.map { it.running }.distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val completions: StateFlow<Int> = _completions.asStateFlow()

    private val dayStats = combine(
        sessions.observeDayTotals(),
        flow { while (true) { emit(today()); delay(60_000) } }.distinctUntilChanged(), // midnight rollover
        settings.dailyGoalMinutes,
    ) { totals, day, goal -> Triple(buildDayStats(totals, goal, day), goal, 0) }

    private val core = combine(_state, _subjectId, _notice) { s, subj, n -> Triple(s, subj, n) }

    val ui: StateFlow<TimerUiState> = combine(
        core,
        dayStats,
        syllabus.observeSubjects().map { l -> l.map { SubjectOption(it.id, it.nameEn, it.nameTa) } },
        combine(settings.focusMinutes, settings.breakMinutes, ::Pair),
    ) { (state, subjectId, notice), (stats, goal, _), subjects, (focus, brk) ->
        val total = TimerConfig(focus, brk).durationMs(state.phase)
        val secs = PomodoroLogic.displaySeconds(state.remainingMs)
        TimerUiState(
            phase = state.phase,
            running = state.running,
            remainingSeconds = secs,
            // from whole seconds, so the state only changes once per second (not on every 250ms tick)
            progress = (1f - secs * 1000f / total).coerceIn(0f, 1f),
            subjects = subjects,
            selectedSubjectId = subjectId,
            todayMinutes = stats.todayMinutes,
            goalMinutes = goal,
            streak = stats.streak,
            focusMinutes = focus,
            breakMinutes = brk,
            notice = notice,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimerUiState())

    // ---- user actions ----

    fun onStartPause() {
        advance() // catch up on any phase that ended since the last tick
        val s = _state.value
        if (s.running) {
            _state.value = PomodoroLogic.pause(s, clock())
        } else {
            if (s.phase == Phase.FOCUS && focusStartedAt == 0L) focusStartedAt = wallClock()
            _state.value = PomodoroLogic.start(s, clock())
            _notice.value = TimerNotice.None
            startTicker()
        }
    }

    fun onReset() {
        _state.value = PomodoroLogic.reset(_state.value, config)
        focusStartedAt = 0L
        _notice.value = TimerNotice.None
    }

    fun onSkip() {
        _state.value = PomodoroLogic.skip(_state.value, config)
        focusStartedAt = 0L
        _notice.value = TimerNotice.None
    }

    fun selectSubject(id: String?) { if (!_state.value.running) _subjectId.value = id }

    /** Durations can't change mid-session; changing one resets the (idle) current phase to the new length. */
    fun changeFocusMinutes(delta: Int) {
        if (_state.value.running) return
        settings.setFocusMinutes(TimerLimits.step(settings.focusMinutes.value, delta, TimerLimits.FOCUS))
        onReset()
    }

    fun changeBreakMinutes(delta: Int) {
        if (_state.value.running) return
        settings.setBreakMinutes(TimerLimits.step(settings.breakMinutes.value, delta, TimerLimits.BREAK))
        onReset()
    }

    fun changeDailyGoal(delta: Int) =
        settings.setDailyGoalMinutes(TimerLimits.step(settings.dailyGoalMinutes.value, delta, TimerLimits.GOAL))

    /** v1 rule: the timer only runs while the app is open. Leaving it pauses, so away-time never counts as study. */
    fun onAppStopped() {
        advance()
        val s = _state.value
        if (s.running) {
            _state.value = PomodoroLogic.pause(s, clock())
            _notice.value = TimerNotice.LeftApp
        }
    }

    // ---- internals ----

    private fun startTicker() {
        if (tickerJob?.isActive == true) return
        tickerJob = viewModelScope.launch {
            while (_state.value.running) {
                delay(250)
                advance()
            }
        }
    }

    private fun advance() {
        val r = PomodoroLogic.tick(_state.value, clock(), config)
        _state.value = r.state
        if (r.focusCompleted) onFocusCompleted()
    }

    private fun onFocusCompleted() {
        val minutes = settings.focusMinutes.value
        val subject = _subjectId.value
        val startedAt = focusStartedAt.takeIf { it != 0L } ?: wallClock()
        val day = today()
        focusStartedAt = 0L
        viewModelScope.launch { sessions.logFocus(minutes, subject, startedAt, day) }
        _notice.value = TimerNotice.Logged(minutes)
        _completions.value += 1
    }
}

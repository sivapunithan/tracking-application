package com.spcode.tnpscprep.data

import android.content.Context
import com.spcode.tnpscprep.domain.TimerLimits
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A handful of user preferences. SharedPreferences (built in, zero dependencies) instead of
 * DataStore: three ints don't justify another library. Exposed as StateFlow so the UI reacts.
 */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("tnpscprep_settings", Context.MODE_PRIVATE)

    private val _focus = MutableStateFlow(prefs.getInt(K_FOCUS, 25).coerceIn(TimerLimits.FOCUS))
    private val _break = MutableStateFlow(prefs.getInt(K_BREAK, 5).coerceIn(TimerLimits.BREAK))
    private val _goal = MutableStateFlow(prefs.getInt(K_GOAL, 60).coerceIn(TimerLimits.GOAL))

    private val _targetExam = MutableStateFlow(prefs.getString(K_TARGET, "ALL") ?: "ALL")
    private val _examDay = MutableStateFlow(prefs.getLong(K_EXAM_DAY, NO_DATE))

    /** "ALL", "G1", "G2" or "G4": which exam the Today screen reports on. Kept as text so data/ doesn't depend on ui/. */
    val targetExam: StateFlow<String> = _targetExam.asStateFlow()
    /** Exam date as an epoch day (LocalDate.toEpochDay), or [NO_DATE]. */
    val examEpochDay: StateFlow<Long> = _examDay.asStateFlow()

    fun setTargetExam(v: String) { prefs.edit().putString(K_TARGET, v).apply(); _targetExam.value = v }
    fun setExamEpochDay(v: Long) { prefs.edit().putLong(K_EXAM_DAY, v).apply(); _examDay.value = v }

    val focusMinutes: StateFlow<Int> = _focus.asStateFlow()
    val breakMinutes: StateFlow<Int> = _break.asStateFlow()
    val dailyGoalMinutes: StateFlow<Int> = _goal.asStateFlow()

    fun setFocusMinutes(v: Int) = _focus.set(K_FOCUS, v.coerceIn(TimerLimits.FOCUS))
    fun setBreakMinutes(v: Int) = _break.set(K_BREAK, v.coerceIn(TimerLimits.BREAK))
    fun setDailyGoalMinutes(v: Int) = _goal.set(K_GOAL, v.coerceIn(TimerLimits.GOAL))

    private fun MutableStateFlow<Int>.set(key: String, v: Int) {
        prefs.edit().putInt(key, v).apply()
        value = v
    }

    companion object {
        const val NO_DATE = Long.MIN_VALUE
        private const val K_FOCUS = "focus_min"
        private const val K_BREAK = "break_min"
        private const val K_GOAL = "daily_goal_min"
        private const val K_TARGET = "target_exam"
        private const val K_EXAM_DAY = "exam_epoch_day"
    }
}

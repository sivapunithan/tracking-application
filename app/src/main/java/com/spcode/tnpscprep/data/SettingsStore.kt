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

    private companion object {
        const val K_FOCUS = "focus_min"
        const val K_BREAK = "break_min"
        const val K_GOAL = "daily_goal_min"
    }
}

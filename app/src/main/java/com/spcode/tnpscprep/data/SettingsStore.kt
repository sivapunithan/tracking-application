package com.spcode.tnpscprep.data

import com.spcode.tnpscprep.domain.ExamSelection
import android.content.Context
import com.spcode.tnpscprep.domain.TimerLimits
import java.util.Base64
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

    fun setTargetExam(v: String) { prefs.edit().putString(K_TARGET, v).apply(); _targetExam.value = v; _examMask.value = ExamSelection.parse(v) }
    private val _examMask = MutableStateFlow(ExamSelection.parse(_targetExam.value))
    /** "My exams" as a bitmask (G1=1, G2=2, G4=4); every exam when nothing specific is chosen. The one global filter. */
    val examMask: StateFlow<Int> = _examMask.asStateFlow()
    fun setExamMask(mask: Int) = setTargetExam(ExamSelection.format(mask))
    fun setExamEpochDay(v: Long) { prefs.edit().putLong(K_EXAM_DAY, v).apply(); _examDay.value = v }

    private val _newsEnabled = MutableStateFlow(prefs.getBoolean(K_NEWS, true))
    private val _newsLast = MutableStateFlow(prefs.getLong(K_NEWS_LAST, 0L))

    /** Master switch for the only feature that uses the internet. When off, the app makes NO network requests. */
    val newsEnabled: StateFlow<Boolean> = _newsEnabled.asStateFlow()
    /** Epoch millis of the last successful headline refresh; 0 = never. */
    val lastNewsRefreshMs: StateFlow<Long> = _newsLast.asStateFlow()

    fun setNewsEnabled(v: Boolean) { prefs.edit().putBoolean(K_NEWS, v).apply(); _newsEnabled.value = v }
    fun setLastNewsRefresh(ms: Long) { prefs.edit().putLong(K_NEWS_LAST, ms).apply(); _newsLast.value = ms }

    private val _theme = MutableStateFlow(prefs.getString(K_THEME, "LIGHT") ?: "LIGHT")
    private val _name = MutableStateFlow(prefs.getString(K_NAME, "") ?: "")

    /** "LIGHT" (default), "DARK" or "SYSTEM". Kept as text so data/ doesn't depend on ui/. */
    val themeMode: StateFlow<String> = _theme.asStateFlow()
    /** Optional first name for the greeting on the Today screen. Stays on the phone. */
    val userName: StateFlow<String> = _name.asStateFlow()

    fun setThemeMode(v: String) { prefs.edit().putString(K_THEME, v).apply(); _theme.value = v }
    fun setUserName(v: String) { val t = v.trim().take(30); prefs.edit().putString(K_NAME, t).apply(); _name.value = t }

    private val _account = MutableStateFlow(prefs.getBoolean(K_ACCOUNT, false))

    /** True once the local account (name + optional PIN) has been created on this phone. */
    val accountCreated: StateFlow<Boolean> = _account.asStateFlow()
    fun markAccountCreated() { prefs.edit().putBoolean(K_ACCOUNT, true).apply(); _account.value = true }

    // ---- health profile (private, on-device; all optional) ----
    private val _health = MutableStateFlow(readHealth())
    val healthProfile: StateFlow<HealthProfile> = _health.asStateFlow()
    fun setHealthProfile(p: HealthProfile) {
        prefs.edit().putInt(K_H_HEIGHT, p.heightCm).putInt(K_H_AGE, p.age).putString(K_H_SEX, p.sex)
            .putString(K_H_ACT, p.activity).putInt(K_H_CYCLE, p.cycleDays).apply()
        _health.value = p
    }
    private fun readHealth() = HealthProfile(
        prefs.getInt(K_H_HEIGHT, 0), prefs.getInt(K_H_AGE, 0), prefs.getString(K_H_SEX, "FEMALE") ?: "FEMALE",
        prefs.getString(K_H_ACT, "LIGHT") ?: "LIGHT", prefs.getInt(K_H_CYCLE, 28),
    )

    // ---- food check-in: strict mode is OFF unless she turns it on; lastFoodAlert = start date of the chain already shown ----
    private val _strictFood = MutableStateFlow(prefs.getBoolean(K_STRICT, false))
    val strictFood: StateFlow<Boolean> = _strictFood.asStateFlow()
    fun setStrictFood(v: Boolean) { prefs.edit().putBoolean(K_STRICT, v).apply(); _strictFood.value = v }
    private val _foodAlert = MutableStateFlow(prefs.getString(K_FOOD_ALERT, "") ?: "")
    val lastFoodAlertStart: StateFlow<String> = _foodAlert.asStateFlow()
    fun setLastFoodAlertStart(v: String) { prefs.edit().putString(K_FOOD_ALERT, v).apply(); _foodAlert.value = v }

    // ---- Getting started checklist: once completed it never comes back ----
    private val _gsDone = MutableStateFlow(prefs.getBoolean(K_GS_DONE, false))
    val gettingStartedDone: StateFlow<Boolean> = _gsDone.asStateFlow()
    fun setGettingStartedDone(v: Boolean) { prefs.edit().putBoolean(K_GS_DONE, v).apply(); _gsDone.value = v }

    // ---- phone usage (opt-in, off by default) ----
    private val _usage = MutableStateFlow(prefs.getBoolean(K_USAGE, false))
    val usageEnabled: StateFlow<Boolean> = _usage.asStateFlow()
    fun setUsageEnabled(v: Boolean) { prefs.edit().putBoolean(K_USAGE, v).apply(); _usage.value = v }

    // ---- PIN: only a salted PBKDF2 hash is stored, never the PIN itself ----
    fun hasPin(): Boolean = prefs.contains(K_PIN_HASH)
    fun pinRecord(): Pair<ByteArray, ByteArray>? {
        val salt = prefs.getString(K_PIN_SALT, null) ?: return null
        val hash = prefs.getString(K_PIN_HASH, null) ?: return null
        return runCatching { Base64.getDecoder().decode(salt) to Base64.getDecoder().decode(hash) }.getOrNull()
    }
    fun savePin(salt: ByteArray, hash: ByteArray) {
        prefs.edit().putString(K_PIN_SALT, Base64.getEncoder().encodeToString(salt))
            .putString(K_PIN_HASH, Base64.getEncoder().encodeToString(hash)).putInt(K_FAILS, 0).putLong(K_LOCK_UNTIL, 0L).apply()
    }
    fun clearPin() { prefs.edit().remove(K_PIN_SALT).remove(K_PIN_HASH).putInt(K_FAILS, 0).putLong(K_LOCK_UNTIL, 0L).apply() }

    // ---- wrong-PIN accounting (persists across restarts so closing the app does not reset the lockout) ----
    fun failedAttempts(): Int = prefs.getInt(K_FAILS, 0)
    fun lockedUntilMs(): Long = prefs.getLong(K_LOCK_UNTIL, 0L)
    fun recordFailure(lockUntil: Long, fails: Int) { prefs.edit().putInt(K_FAILS, fails).putLong(K_LOCK_UNTIL, lockUntil).apply() }
    fun recordSuccess() { prefs.edit().putInt(K_FAILS, 0).putLong(K_LOCK_UNTIL, 0L).apply() }

    /** "Erase account": forget everything this class stores (the database is cleared separately). */
    fun clearAll() {
        prefs.edit().clear().apply()
        _focus.value = 25; _break.value = 5; _goal.value = 60
        _targetExam.value = "ALL"; _examMask.value = com.spcode.tnpscprep.domain.Exam.ALL_MASK; _examDay.value = NO_DATE
        _newsEnabled.value = true; _newsLast.value = 0L
        _theme.value = "LIGHT"; _name.value = ""; _account.value = false
        _health.value = HealthProfile(); _usage.value = false; _gsDone.value = false; _strictFood.value = false; _foodAlert.value = ""
    }

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
        private const val K_ACCOUNT = "account_created"
        private const val K_PIN_SALT = "pin_salt"
        private const val K_PIN_HASH = "pin_hash"
        private const val K_FAILS = "pin_failed"
        private const val K_LOCK_UNTIL = "pin_lock_until"
        private const val K_THEME = "theme_mode"
        private const val K_H_HEIGHT = "h_height"
        private const val K_H_AGE = "h_age"
        private const val K_H_SEX = "h_sex"
        private const val K_H_ACT = "h_act"
        private const val K_H_CYCLE = "h_cycle"
        private const val K_STRICT = "food_strict"
        private const val K_FOOD_ALERT = "food_alert_start"
        private const val K_GS_DONE = "getting_started_done"
        private const val K_USAGE = "usage_enabled"
        private const val K_NAME = "user_name"
        private const val K_NEWS = "news_enabled"
        private const val K_NEWS_LAST = "news_last_refresh"
        private const val K_EXAM_DAY = "exam_epoch_day"
    }
}

/** Height 0 / age 0 = not entered. Sex and activity are enum names kept as text so data/ stays free of ui/. */
data class HealthProfile(val heightCm: Int = 0, val age: Int = 0, val sex: String = "FEMALE", val activity: String = "LIGHT", val cycleDays: Int = 28)

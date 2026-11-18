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
    private val _moodSounds = MutableStateFlow(prefs.getBoolean(K_MOOD_SOUNDS, true))
    val moodSounds: StateFlow<Boolean> = _moodSounds.asStateFlow()
    fun setMoodSounds(v: Boolean) { prefs.edit().putBoolean(K_MOOD_SOUNDS, v).apply(); _moodSounds.value = v }
    private val _praise = MutableStateFlow(prefs.getBoolean(K_PRAISE, true))
    val praiseNotifs: StateFlow<Boolean> = _praise.asStateFlow()
    fun setPraiseNotifs(v: Boolean) { prefs.edit().putBoolean(K_PRAISE, v).apply(); _praise.value = v }
    private val _praiseAsked = MutableStateFlow(prefs.getBoolean(K_PRAISE_ASKED, false))
    val praiseAsked: StateFlow<Boolean> = _praiseAsked.asStateFlow()
    fun setPraiseAsked(v: Boolean) { prefs.edit().putBoolean(K_PRAISE_ASKED, v).apply(); _praiseAsked.value = v }
    private val _jGuide = MutableStateFlow(prefs.getBoolean(K_J_GUIDE, false))
    val journalGuideDone: StateFlow<Boolean> = _jGuide.asStateFlow()
    fun setJournalGuideDone(v: Boolean) { prefs.edit().putBoolean(K_J_GUIDE, v).apply(); _jGuide.value = v }
    private val _eGuide = MutableStateFlow(prefs.getBoolean(K_E_GUIDE, false))
    val editorGuideDone: StateFlow<Boolean> = _eGuide.asStateFlow()
    fun setEditorGuideDone(v: Boolean) { prefs.edit().putBoolean(K_E_GUIDE, v).apply(); _eGuide.value = v }
    /** Finished first-time tours (page keys: today, study, care, focus, profile). */
    private val _guides = MutableStateFlow(prefs.getStringSet(K_GUIDES, emptySet()) ?: emptySet())
    val guides: StateFlow<Set<String>> = _guides.asStateFlow()
    fun markGuideDone(key: String) { val n = _guides.value + key; prefs.edit().putStringSet(K_GUIDES, n).apply(); _guides.value = n }
    /** "Show the guides again": every tour, including the Journal ones. */
    fun resetGuides() { prefs.edit().remove(K_GUIDES).apply(); _guides.value = emptySet(); setJournalGuideDone(false); setEditorGuideDone(false) }
    private var praiseCounter = prefs.getInt(K_PRAISE_N, 0)
    fun nextPraiseCounter(): Int { praiseCounter++; prefs.edit().putInt(K_PRAISE_N, praiseCounter).apply(); return praiseCounter }
    private val _foodAlert = MutableStateFlow(prefs.getString(K_FOOD_ALERT, "") ?: "")
    val lastFoodAlertStart: StateFlow<String> = _foodAlert.asStateFlow()
    fun setLastFoodAlertStart(v: String) { prefs.edit().putString(K_FOOD_ALERT, v).apply(); _foodAlert.value = v }

    // ---- Getting started checklist: once completed it never comes back ----
    private val _gsDone = MutableStateFlow(prefs.getBoolean(K_GS_DONE, false))
    val gettingStartedDone: StateFlow<Boolean> = _gsDone.asStateFlow()
    private val _wrapHour = MutableStateFlow(prefs.getInt(K_WRAP_HOUR, com.spcode.tnpscprep.domain.TodayGuide.EVENING_HOUR).coerceIn(15, 23))
    /** From which hour (15..23) the Today screen invites her to the evening wrap-up. */
    val wrapupHour: StateFlow<Int> = _wrapHour.asStateFlow()
    fun setWrapupHour(h: Int) { val v = h.coerceIn(15, 23); prefs.edit().putInt(K_WRAP_HOUR, v).apply(); _wrapHour.value = v }

    private val _remOn = MutableStateFlow(prefs.getBoolean(K_REM_ON, false))
    private val _remMin = MutableStateFlow(com.spcode.tnpscprep.domain.ReminderTime.clamp(prefs.getInt(K_REM_MIN, com.spcode.tnpscprep.domain.ReminderTime.DEFAULT_MINUTES)))
    /** Optional evening wrap-up reminder: OFF by default. */
    val reminderEnabled: StateFlow<Boolean> = _remOn.asStateFlow()
    /** Reminder time as minutes after midnight. */
    val reminderMinutes: StateFlow<Int> = _remMin.asStateFlow()
    fun setReminderEnabled(v: Boolean) { prefs.edit().putBoolean(K_REM_ON, v).apply(); _remOn.value = v }
    fun setReminderMinutes(m: Int) { val v = com.spcode.tnpscprep.domain.ReminderTime.clamp(m); prefs.edit().putInt(K_REM_MIN, v).apply(); _remMin.value = v }

    /** Unfinished wrap-up draft (JSON, one day only). See domain/WrapupDrafts. */
    fun wrapupDraft(): String? = prefs.getString(K_WRAP_DRAFT, null)
    fun setWrapupDraft(json: String?) { prefs.edit().apply { if (json == null) remove(K_WRAP_DRAFT) else putString(K_WRAP_DRAFT, json) }.apply() }

    private val _lastBackup = MutableStateFlow(prefs.getLong(K_LAST_BACKUP, 0L))
    /** Epoch millis of the last backup file she saved (0 = never). Not part of the backup itself. */
    val lastBackupMs: StateFlow<Long> = _lastBackup.asStateFlow()
    fun setLastBackupMs(v: Long) { prefs.edit().putLong(K_LAST_BACKUP, v).apply(); _lastBackup.value = v }

    // Backup reminder card on Today (not part of the backup file)
    private val _bkRemind = MutableStateFlow(prefs.getBoolean(K_BK_REMIND, true))
    val backupReminder: StateFlow<Boolean> = _bkRemind.asStateFlow()
    fun setBackupReminder(v: Boolean) { prefs.edit().putBoolean(K_BK_REMIND, v).apply(); _bkRemind.value = v }
    private val _bkSnooze = MutableStateFlow(prefs.getLong(K_BK_SNOOZE, 0L))
    val backupSnoozeUntilMs: StateFlow<Long> = _bkSnooze.asStateFlow()
    fun setBackupSnoozeUntil(v: Long) { prefs.edit().putLong(K_BK_SNOOZE, v).apply(); _bkSnooze.value = v }
    private val _bkBase = MutableStateFlow(prefs.getLong(K_BK_BASE, 0L))
    /** When the app first looked at the backup reminder: the clock start for someone who never backed up. */
    val backupBaseMs: StateFlow<Long> = _bkBase.asStateFlow()
    fun setBackupBaseMs(v: Long) { prefs.edit().putLong(K_BK_BASE, v).apply(); _bkBase.value = v }

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
        _wrapHour.value = com.spcode.tnpscprep.domain.TodayGuide.EVENING_HOUR; _remOn.value = false; _remMin.value = com.spcode.tnpscprep.domain.ReminderTime.DEFAULT_MINUTES; _targetExam.value = "ALL"; _examMask.value = com.spcode.tnpscprep.domain.Exam.ALL_MASK; _examDay.value = NO_DATE
        _newsEnabled.value = true; _newsLast.value = 0L; _lastBackup.value = 0L; _bkRemind.value = true; _bkSnooze.value = 0L; _bkBase.value = 0L
        _theme.value = "LIGHT"; _name.value = ""; _account.value = false
        _health.value = HealthProfile(); _usage.value = false; _gsDone.value = false; _strictFood.value = false; _foodAlert.value = ""; _moodSounds.value = true; _praise.value = true; _praiseAsked.value = false; _jGuide.value = false; _eGuide.value = false; _guides.value = emptySet(); praiseCounter = 0
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
        private const val K_MOOD_SOUNDS = "mood_sounds"
        private const val K_PRAISE = "praise_notifs"
        private const val K_PRAISE_N = "praise_n"
        private const val K_GUIDES = "guides_done"
        private const val K_J_GUIDE = "journal_guide_done"
        private const val K_E_GUIDE = "editor_guide_done"
        private const val K_PRAISE_ASKED = "praise_asked"
        private const val K_FOOD_ALERT = "food_alert_start"
        private const val K_GS_DONE = "getting_started_done"
        private const val K_WRAP_DRAFT = "wrapup_draft"
        private const val K_WRAP_HOUR = "wrapup_hour"
        private const val K_REM_ON = "reminder_on"
        private const val K_REM_MIN = "reminder_minutes"
        private const val K_USAGE = "usage_enabled"
        private const val K_NAME = "user_name"
        private const val K_NEWS = "news_enabled"
        private const val K_NEWS_LAST = "news_last_refresh"
        private const val K_EXAM_DAY = "exam_epoch_day"
        private const val K_LAST_BACKUP = "last_backup_ms"
        private const val K_BK_REMIND = "backup_reminder"
        private const val K_BK_SNOOZE = "backup_snooze_until"
        private const val K_BK_BASE = "backup_base_ms"
    }
}

/** Height 0 / age 0 = not entered. Sex and activity are enum names kept as text so data/ stays free of ui/. */
data class HealthProfile(val heightCm: Int = 0, val age: Int = 0, val sex: String = "FEMALE", val activity: String = "LIGHT", val cycleDays: Int = 28)

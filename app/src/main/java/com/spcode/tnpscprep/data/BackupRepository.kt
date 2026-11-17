package com.spcode.tnpscprep.data

import com.spcode.tnpscprep.data.db.AppDatabase

/** Builds the full backup file from the database + chosen preferences. Read-only: exporting never changes anything. */
class BackupRepository(private val db: AppDatabase, private val settings: SettingsStore, private val now: () -> Long = System::currentTimeMillis) {

    suspend fun buildFile(): FullBackupFile {
        val userItems = db.affairDao().allUser()
        val userCreatedById = userItems.associate { "user-${it.id}" to it.createdAt }
        return FullBackupFile(
            exportedAt = now(),
            diary = db.diaryDao().all().map { DiaryDto(it.localDate, it.text, it.moodEmojiId, it.createdAt, it.updatedAt, it.pinned, it.wrapup) },
            progress = db.progressDao().all().map { ProgressDto(it.topicId, it.level, it.lastUpdated) },
            notes = db.noteDao().all().map { NoteDto(it.topicId, it.note, it.starred, it.updatedAt) },
            sessions = db.sessionDao().all().map { SessionDto(it.date, it.startedAt, it.minutes, it.subjectId) },
            // Her own entries have local ids ("user-<n>") that differ between phones: carry their createdAt instead.
            affairState = db.affairDao().allStates().mapNotNull { s ->
                if (s.itemId.startsWith("user-")) userCreatedById[s.itemId]?.let { AffairStateDto(userCreatedAt = it, bookmarked = s.bookmarked, level = s.level, lastUpdated = s.lastUpdated) }
                else AffairStateDto(itemId = s.itemId, bookmarked = s.bookmarked, level = s.level, lastUpdated = s.lastUpdated)
            },
            userAffairs = userItems.map { UserAffairDto(it.date, it.category, it.title, it.summary, it.createdAt) },
            events = db.activityDao().allForBackup().filter { it.type !in AppBackup.LOCAL_ID_EVENT_TYPES }.map { EventDto(it.timestamp, it.localDate, it.type, it.refId, it.details) },
            dayNotes = db.activityDao().allNotes().map { DayNoteDto(it.date, it.note, it.updatedAt) },
            plans = db.plannedTaskDao().all().map { PlanDto(it.localDate, it.title, it.refType, it.refId, it.done, it.createdAt, it.sortOrder) },
            health = db.healthDao().all().map { HealthDto(it.date, it.kind, it.value, it.createdAt) },
            steps = db.stepDao().all().map { StepDto(it.date, it.baseline, it.lastReading, it.steps, it.bootMs, it.updatedAt) },
            settings = settingsSnapshot(),
        )
    }

    /** Preferences she chose. The PIN hash/salt, wrong-PIN counters, news switch/timestamps and the wrap-up draft are NOT here. */
    fun settingsSnapshot(): SettingsDto {
        val h = settings.healthProfile.value
        return SettingsDto(
            userName = settings.userName.value, targetExam = settings.targetExam.value,
            examEpochDay = settings.examEpochDay.value.takeIf { it != SettingsStore.NO_DATE },
            dailyGoalMinutes = settings.dailyGoalMinutes.value, focusMinutes = settings.focusMinutes.value, breakMinutes = settings.breakMinutes.value,
            wrapupHour = settings.wrapupHour.value, reminderEnabled = settings.reminderEnabled.value, reminderMinutes = settings.reminderMinutes.value,
            themeMode = settings.themeMode.value, strictFood = settings.strictFood.value, gettingStartedDone = settings.gettingStartedDone.value,
            heightCm = h.heightCm, age = h.age, sex = h.sex, activity = h.activity, cycleDays = h.cycleDays,
        )
    }

    suspend fun exportJson(): String = AppBackup.encode(buildFile())
}

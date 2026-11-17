package com.spcode.tnpscprep.data

import androidx.room.withTransaction
import com.spcode.tnpscprep.data.db.ActivityEventEntity
import com.spcode.tnpscprep.data.db.AffairStateEntity
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.DayNoteEntity
import com.spcode.tnpscprep.data.db.HealthEntryEntity
import com.spcode.tnpscprep.data.db.PlannedTaskEntity
import com.spcode.tnpscprep.data.db.StepDayEntity
import com.spcode.tnpscprep.data.db.StudySessionEntity
import com.spcode.tnpscprep.data.db.TopicNoteEntity
import com.spcode.tnpscprep.data.db.TopicProgressEntity
import com.spcode.tnpscprep.data.db.UserAffairEntity

data class RestoreOutcome(val added: Int, val updated: Int)

/** Builds the full backup file (read-only) and restores one (merge-only, one transaction). */
class BackupRepository(
    private val db: AppDatabase, private val settings: SettingsStore, private val diary: DiaryRepository,
    private val now: () -> Long = System::currentTimeMillis,
) {

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

    /** Exports, and remembers the time only after the caller has really written the file. */
    fun markBackedUp() = settings.setLastBackupMs(now())

    /**
     * Merge-only restore in ONE transaction: if anything throws, nothing is applied. Nothing is ever deleted
     * (the only removals are an older local one-per-day check-in replaced by a newer one from the file).
     */
    suspend fun restore(text: String): Result<RestoreOutcome> {
        val file = when (val r = AppBackup.parse(text)) { is AppBackup.Parsed.Ok -> r.file; is AppBackup.Parsed.Bad -> return Result.failure(IllegalArgumentException(r.reason)) }
        return runCatching {
            var added = 0; var updated = 0
            db.withTransaction {
                val d = diary.mergeEntries(file.diary); added += d.added; updated += d.updated

                val pr = db.progressDao(); val lp = pr.all().associate { it.topicId to it.level }
                AppBackup.progressToWrite(lp, file.progress).forEach { pr.upsert(TopicProgressEntity(it.topicId, it.level, it.lastUpdated)); if (it.topicId in lp) updated++ else added++ }

                val nd = db.noteDao(); val ln = nd.all().associate { it.topicId to it.updatedAt }
                AppBackup.notesToWrite(ln, file.notes).forEach { nd.upsert(TopicNoteEntity(it.topicId, it.note, it.starred, it.updatedAt)); if (it.topicId in ln) updated++ else added++ }

                val sd = db.sessionDao()
                AppBackup.sessionsToAdd(sd.all().map { it.startedAt to it.minutes }.toSet(), file.sessions).forEach { sd.insert(StudySessionEntity(date = it.date, startedAt = it.startedAt, minutes = it.minutes, subjectId = it.subjectId)); added++ }

                // Her own affairs first: their new local ids are needed to attach their state.
                val ad = db.affairDao()
                AppBackup.userAffairsToAdd(ad.allUser().map { it.createdAt to it.title }.toSet(), file.userAffairs).forEach { ad.insertUser(UserAffairEntity(date = it.date, category = it.category, title = it.title, summary = it.summary, createdAt = it.createdAt)); added++ }
                val idByCreated = ad.allUser().associate { it.createdAt to it.id }
                val localStates = ad.allStates()
                val createdById = ad.allUser().associate { "user-${it.id}" to it.createdAt }
                fun keyOf(itemId: String) = if (itemId.startsWith("user-")) createdById[itemId]?.let { "user:$it" } ?: itemId else itemId
                val ls = localStates.associate { keyOf(it.itemId) to it.lastUpdated }
                AppBackup.statesToWrite(ls, file.affairState).forEach { s ->
                    val itemId = s.itemId ?: idByCreated[s.userCreatedAt]?.let { "user-$it" } ?: return@forEach
                    ad.upsertState(AffairStateEntity(itemId, s.bookmarked, s.level, s.lastUpdated)); if (AppBackup.stateKey(s) in ls) updated++ else added++
                }

                val act = db.activityDao(); val localEvents = act.allForBackup()
                val ids = localEvents.map { Triple(it.type, it.refId, it.timestamp) }.toSet()
                val latest = localEvents.filter { it.refId != null }.groupBy { it.type to it.refId!! }.mapValues { (_, g) -> g.maxOf { it.timestamp } }
                AppBackup.eventsToAdd(ids, latest, file.events).forEach { e ->
                    // a newer one-per-day answer replaces the older local one so the day never holds two
                    if (e.refId != null && (e.type to e.refId) in latest && e.type in AppBackup.ONE_PER_REF) act.deleteByRef(e.type, e.refId)
                    act.insert(ActivityEventEntity(timestamp = e.timestamp, localDate = e.localDate, type = e.type, refId = e.refId, details = e.details)); added++
                }

                val ln2 = act.allNotes().associate { it.date to it.updatedAt }
                AppBackup.dayNotesToWrite(ln2, file.dayNotes).forEach { act.putNote(DayNoteEntity(it.date, it.note, it.updatedAt)); if (it.date in ln2) updated++ else added++ }

                val pt = db.plannedTaskDao()
                AppBackup.plansToAdd(pt.all().map { Triple(it.localDate, it.createdAt, it.title) }.toSet(), file.plans).forEach {
                    pt.insert(PlannedTaskEntity(localDate = it.localDate, title = it.title, refType = it.refType, refId = it.refId, done = it.done, createdAt = it.createdAt, sortOrder = it.sortOrder)); added++
                }

                val hd = db.healthDao()
                AppBackup.healthToAdd(hd.all().map { Triple(it.kind, it.date, it.createdAt) }.toSet(), file.health).forEach { hd.insert(HealthEntryEntity(date = it.date, kind = it.kind, value = it.value, createdAt = it.createdAt)); added++ }

                val st = db.stepDao(); val lst = st.all().associate { it.date to it.updatedAt }
                AppBackup.stepsToWrite(lst, file.steps).forEach { st.upsert(StepDayEntity(it.date, it.baseline, it.lastReading, it.steps, it.bootMs, it.updatedAt)); if (it.date in lst) updated++ else added++ }
            }
            // Preferences are written only after the database part succeeded.
            file.settings?.let { applySettings(it) }
            RestoreOutcome(added, updated)
        }
    }

    /**
     * Fills in preferences that are still at their defaults on this phone; one she already changed is kept.
     * The reminder switch is NOT restored (turning it on needs the notification permission and an alarm): she switches it on herself.
     */
    private fun applySettings(s: SettingsDto) {
        if (settings.userName.value.isBlank()) s.userName?.let(settings::setUserName)
        if (settings.targetExam.value == "ALL") s.targetExam?.let(settings::setTargetExam)
        if (settings.examEpochDay.value == SettingsStore.NO_DATE) s.examEpochDay?.let(settings::setExamEpochDay)
        if (settings.dailyGoalMinutes.value == 60) s.dailyGoalMinutes?.let(settings::setDailyGoalMinutes)
        if (settings.focusMinutes.value == 25) s.focusMinutes?.let(settings::setFocusMinutes)
        if (settings.breakMinutes.value == 5) s.breakMinutes?.let(settings::setBreakMinutes)
        if (settings.wrapupHour.value == com.spcode.tnpscprep.domain.TodayGuide.EVENING_HOUR) s.wrapupHour?.let(settings::setWrapupHour)
        if (settings.reminderMinutes.value == com.spcode.tnpscprep.domain.ReminderTime.DEFAULT_MINUTES) s.reminderMinutes?.let(settings::setReminderMinutes)
        if (settings.themeMode.value == "LIGHT") s.themeMode?.let(settings::setThemeMode)
        if (!settings.strictFood.value) s.strictFood?.let(settings::setStrictFood)
        if (!settings.gettingStartedDone.value) s.gettingStartedDone?.let(settings::setGettingStartedDone)
        if (settings.healthProfile.value == HealthProfile()) {
            val d = HealthProfile()
            settings.setHealthProfile(HealthProfile(s.heightCm ?: d.heightCm, s.age ?: d.age, s.sex ?: d.sex, s.activity ?: d.activity, s.cycleDays ?: d.cycleDays))
        }
    }
}

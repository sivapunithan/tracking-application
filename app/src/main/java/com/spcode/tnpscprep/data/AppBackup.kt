package com.spcode.tnpscprep.data

import com.spcode.tnpscprep.domain.ActivityType
import com.spcode.tnpscprep.domain.DiaryLimits
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Full "Mathi backup" (file version 3). Same envelope as the diary export ({"app","version","exportedAt","diary"}) plus more top-level
 * sections, so the diary-only import still reads the diary out of a full file. Plain unencrypted text: the UI says so.
 *
 * NEVER in the file: the PIN hash/salt, the syllabus, the curated pack, cached headlines, phone-usage history, the wrap-up draft.
 * Ids that differ between phones (diary id, user-affair id, event id) are never used as identity: each section has its own natural key.
 */
@Serializable
data class FullBackupFile(
    val app: String = DiaryBackupFile.APP,
    val version: Int = DiaryBackupFile.VERSION,
    val exportedAt: Long = 0,
    val diary: List<DiaryDto> = emptyList(),
    val progress: List<ProgressDto> = emptyList(),
    val notes: List<NoteDto> = emptyList(),
    val sessions: List<SessionDto> = emptyList(),
    val affairState: List<AffairStateDto> = emptyList(),
    val userAffairs: List<UserAffairDto> = emptyList(),
    val events: List<EventDto> = emptyList(),
    val dayNotes: List<DayNoteDto> = emptyList(),
    val plans: List<PlanDto> = emptyList(),
    val health: List<HealthDto> = emptyList(),
    val steps: List<StepDto> = emptyList(),
    val settings: SettingsDto? = null,
    /** Photo FRAMES only (the pictures themselves travel inside a .mathi zip). Optional: a plain file has none. */
    val photos: List<PhotoDto> = emptyList(),
)

@Serializable data class ProgressDto(val topicId: String, val level: Int, val lastUpdated: Long)
@Serializable data class NoteDto(val topicId: String, val note: String, val starred: Boolean, val updatedAt: Long)
@Serializable data class SessionDto(val date: String, val startedAt: Long, val minutes: Int, val subjectId: String? = null)

/** [itemId] is a pack id or "news-<hash>". For her OWN entries [userCreatedAt] is set instead (their local ids differ between phones). */
@Serializable data class AffairStateDto(val itemId: String? = null, val userCreatedAt: Long? = null, val bookmarked: Boolean, val level: Int, val lastUpdated: Long)
@Serializable data class UserAffairDto(val date: String, val category: String, val title: String, val summary: String, val createdAt: Long)
@Serializable data class EventDto(val timestamp: Long, val localDate: String, val type: String, val refId: String? = null, val details: String = "{}")
@Serializable data class DayNoteDto(val date: String, val note: String, val updatedAt: Long)
@Serializable data class PlanDto(val localDate: String, val title: String, val refType: String, val refId: String? = null, val done: Boolean, val createdAt: Long, val sortOrder: Int)
@Serializable data class HealthDto(val date: String, val kind: String, val value: Double, val createdAt: Long)
@Serializable data class PhotoDto(val name: String, val frame: String, val cornerDp: Int, val width: Int, val height: Int, val createdAt: Long)
@Serializable data class StepDto(val date: String, val baseline: Long, val lastReading: Long, val steps: Int, val bootMs: Long, val updatedAt: Long)

/** Only preferences she chose. Every field is optional so a file from an older/newer app version still reads. */
@Serializable
data class SettingsDto(
    val userName: String? = null, val targetExam: String? = null, val examEpochDay: Long? = null,
    val dailyGoalMinutes: Int? = null, val focusMinutes: Int? = null, val breakMinutes: Int? = null,
    val wrapupHour: Int? = null, val reminderEnabled: Boolean? = null, val reminderMinutes: Int? = null,
    val themeMode: String? = null, val strictFood: Boolean? = null, val gettingStartedDone: Boolean? = null,
    val heightCm: Int? = null, val age: Int? = null, val sex: String? = null, val activity: String? = null, val cycleDays: Int? = null,
)

object AppBackup {
    const val MAX_FILE_CHARS = 40_000_000
    private const val MAX_ROWS = 1_000_000
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = false }

    /** Event types whose refId is a LOCAL id (differs between phones): they are rebuilt by the diary import, never carried. */
    val LOCAL_ID_EVENT_TYPES = setOf(ActivityType.DIARY.name)

    fun encode(file: FullBackupFile): String = json.encodeToString(FullBackupFile.serializer(), file)

    sealed interface Parsed { data class Ok(val file: FullBackupFile) : Parsed; data class Bad(val reason: String) : Parsed }

    /** All-or-nothing: one invalid row rejects the whole file, so a damaged file can never half-apply. */
    fun parse(text: String): Parsed {
        if (text.length > MAX_FILE_CHARS) return Parsed.Bad("too_big")
        val f = runCatching { json.decodeFromString(FullBackupFile.serializer(), text) }.getOrElse { return Parsed.Bad("not_a_backup") }
        if (f.app != DiaryBackupFile.APP) return Parsed.Bad("not_a_backup")
        if (f.version !in 1..DiaryBackupFile.VERSION) return Parsed.Bad("version")
        val total = f.diary.size + f.progress.size + f.notes.size + f.sessions.size + f.affairState.size + f.userAffairs.size +
            f.events.size + f.dayNotes.size + f.plans.size + f.health.size + f.steps.size + f.photos.size
        if (total > MAX_ROWS) return Parsed.Bad("too_big")
        return if (valid(f)) Parsed.Ok(f) else Parsed.Bad("bad_entry")
    }

    private fun date(s: String) = runCatching { LocalDate.parse(s) }.isSuccess
    private fun short(s: String?, max: Int) = s == null || s.length <= max

    private fun valid(f: FullBackupFile): Boolean {
        // Diary rows are validated by the same rules as the diary-only import.
        if (DiaryBackup.parse(json.encodeToString(DiaryBackupFile.serializer(), DiaryBackupFile(exportedAt = f.exportedAt, diary = f.diary))) !is ParseResult.Ok) return false
        if (!f.progress.all { it.topicId.isNotBlank() && short(it.topicId, 64) && it.level in 0..4 && it.lastUpdated >= 0 }) return false
        if (!f.notes.all { it.topicId.isNotBlank() && short(it.topicId, 64) && it.note.length <= 20_000 && it.updatedAt >= 0 }) return false
        if (!f.sessions.all { date(it.date) && it.startedAt > 0 && it.minutes in 0..1440 && short(it.subjectId, 64) }) return false
        if (!f.affairState.all { (it.itemId != null) != (it.userCreatedAt != null) && short(it.itemId, 80) && it.level in 0..4 && it.lastUpdated >= 0 }) return false
        if (!f.userAffairs.all { date(it.date) && it.createdAt > 0 && it.title.length <= 2_000 && it.summary.length <= 20_000 && short(it.category, 32) }) return false
        if (!f.events.all { date(it.localDate) && it.timestamp > 0 && it.type.isNotBlank() && short(it.type, 40) && short(it.refId, 80) && it.details.length <= 20_000 && it.type !in LOCAL_ID_EVENT_TYPES }) return false
        if (!f.dayNotes.all { date(it.date) && it.note.length <= 1_000 }) return false
        if (!f.plans.all { date(it.localDate) && it.title.length <= 500 && short(it.refType, 32) && short(it.refId, 80) && it.createdAt > 0 }) return false
        if (!f.health.all { date(it.date) && it.kind.isNotBlank() && short(it.kind, 32) && it.value.isFinite() && it.createdAt > 0 }) return false
        if (!f.steps.all { date(it.date) && it.steps in 0..1_000_000 }) return false
        if (!f.photos.all { com.spcode.tnpscprep.domain.DiaryPhotos.isName(it.name) && short(it.frame, 16) && it.cornerDp in 0..96 && it.width in 1..20_000 && it.height in 1..20_000 && it.createdAt > 0 }) return false
        f.settings?.let { s ->
            if (!short(s.userName, 60) || !short(s.targetExam, 20) || !short(s.themeMode, 12) || !short(s.sex, 12) || !short(s.activity, 12)) return false
        }
        return true
    }

    /** Dedupe helper used by every section: the LATER-updated copy of the same natural key wins. */
    fun <T> newest(rows: List<T>, key: (T) -> Any, stamp: (T) -> Long): List<T> = rows.groupBy(key).map { (_, g) -> g.maxBy(stamp) }

    // ---- merge rules (pure; the repository applies the result inside ONE transaction; nothing is ever deleted) ----

    /** Topic level: the HIGHER level wins (a restore must never lower her progress); equal keeps the local row. */
    fun progressToWrite(local: Map<String, Int>, incoming: List<ProgressDto>): List<ProgressDto> =
        newest(incoming, { it.topicId }, { it.level.toLong() }).filter { it.level > (local[it.topicId] ?: 0) }

    /** Notes: the copy edited later wins. A topic with no local note is added. */
    fun notesToWrite(local: Map<String, Long>, incoming: List<NoteDto>): List<NoteDto> =
        newest(incoming, { it.topicId }, { it.updatedAt }).filter { n -> local[n.topicId]?.let { n.updatedAt > it } ?: true }

    /** Focus sessions: identity = startedAt + minutes (the same session is never counted twice). */
    fun sessionsToAdd(local: Set<Pair<Long, Int>>, incoming: List<SessionDto>): List<SessionDto> =
        incoming.distinctBy { it.startedAt to it.minutes }.filter { (it.startedAt to it.minutes) !in local }

    /** Own affairs: identity = createdAt + title. */
    fun userAffairsToAdd(local: Set<Pair<Long, String>>, incoming: List<UserAffairDto>): List<UserAffairDto> =
        incoming.distinctBy { it.createdAt to it.title }.filter { (it.createdAt to it.title) !in local }

    /** Affair state: later lastUpdated wins. [key] is the pack id, or "user:<createdAt>" for her own entries. */
    fun stateKey(s: AffairStateDto): String = s.itemId ?: "user:${s.userCreatedAt}"
    fun statesToWrite(local: Map<String, Long>, incoming: List<AffairStateDto>): List<AffairStateDto> =
        newest(incoming, ::stateKey, { it.lastUpdated }).filter { s -> local[stateKey(s)]?.let { s.lastUpdated > it } ?: true }

    /**
     * Activity events. Identity = type + refId + timestamp. One-per-day check-ins (FOOD/MOOD/STEPS: refId = the date) keep only the
     * NEWER row per (type, refId), so a restore never gives a day two food answers.
     */
    val ONE_PER_REF = setOf(ActivityType.FOOD_CHECKIN.name, ActivityType.MOOD_CHECKIN.name, ActivityType.STEPS.name)
    fun eventsToAdd(local: Set<Triple<String, String?, Long>>, localLatestByRef: Map<Pair<String, String>, Long>, incoming: List<EventDto>): List<EventDto> {
        val (single, many) = incoming.partition { it.type in ONE_PER_REF && it.refId != null }
        val newestSingle = newest(single, { it.type to it.refId }, { it.timestamp }).filter { e -> localLatestByRef[e.type to e.refId!!]?.let { e.timestamp > it } ?: true }
        val rest = many.distinctBy { Triple(it.type, it.refId, it.timestamp) }.filter { Triple(it.type, it.refId, it.timestamp) !in local }
        return newestSingle + rest
    }

    fun dayNotesToWrite(local: Map<String, Long>, incoming: List<DayNoteDto>): List<DayNoteDto> =
        newest(incoming, { it.date }, { it.updatedAt }).filter { n -> local[n.date]?.let { n.updatedAt > it } ?: true }

    /** Plans: identity = date + createdAt + title; an incoming "done" is added to a local copy, never removed. */
    fun plansToAdd(local: Set<Triple<String, Long, String>>, incoming: List<PlanDto>): List<PlanDto> =
        incoming.distinctBy { Triple(it.localDate, it.createdAt, it.title) }.filter { Triple(it.localDate, it.createdAt, it.title) !in local }

    /** Health readings: identity = kind + date + createdAt. */
    fun healthToAdd(local: Set<Triple<String, String, Long>>, incoming: List<HealthDto>): List<HealthDto> =
        incoming.distinctBy { Triple(it.kind, it.date, it.createdAt) }.filter { Triple(it.kind, it.date, it.createdAt) !in local }

    fun stepsToWrite(local: Map<String, Long>, incoming: List<StepDto>): List<StepDto> =
        newest(incoming, { it.date }, { it.updatedAt }).filter { s -> local[s.date]?.let { s.updatedAt > it } ?: true }

    /**
     * Photos to bring back from a .mathi: only those whose picture is in the zip ([zipNames]), that this phone does not have yet,
     * and whose token is in an entry that exists here ([entryIdOf] = the local diary row holding "[[photo:name]]").
     */
    fun photosToRestore(localNames: Set<String>, zipNames: Set<String>, incoming: List<PhotoDto>, entryIdOf: (String) -> Long?): List<Pair<PhotoDto, Long>> =
        incoming.distinctBy { it.name }.filter { it.name in zipNames && it.name !in localNames }.mapNotNull { p -> entryIdOf(p.name)?.let { p to it } }
}

package com.spcode.tnpscprep.data

import com.spcode.tnpscprep.domain.DiaryLimits
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Export file: { "app": "TnpscPrep", "version": 1, "diary": [ ... ] }. Top-level sections so a future full backup can add more
 * (progress, notes ...) without breaking old files. Plain unencrypted text: the UI says so.
 */
@Serializable
data class DiaryBackupFile(val app: String = APP, val version: Int = VERSION, val exportedAt: Long = 0, val diary: List<DiaryDto> = emptyList()) {
    companion object { const val APP = "TnpscPrep"; const val VERSION = 1 }
}

@Serializable
data class DiaryDto(val localDate: String, val text: String, val moodEmojiId: String? = null, val createdAt: Long, val updatedAt: Long)

/** What an existing row looks like to the merge (its identity is localDate + createdAt, because ids differ between phones). */
data class ExistingDiary(val id: Long, val localDate: String, val createdAt: Long, val updatedAt: Long)

data class MergePlan(val inserts: List<DiaryDto>, val updates: List<Pair<Long, DiaryDto>>, val unchanged: Int)

sealed interface ParseResult {
    data class Ok(val entries: List<DiaryDto>) : ParseResult
    data class Bad(val reason: String) : ParseResult
}

object DiaryBackup {
    const val MAX_FILE_CHARS = 10_000_000
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true; prettyPrint = true }

    fun encode(entries: List<DiaryDto>, nowMs: Long): String = json.encodeToString(DiaryBackupFile.serializer(), DiaryBackupFile(exportedAt = nowMs, diary = entries))

    /** All-or-nothing: ONE invalid entry rejects the whole file, so a damaged file can never half-apply. */
    fun parse(text: String): ParseResult {
        if (text.length > MAX_FILE_CHARS) return ParseResult.Bad("too_big")
        val file = runCatching { json.decodeFromString(DiaryBackupFile.serializer(), text) }.getOrElse { return ParseResult.Bad("not_a_backup") }
        if (file.app != DiaryBackupFile.APP) return ParseResult.Bad("not_a_backup")
        if (file.version != DiaryBackupFile.VERSION) return ParseResult.Bad("version")
        for (e in file.diary) {
            if (runCatching { LocalDate.parse(e.localDate) }.isFailure) return ParseResult.Bad("bad_entry")
            if (e.text.length > DiaryLimits.MAX_CHARS || e.createdAt <= 0 || e.updatedAt < 0) return ParseResult.Bad("bad_entry")
            if (e.moodEmojiId != null && e.moodEmojiId.length > 20) return ParseResult.Bad("bad_entry")
        }
        // The same entry twice in one file: keep the later-updated copy.
        val deduped = file.diary.groupBy { it.localDate to it.createdAt }.map { (_, g) -> g.maxBy { it.updatedAt } }
        return ParseResult.Ok(deduped)
    }

    /** New entries are added; a match keeps whichever side was edited later; NOTHING is ever deleted. */
    fun plan(existing: List<ExistingDiary>, incoming: List<DiaryDto>): MergePlan {
        val byKey = existing.associateBy { it.localDate to it.createdAt }
        val inserts = ArrayList<DiaryDto>(); val updates = ArrayList<Pair<Long, DiaryDto>>(); var same = 0
        for (e in incoming) {
            val have = byKey[e.localDate to e.createdAt]
            when {
                have == null -> inserts += e
                e.updatedAt > have.updatedAt -> updates += have.id to e
                else -> same++
            }
        }
        return MergePlan(inserts, updates, same)
    }
}

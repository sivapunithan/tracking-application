package com.spcode.tnpscprep.data

import androidx.room.withTransaction
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.DiaryEntryEntity
import com.spcode.tnpscprep.domain.ActivityType
import com.spcode.tnpscprep.domain.DiaryItem
import com.spcode.tnpscprep.domain.DiarySearch
import com.spcode.tnpscprep.domain.DiaryLimits
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive

data class ImportOutcome(val added: Int, val updated: Int, val unchanged: Int)

class DiaryRepository(private val db: AppDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = db.diaryDao()

    private fun rows(f: Flow<List<com.spcode.tnpscprep.data.db.DiaryPreviewRow>>): Flow<List<DiaryItem>> =
        f.map { l -> l.mapNotNull { r -> runCatching { LocalDate.parse(r.localDate) }.getOrNull()?.let { DiaryItem(r.id, it, r.moodEmojiId, r.preview, r.pinned, r.isWrapup) } } }

    /** Timeline, or search results when [query] is not blank. */
    fun observe(query: String): Flow<List<DiaryItem>> {
        val p = DiarySearch.likePattern(query) ?: return rows(dao.observePreviews())
        return rows(dao.observeSearch(p, DiaryLimits.SEARCH_LIMIT))
    }

    /** Evening Wrap-up as a normal diary entry (so search and the day story keep working). Saving again edits the same entry. */
    suspend fun saveWrapup(date: LocalDate, content: com.spcode.tnpscprep.domain.WrapupContent, mood: String?, headings: com.spcode.tnpscprep.domain.WrapupHeadings): Long = db.withTransaction {
        val store = object : com.spcode.tnpscprep.domain.WrapupStore {
            override suspend fun find(date: LocalDate) = dao.wrapupFor(date.toString())?.let { com.spcode.tnpscprep.domain.WrapupRow(it.id, date, it.text, it.moodEmojiId, it.wrapup.orEmpty()) }
            override suspend fun insert(date: LocalDate, text: String, mood: String?, json: String, now: Long) =
                dao.insert(DiaryEntryEntity(localDate = date.toString(), text = text, moodEmojiId = mood, createdAt = now, updatedAt = now, wrapup = json))
            override suspend fun update(id: Long, text: String, mood: String?, json: String, now: Long) {
                dao.get(id)?.let { dao.update(it.copy(text = text, moodEmojiId = mood, updatedAt = now, wrapup = json)) }
            }
        }
        val t = now()
        val id = com.spcode.tnpscprep.domain.WrapupSaver.save(store, date, content, mood, headings, t)
        writeEvent(id, date.toString(), com.spcode.tnpscprep.domain.Wrapup.render(content, headings).length, t)
        id
    }

    suspend fun wrapupFor(date: LocalDate): DiaryEntryEntity? = dao.wrapupFor(date.toString())
    fun observeWrapupDates(from: LocalDate): Flow<List<String>> = dao.observeWrapupDates(from.toString())

    suspend fun setPinned(id: Long, pinned: Boolean) = dao.setPinned(id, pinned)

    fun observeForDate(date: LocalDate): Flow<List<DiaryEntryEntity>> = dao.observeForDate(date.toString())
    suspend fun get(id: Long): DiaryEntryEntity? = dao.get(id)

    /** Creates (id == null) or updates an entry and refreshes its single DIARY event, all in one transaction. Returns the id. */
    suspend fun save(id: Long?, date: LocalDate, text: String, mood: String?): Long = db.withTransaction {
        val t = now()
        val entryId = if (id == null) dao.insert(DiaryEntryEntity(localDate = date.toString(), text = text, moodEmojiId = mood, createdAt = t, updatedAt = t))
        else {
            val old = dao.get(id)
            if (old == null) dao.insert(DiaryEntryEntity(localDate = date.toString(), text = text, moodEmojiId = mood, createdAt = t, updatedAt = t))
            else { dao.update(old.copy(text = text, moodEmojiId = mood, updatedAt = t)); id }
        }
        writeEvent(entryId, date.toString(), text.length, t)
        entryId
    }

    /** Deletes an entry and its event. Returns the removed row so the caller can offer Undo. */
    suspend fun delete(id: Long): DiaryEntryEntity? = db.withTransaction {
        val old = dao.get(id) ?: return@withTransaction null
        dao.delete(id)
        db.activityDao().deleteByRef(ActivityType.DIARY.name, id.toString())
        old
    }

    /** Undo: puts the exact row back (same id, same timestamps). */
    suspend fun restore(e: DiaryEntryEntity) = db.withTransaction {
        dao.insertWithId(e)
        writeEvent(e.id, e.localDate, e.text.length, now())
    }

    suspend fun exportJson(): String = DiaryBackup.encode(dao.all().map { DiaryDto(it.localDate, it.text, it.moodEmojiId, it.createdAt, it.updatedAt, it.pinned, it.wrapup) }, now())

    /** Merge-only import in ONE transaction: adds new entries, updates a match only if the file's copy is newer, never deletes. */
    suspend fun importJson(text: String): Result<ImportOutcome> {
        val entries = when (val r = DiaryBackup.parse(text)) { is ParseResult.Ok -> r.entries; is ParseResult.Bad -> return Result.failure(IllegalArgumentException(r.reason)) }
        return runCatching {
            db.withTransaction {
                val plan = DiaryBackup.plan(dao.all().map { ExistingDiary(it.id, it.localDate, it.createdAt, it.updatedAt) }, entries)
                plan.inserts.forEach { e ->
                    val id = dao.insert(DiaryEntryEntity(localDate = e.localDate, text = e.text, moodEmojiId = e.moodEmojiId, createdAt = e.createdAt, updatedAt = e.updatedAt, pinned = e.pinned, wrapup = e.wrapup))
                    writeEvent(id, e.localDate, e.text.length, now())
                }
                plan.updates.forEach { (id, e) ->
                    dao.get(id)?.let { dao.update(it.copy(text = e.text, moodEmojiId = e.moodEmojiId, updatedAt = e.updatedAt, pinned = it.pinned || e.pinned, wrapup = e.wrapup ?: it.wrapup)); writeEvent(id, it.localDate, e.text.length, now()) }
                }
                ImportOutcome(plan.inserts.size, plan.updates.size, plan.unchanged)
            }
        }
    }

    private suspend fun writeEvent(entryId: Long, date: String, chars: Int, t: Long) {
        val act = db.activityDao()
        act.deleteByRef(ActivityType.DIARY.name, entryId.toString())
        act.insert(ActivityEvents.build(ActivityType.DIARY, entryId.toString(), t, localDateOverride = date) { put("chars", JsonPrimitive(chars)) })
    }
}

package com.spcode.tnpscprep.data

import androidx.room.withTransaction
import com.spcode.tnpscprep.data.db.AffairStateEntity
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.UserAffairEntity
import com.spcode.tnpscprep.data.seed.RoomAffairStore
import com.spcode.tnpscprep.domain.ActivityType
import com.spcode.tnpscprep.domain.RevisionLevel
import kotlinx.serialization.json.put
import java.time.LocalDate
import kotlinx.coroutines.flow.map

class AffairRepository(
    private val db: AppDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val dao = db.affairDao()

    fun observePack() = dao.observePack()
    fun observeUser() = dao.observeUser()
    fun observeStates() = dao.observeStates()
    fun observeDataUpTo() = dao.observeMeta(RoomAffairStore.KEY_UP_TO).map { it?.let(LocalDate::parse) }

    suspend fun toggleBookmark(itemId: String) = db.withTransaction {
        val cur = dao.getState(itemId)
        write(itemId, bookmarked = !(cur?.bookmarked ?: false), level = cur?.level ?: 0)
    }

    /** "I revised this": level +1 (stops at the top level but still refreshes the date, like topics). */
    suspend fun revise(itemId: String) = db.withTransaction {
        val cur = dao.getState(itemId)
        val next = RevisionLevel.advance(cur?.level ?: 0)
        write(itemId, bookmarked = cur?.bookmarked ?: false, level = next)
        db.activityDao().insert(ActivityEvents.build(ActivityType.AFFAIR_REVISED, itemId, now()) { put("to", next) })
    }

    /** Undo a mistaken "Revised": back to level 0, keeping the bookmark (the state row is deleted when nothing is left to keep). */
    suspend fun resetRevision(itemId: String) = db.withTransaction {
        val cur = dao.getState(itemId)
        if (cur != null) write(itemId, bookmarked = cur.bookmarked, level = 0)
    }

    private suspend fun write(itemId: String, bookmarked: Boolean, level: Int) {
        if (!bookmarked && level == 0) dao.deleteState(itemId)
        else dao.upsertState(AffairStateEntity(itemId, bookmarked, level, now()))
    }

    suspend fun addUserItem(date: LocalDate, category: String, title: String, summary: String) = db.withTransaction {
        val id = dao.insertUser(UserAffairEntity(date = date.toString(), category = category, title = title.trim(), summary = summary.trim(), createdAt = now()))
        db.activityDao().insert(ActivityEvents.build(ActivityType.AFFAIR_ADDED, "user-$id", now()) { put("category", category) })
    }

    suspend fun deleteUserItem(id: Long) = db.withTransaction {
        dao.deleteUser(id)
        dao.deleteState("$USER_PREFIX$id")
    }

    companion object { const val USER_PREFIX = "user-" }
}

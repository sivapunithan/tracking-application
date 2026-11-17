package com.spcode.tnpscprep.data

import androidx.room.withTransaction
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.PlannedTaskEntity
import com.spcode.tnpscprep.domain.PlanOrder
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** REVISION = revise a topic that is due; TOPIC = read a new topic. (Plain text in the table, so adding a value needs no migration.) */
enum class PlanRefType { TOPIC, CURRENT_AFFAIRS, CUSTOM, REVISION }

data class PlanDraftItem(val title: String, val refType: PlanRefType, val refId: String?)

class PlannedTaskRepository(private val db: AppDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = db.plannedTaskDao()

    fun observe(date: LocalDate): Flow<List<PlannedTaskEntity>> = dao.observe(date.toString())

    /** Replaces the list for [date] (used by the wrap-up for tomorrow's top 3): blank titles are dropped, order = list order. */
    suspend fun replaceFor(date: LocalDate, items: List<PlanDraftItem>) = db.withTransaction {
        dao.deleteForDate(date.toString())
        items.filter { it.title.isNotBlank() }.forEachIndexed { i, it ->
            dao.insert(PlannedTaskEntity(localDate = date.toString(), title = it.title.trim(), refType = it.refType.name, refId = it.refId, done = false, createdAt = now(), sortOrder = i))
        }
    }

    /** Stores [items] as the day's plan ONLY if the day has no plan yet (a wrap-up plan from last night wins). Returns true when it wrote. */
    suspend fun generateIfEmpty(date: LocalDate, items: List<PlanDraftItem>): Boolean = db.withTransaction {
        if (items.isEmpty() || dao.forDate(date.toString()).isNotEmpty()) return@withTransaction false
        items.forEachIndexed { i, it -> dao.insert(PlannedTaskEntity(localDate = date.toString(), title = it.title.trim(), refType = it.refType.name, refId = it.refId, done = false, createdAt = now(), sortOrder = i)) }
        true
    }

    suspend fun get(date: LocalDate): List<PlannedTaskEntity> = dao.forDate(date.toString())

    suspend fun setDone(id: Long, done: Boolean) = dao.setDone(id, done)

    /** Moves one task inside the day's list and rewrites the order numbers in one transaction. */
    suspend fun move(date: LocalDate, from: Int, to: Int) = db.withTransaction {
        val list = dao.forDate(date.toString())
        PlanOrder.move(list, from, to).forEachIndexed { i, t -> if (t.sortOrder != i) dao.setOrder(t.id, i) }
    }

    suspend fun delete(id: Long) = dao.delete(id)
}

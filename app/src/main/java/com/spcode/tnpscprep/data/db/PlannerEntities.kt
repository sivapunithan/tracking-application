package com.spcode.tnpscprep.data.db

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * One planned task for a day. "Tomorrow's top 3" from the evening wrap-up is stored here under tomorrow's date (3 AM day change).
 * [refType] = TOPIC / CURRENT_AFFAIRS / CUSTOM, [refId] = the topic or affair id (null for custom). No foreign keys (like every user-data table).
 */
@Entity(tableName = "planned_task", indices = [Index("localDate")])
data class PlannedTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localDate: String,
    val title: String,
    val refType: String,
    val refId: String?,
    val done: Boolean,
    val createdAt: Long,
    val sortOrder: Int,
)

@Dao
interface PlannedTaskDao {
    @Insert suspend fun insert(t: PlannedTaskEntity): Long
    @Query("SELECT * FROM planned_task WHERE localDate = :date ORDER BY sortOrder, id") fun observe(date: String): Flow<List<PlannedTaskEntity>>
    @Query("SELECT * FROM planned_task WHERE localDate = :date ORDER BY sortOrder, id") suspend fun forDate(date: String): List<PlannedTaskEntity>
    @Query("UPDATE planned_task SET done = :done WHERE id = :id") suspend fun setDone(id: Long, done: Boolean)
    @Query("UPDATE planned_task SET sortOrder = :order WHERE id = :id") suspend fun setOrder(id: Long, order: Int)
    @Query("DELETE FROM planned_task WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM planned_task WHERE localDate = :date") suspend fun deleteForDate(date: String)
}

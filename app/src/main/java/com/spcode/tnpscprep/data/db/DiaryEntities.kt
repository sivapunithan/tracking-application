package com.spcode.tnpscprep.data.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

/** A diary entry. Several per day are allowed. [localDate] is the app's 3 AM-rollover day. No foreign keys (like every user-data table). */
@Entity(tableName = "diary_entry", indices = [Index("localDate")])
data class DiaryEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val localDate: String,
    val text: String,
    val moodEmojiId: String?,
    val createdAt: Long,
    val updatedAt: Long,
    /** Pinned entries are listed first in the timeline. Added in schema v11 (default 0 = not pinned). */
    @ColumnInfo(defaultValue = "0") val pinned: Boolean = false,
    /** Non-null = this entry is an Evening Wrap-up; holds its structured parts as JSON (see domain/Wrapup). Added in schema v12. */
    val wrapup: String? = null,
)

/** Light projection for the timeline: only the first 200 characters of each text are read. */
data class DiaryPreviewRow(val id: Long, val localDate: String, val moodEmojiId: String?, val preview: String, val pinned: Boolean = false, val isWrapup: Boolean = false)

@Dao
interface DiaryDao {
    @Insert suspend fun insert(e: DiaryEntryEntity): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertWithId(e: DiaryEntryEntity)
    @Update suspend fun update(e: DiaryEntryEntity)
    @Query("SELECT * FROM diary_entry WHERE id = :id") suspend fun get(id: Long): DiaryEntryEntity?
    @Query("DELETE FROM diary_entry WHERE id = :id") suspend fun delete(id: Long)
    @Query("SELECT * FROM diary_entry ORDER BY localDate, createdAt") suspend fun all(): List<DiaryEntryEntity>

    @Query("SELECT id, localDate, moodEmojiId, substr(text, 1, 200) AS preview, pinned, (wrapup IS NOT NULL) AS isWrapup FROM diary_entry ORDER BY localDate DESC, createdAt DESC")
    fun observePreviews(): Flow<List<DiaryPreviewRow>>

    // Plain substring match. ESCAPE '\' so a typed % or _ is literal. See domain/DiarySearch.
    @Query("SELECT id, localDate, moodEmojiId, substr(text, 1, 200) AS preview, pinned, (wrapup IS NOT NULL) AS isWrapup FROM diary_entry WHERE text LIKE :pattern ESCAPE '\\' ORDER BY localDate DESC, createdAt DESC LIMIT :limit")
    fun observeSearch(pattern: String, limit: Int): Flow<List<DiaryPreviewRow>>

    @Query("UPDATE diary_entry SET pinned = :pinned WHERE id = :id") suspend fun setPinned(id: Long, pinned: Boolean)

    @Query("SELECT * FROM diary_entry WHERE localDate = :date AND wrapup IS NOT NULL ORDER BY id LIMIT 1") suspend fun wrapupFor(date: String): DiaryEntryEntity?
    @Query("SELECT localDate FROM diary_entry WHERE wrapup IS NOT NULL AND localDate >= :from") fun observeWrapupDates(from: String): Flow<List<String>>

    @Query("SELECT * FROM diary_entry WHERE localDate = :date ORDER BY createdAt")
    fun observeForDate(date: String): Flow<List<DiaryEntryEntity>>
}

package com.spcode.tnpscprep.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SyllabusDao {
    // @Upsert = insert-or-update in place. Unlike REPLACE it never deletes the old row first,
    // so it can't trigger cascades.
    @Upsert suspend fun upsertSubjects(items: List<SubjectEntity>)
    @Upsert suspend fun upsertUnits(items: List<SyllabusUnitEntity>)
    @Upsert suspend fun upsertTopics(items: List<TopicEntity>)

    @Query("SELECT id FROM subject") suspend fun subjectIds(): List<String>
    @Query("SELECT id FROM unit") suspend fun unitIds(): List<String>
    @Query("SELECT id FROM topic") suspend fun topicIds(): List<String>

    @Query("DELETE FROM topic WHERE id IN (:ids)") suspend fun deleteTopics(ids: List<String>)
    @Query("DELETE FROM unit WHERE id IN (:ids)") suspend fun deleteUnits(ids: List<String>)
    @Query("DELETE FROM subject WHERE id IN (:ids)") suspend fun deleteSubjects(ids: List<String>)

    @Query("SELECT value FROM app_meta WHERE `key` = :key") suspend fun getMeta(key: String): String?
    @Upsert suspend fun putMeta(meta: AppMetaEntity)

    @Query("SELECT * FROM subject ORDER BY sortOrder") fun observeSubjects(): Flow<List<SubjectEntity>>
    @Query("SELECT * FROM unit ORDER BY subjectId, sortOrder") fun observeUnits(): Flow<List<SyllabusUnitEntity>>

    /** LEFT JOIN: topics with no progress row come back as level 0. mask = Exam bits; ALL_MASK = everything. */
    @Query(
        """SELECT t.id, t.subjectId, t.unitId, t.nameEn, t.nameTa, t.exams,
                  COALESCE(p.level, 0) AS level
           FROM topic t LEFT JOIN topic_progress p ON p.topicId = t.id
           WHERE (t.exams & :mask) != 0
           ORDER BY t.subjectId, t.unitId, t.sortOrder"""
    )
    fun observeTopics(mask: Int): Flow<List<TopicWithLevel>>
}

@Dao
interface ProgressDao {
    @Upsert suspend fun upsert(progress: TopicProgressEntity)
    @Query("SELECT level FROM topic_progress WHERE topicId = :topicId") suspend fun getLevel(topicId: String): Int?
    @Query("DELETE FROM topic_progress WHERE topicId = :topicId") suspend fun reset(topicId: String)
}

@Dao
interface SessionDao {
    @Insert suspend fun insert(session: StudySessionEntity)

    @Query("SELECT date, SUM(minutes) AS minutes FROM study_session GROUP BY date")
    fun observeDayTotals(): Flow<List<DayTotal>>
}

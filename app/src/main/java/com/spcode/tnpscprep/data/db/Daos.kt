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
                  COALESCE(p.level, 0) AS level,
                  COALESCE(n.starred, 0) AS starred,
                  CASE WHEN n.note IS NOT NULL AND n.note != '' THEN 1 ELSE 0 END AS hasNote
           FROM topic t
           LEFT JOIN topic_progress p ON p.topicId = t.id
           LEFT JOIN topic_note n ON n.topicId = t.id
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

    @Query(
        """SELECT p.topicId AS topicId, t.subjectId AS subjectId, t.nameEn AS nameEn, t.nameTa AS nameTa,
                  p.level AS level, p.lastUpdated AS lastUpdated
           FROM topic_progress p JOIN topic t ON t.id = p.topicId
           WHERE p.level > 0 AND (t.exams & :mask) != 0"""
    )
    fun observeStudied(mask: Int): Flow<List<StudiedTopic>>
}

@Dao
interface SessionDao {
    @Insert suspend fun insert(session: StudySessionEntity)

    @Query("SELECT date, SUM(minutes) AS minutes FROM study_session GROUP BY date")
    fun observeDayTotals(): Flow<List<DayTotal>>
}

@Dao
interface NoteDao {
    @Query("SELECT * FROM topic_note WHERE topicId = :topicId") suspend fun get(topicId: String): TopicNoteEntity?
    @Upsert suspend fun upsert(note: TopicNoteEntity)
    @Query("DELETE FROM topic_note WHERE topicId = :topicId") suspend fun delete(topicId: String)

    @Query(
        """SELECT t.id AS topicId, t.subjectId AS subjectId, t.nameEn AS nameEn, t.nameTa AS nameTa,
                  COALESCE(p.level, 0) AS level, n.note AS note
           FROM topic_note n
           JOIN topic t ON t.id = n.topicId
           LEFT JOIN topic_progress p ON p.topicId = t.id
           WHERE n.starred = 1 AND (t.exams & :mask) != 0
           ORDER BY t.subjectId, t.unitId, t.sortOrder"""
    )
    fun observeStarred(mask: Int): Flow<List<StarredTopic>>
}

@Dao
interface AffairDao {
    @Query("SELECT * FROM current_affair") fun observePack(): Flow<List<CurrentAffairEntity>>
    @Query("SELECT * FROM ca_user_item") fun observeUser(): Flow<List<UserAffairEntity>>
    @Query("SELECT * FROM ca_state") fun observeStates(): Flow<List<AffairStateEntity>>

    @Upsert suspend fun upsertPack(items: List<CurrentAffairEntity>)
    @Query("SELECT id FROM current_affair") suspend fun packIds(): List<String>
    @Query("DELETE FROM current_affair WHERE id IN (:ids)") suspend fun deletePack(ids: List<String>)

    @Insert suspend fun insertUser(item: UserAffairEntity): Long
    @Query("DELETE FROM ca_user_item WHERE id = :id") suspend fun deleteUser(id: Long)

    @Query("SELECT * FROM ca_state WHERE itemId = :itemId") suspend fun getState(itemId: String): AffairStateEntity?
    @Upsert suspend fun upsertState(state: AffairStateEntity)
    @Query("DELETE FROM ca_state WHERE itemId = :itemId") suspend fun deleteState(itemId: String)

    @Query("SELECT value FROM app_meta WHERE `key` = :key") suspend fun getMeta(key: String): String?
    @Query("SELECT value FROM app_meta WHERE `key` = :key") fun observeMeta(key: String): Flow<String?>
    @Upsert suspend fun putMeta(meta: AppMetaEntity)
}

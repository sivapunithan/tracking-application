package com.spcode.tnpscprep.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
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

    @Query("SELECT * FROM topic_progress") suspend fun all(): List<TopicProgressEntity>
}

@Dao
interface SessionDao {
    @Insert suspend fun insert(session: StudySessionEntity)

    @Query("SELECT date, SUM(minutes) AS minutes FROM study_session GROUP BY date")
    fun observeDayTotals(): Flow<List<DayTotal>>

    @Query("SELECT * FROM study_session ORDER BY startedAt") suspend fun all(): List<StudySessionEntity>
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

    @Query("SELECT * FROM topic_note") suspend fun all(): List<TopicNoteEntity>
}

@Dao
interface AffairDao {
    @Query("SELECT * FROM current_affair") fun observePack(): Flow<List<CurrentAffairEntity>>
    @Query("SELECT * FROM ca_user_item") fun observeUser(): Flow<List<UserAffairEntity>>
    @Query("SELECT * FROM ca_state") fun observeStates(): Flow<List<AffairStateEntity>>
    @Query("SELECT * FROM ca_state") suspend fun allStates(): List<AffairStateEntity>
    @Query("SELECT * FROM ca_user_item") suspend fun allUser(): List<UserAffairEntity>

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

@Dao
interface NewsDao {
    // IGNORE: an article we already have keeps its original row (and its fetch time).
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertAll(items: List<NewsItemEntity>)

    @Query("SELECT * FROM news_item WHERE publishedAt >= :since ORDER BY publishedAt DESC LIMIT 1500")
    fun observeSince(since: Long): Flow<List<NewsItemEntity>>

    /** Headlines she has revised at least once: needed for the Today "due for revision" list. */
    @Query("SELECT n.* FROM news_item n JOIN ca_state s ON s.itemId = 'news-' || n.id WHERE s.level > 0")
    fun observeRevised(): Flow<List<NewsItemEntity>>

    /** Retention: delete old headlines, but NEVER one she starred or revised. */
    @Query("DELETE FROM news_item WHERE publishedAt < :cutoff AND ('news-' || id) NOT IN (SELECT itemId FROM ca_state)")
    suspend fun prune(cutoff: Long): Int
}

@Dao
interface ActivityDao {
    @Insert suspend fun insert(event: ActivityEventEntity)

    @Query("SELECT * FROM activity_event WHERE localDate >= :from AND localDate <= :to AND type != 'HEALTH_LOG' ORDER BY timestamp")
    suspend fun between(from: String, to: String): List<ActivityEventEntity>

    @Query("SELECT * FROM activity_event WHERE localDate = :date AND type != 'HEALTH_LOG' ORDER BY timestamp")
    fun observeDay(date: String): Flow<List<ActivityEventEntity>>

    @Query("SELECT COUNT(*) FROM activity_event") suspend fun count(): Int

    @Query("SELECT * FROM activity_event WHERE type = :type AND localDate >= :from ORDER BY timestamp")
    fun observeByType(type: String, from: String): Flow<List<ActivityEventEntity>>

    @Query("DELETE FROM activity_event WHERE type = :type AND refId = :refId") suspend fun deleteByRef(type: String, refId: String)

    @Query("DELETE FROM activity_event WHERE type = :type") suspend fun deleteType(type: String)

    @Query("SELECT * FROM activity_event WHERE localDate >= :from AND type NOT IN ('HEALTH_LOG', 'STEPS', 'FOOD_CHECKIN', 'DIARY', 'MOOD_CHECKIN') ORDER BY timestamp")
    fun observeSince(from: String): Flow<List<ActivityEventEntity>>

    /** Backup export: everything except private health markers (HEALTH_LOG rows carry only a kind, and the readings go in their own section). */
    @Query("SELECT * FROM activity_event WHERE type != 'HEALTH_LOG' ORDER BY timestamp") suspend fun allForBackup(): List<ActivityEventEntity>
    @Query("SELECT * FROM day_note") suspend fun allNotes(): List<DayNoteEntity>

    @Query("SELECT * FROM day_note WHERE date = :date")
    fun observeNote(date: String): Flow<DayNoteEntity?>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE) suspend fun putNote(n: DayNoteEntity)

    @Query("DELETE FROM day_note WHERE date = :date") suspend fun deleteNote(date: String)

    @Query("SELECT date FROM day_note WHERE date >= :from")
    fun observeNoteDates(from: String): Flow<List<String>>
}

@Dao
interface HealthDao {
    @Insert suspend fun insert(e: HealthEntryEntity)
    @Query("SELECT * FROM health_entry ORDER BY date DESC, id DESC") fun observeAll(): Flow<List<HealthEntryEntity>>
    @Query("SELECT * FROM health_entry ORDER BY date, id") suspend fun all(): List<HealthEntryEntity>
    @Query("DELETE FROM health_entry WHERE id = :id") suspend fun delete(id: Long)
    @Query("DELETE FROM health_entry") suspend fun deleteAll()
}

@Dao
interface StepDao {
    @Query("SELECT * FROM step_day ORDER BY date DESC LIMIT 1") suspend fun latest(): StepDayEntity?
    @Query("SELECT * FROM step_day ORDER BY date") suspend fun all(): List<StepDayEntity>
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE) suspend fun upsert(e: StepDayEntity)
    @Query("SELECT * FROM step_day WHERE date >= :from ORDER BY date") fun observeSince(from: String): Flow<List<StepDayEntity>>
}

@Dao
interface UsageDao {
    @Query("DELETE FROM usage_day WHERE date = :date") suspend fun deleteDay(date: String)
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE) suspend fun insertApps(rows: List<UsageDayEntity>)
    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE) suspend fun upsertTotal(t: UsageTotalEntity)
    @Query("SELECT * FROM usage_day WHERE date >= :from ORDER BY date, minutes DESC") fun observeApps(from: String): Flow<List<UsageDayEntity>>
    @Query("SELECT * FROM usage_total WHERE date >= :from ORDER BY date") fun observeTotals(from: String): Flow<List<UsageTotalEntity>>
    @Query("DELETE FROM usage_day WHERE date < :before") suspend fun pruneApps(before: String)
    @Query("DELETE FROM usage_total WHERE date < :before") suspend fun pruneTotals(before: String)
    @Query("DELETE FROM usage_day") suspend fun deleteAllApps()
    @Query("DELETE FROM usage_total") suspend fun deleteAllTotals()
}

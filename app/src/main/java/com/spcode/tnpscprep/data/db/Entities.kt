package com.spcode.tnpscprep.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ---- SYLLABUS tables: replaceable by a re-seed. IDs are stable strings from syllabus.json. ----

@Entity(tableName = "subject")
data class SubjectEntity(
    @PrimaryKey val id: String,
    val nameEn: String,
    val nameTa: String,
    val sortOrder: Int,
)

// Named SyllabusUnit (not Unit) to avoid clashing with kotlin.Unit.
@Entity(
    tableName = "unit",
    foreignKeys = [ForeignKey(SubjectEntity::class, ["id"], ["subjectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("subjectId")],
)
data class SyllabusUnitEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val nameEn: String,
    val nameTa: String,
    val sortOrder: Int,
)

@Entity(
    tableName = "topic",
    foreignKeys = [ForeignKey(SyllabusUnitEntity::class, ["id"], ["unitId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("unitId"), Index("subjectId")],
)
data class TopicEntity(
    @PrimaryKey val id: String,
    val subjectId: String,
    val unitId: String,
    val nameEn: String,
    val nameTa: String,
    /** Bitmask, see [com.spcode.tnpscprep.domain.Exam]. */
    val exams: Int,
    val sortOrder: Int,
)

// ---- USER data: never touched by a re-seed ----

/**
 * Deliberately NO foreign key to `topic`: a re-seed (or any cascade) can never delete or reset this.
 * A row for a topic that a newer syllabus removed simply becomes an unused orphan.
 * No row means level 0 (Not started); long-press reset deletes the row.
 */
@Entity(tableName = "topic_progress")
data class TopicProgressEntity(
    @PrimaryKey val topicId: String,
    val level: Int,
    val lastUpdated: Long,
)

@Entity(tableName = "study_session", indices = [Index("date")])
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Local date, ISO yyyy-MM-dd, so "which day" is decided once when logged, not by later timezone changes. */
    val date: String,
    val startedAt: Long,
    val minutes: Int,
    /** Optional tag; plain column, no FK, so history survives syllabus changes. */
    val subjectId: String?,
)

/**
 * Her own notes + "weak topic" star. Like topic_progress: NO foreign key to `topic`, so a syllabus
 * update can never delete what she wrote. A row exists only if there is a note or a star.
 */
@Entity(tableName = "topic_note")
data class TopicNoteEntity(
    @PrimaryKey val topicId: String,
    val note: String,
    val starred: Boolean,
    val updatedAt: Long,
)

@Entity(tableName = "app_meta")
data class AppMetaEntity(
    @PrimaryKey val key: String,
    val value: String,
)

/** Query result (not a table): a topic joined with its progress level. */
data class TopicWithLevel(
    val id: String,
    val subjectId: String,
    val unitId: String,
    val nameEn: String,
    val nameTa: String,
    val exams: Int,
    val level: Int,
    val starred: Boolean = false,
    val hasNote: Boolean = false,
)

data class DayTotal(val date: String, val minutes: Int)

/** A topic that has been started (level > 0) with when it was last touched: input for the revision schedule. */
data class StudiedTopic(
    val topicId: String,
    val subjectId: String,
    val nameEn: String,
    val nameTa: String,
    val level: Int,
    val lastUpdated: Long,
)

data class StarredTopic(
    val topicId: String,
    val subjectId: String,
    val nameEn: String,
    val nameTa: String,
    val level: Int,
    val note: String,
)

// ---- CURRENT AFFAIRS ----

/** Items shipped in assets/current_affairs.json. Replaceable by a newer pack (see AffairSeeder). */
@Entity(tableName = "current_affair", indices = [Index("date")])
data class CurrentAffairEntity(
    @PrimaryKey val id: String,
    /** ISO yyyy-MM-dd */
    val date: String,
    val category: String,
    val titleEn: String,
    val titleTa: String,
    val summaryEn: String,
    val summaryTa: String,
    val sourceName: String,
    val url: String,
    val verified: Boolean,
)

/** Entries she writes herself. Never touched by a pack update. */
@Entity(tableName = "ca_user_item", indices = [Index("date")])
data class UserAffairEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    val category: String,
    val title: String,
    val summary: String,
    val createdAt: Long,
)

/** Bookmark + revision level for ANY item (pack id, or "user-<n>"). No FK, like topic_progress. */
@Entity(tableName = "ca_state")
data class AffairStateEntity(
    @PrimaryKey val itemId: String,
    val bookmarked: Boolean,
    val level: Int,
    val lastUpdated: Long,
)

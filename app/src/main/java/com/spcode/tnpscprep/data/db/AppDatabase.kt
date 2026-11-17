package com.spcode.tnpscprep.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

// exportSchema = true writes app/schemas/*.json: the baseline for writing migrations later, so a
// schema change never forces the user to lose her progress.
@Database(
    entities = [
        SubjectEntity::class, SyllabusUnitEntity::class, TopicEntity::class,
        TopicProgressEntity::class, StudySessionEntity::class, AppMetaEntity::class, TopicNoteEntity::class,
        CurrentAffairEntity::class, UserAffairEntity::class, AffairStateEntity::class, NewsItemEntity::class, ActivityEventEntity::class, DayNoteEntity::class, HealthEntryEntity::class, StepDayEntity::class, UsageDayEntity::class, UsageTotalEntity::class, DiaryEntryEntity::class,
    ],
    version = 10,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun syllabusDao(): SyllabusDao
    abstract fun progressDao(): ProgressDao
    abstract fun sessionDao(): SessionDao
    abstract fun noteDao(): NoteDao
    abstract fun affairDao(): AffairDao
    abstract fun newsDao(): NewsDao
    abstract fun activityDao(): ActivityDao
    abstract fun healthDao(): HealthDao
    abstract fun stepDao(): StepDao
    abstract fun usageDao(): UsageDao
    abstract fun diaryDao(): DiaryDao
}

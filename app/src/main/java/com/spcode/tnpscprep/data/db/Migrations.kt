package com.spcode.tnpscprep.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Migrations keep the user's data when the schema changes. NEVER use fallbackToDestructiveMigration:
 * it would wipe her progress and notes. Rule: every schema change = version bump + a Migration here +
 * the new app/schemas/<n>.json committed.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `topic_note` (`topicId` TEXT NOT NULL, `note` TEXT NOT NULL, " +
                "`starred` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`topicId`))"
        )
    }
}


val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `current_affair` (`id` TEXT NOT NULL, `date` TEXT NOT NULL, `category` TEXT NOT NULL, `titleEn` TEXT NOT NULL, `titleTa` TEXT NOT NULL, `summaryEn` TEXT NOT NULL, `summaryTa` TEXT NOT NULL, `sourceName` TEXT NOT NULL, `url` TEXT NOT NULL, `verified` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_current_affair_date` ON `current_affair` (`date`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `ca_user_item` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `category` TEXT NOT NULL, `title` TEXT NOT NULL, `summary` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_ca_user_item_date` ON `ca_user_item` (`date`)")
        db.execSQL("CREATE TABLE IF NOT EXISTS `ca_state` (`itemId` TEXT NOT NULL, `bookmarked` INTEGER NOT NULL, `level` INTEGER NOT NULL, `lastUpdated` INTEGER NOT NULL, PRIMARY KEY(`itemId`))")
    }
}


val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `news_item` (`id` TEXT NOT NULL, `feedId` TEXT NOT NULL, `source` TEXT NOT NULL, `title` TEXT NOT NULL, `url` TEXT NOT NULL, `publishedAt` INTEGER NOT NULL, `category` TEXT NOT NULL, `language` TEXT NOT NULL, `fetchedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_news_item_publishedAt` ON `news_item` (`publishedAt`)")
    }
}


/**
 * 4 -> 5: create the activity log and BACKFILL it from what she already has, so her history is not lost:
 * every logged focus session, and one event per topic at its last-touched time (older topic history was never stored).
 * Backfilled rows carry "backfilled":true. 'localtime' converts with the device zone.
 */
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `activity_event` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `timestamp` INTEGER NOT NULL, `localDate` TEXT NOT NULL, `type` TEXT NOT NULL, `refId` TEXT, `details` TEXT NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_event_localDate` ON `activity_event` (`localDate`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_activity_event_type` ON `activity_event` (`type`)")
        db.execSQL(
            "INSERT INTO activity_event (timestamp, localDate, type, refId, details) " +
                "SELECT startedAt, date, 'FOCUS_SESSION', subjectId, '{\"minutes\":' || minutes || ',\"backfilled\":true}' FROM study_session"
        )
        db.execSQL(
            "INSERT INTO activity_event (timestamp, localDate, type, refId, details) " +
                "SELECT lastUpdated, date(lastUpdated / 1000, 'unixepoch', 'localtime'), 'TOPIC_PROGRESS', topicId, " +
                "'{\"to\":' || level || ',\"backfilled\":true}' FROM topic_progress WHERE level > 0"
        )
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `day_note` (`date` TEXT NOT NULL, `note` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`date`))")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `health_entry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `date` TEXT NOT NULL, `kind` TEXT NOT NULL, `value` REAL NOT NULL, `createdAt` INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_health_entry_date` ON `health_entry` (`date`)")
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `step_day` (`date` TEXT NOT NULL, `baseline` INTEGER NOT NULL, `lastReading` INTEGER NOT NULL, `steps` INTEGER NOT NULL, `bootMs` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`date`))")
    }
}

val MIGRATION_8_9 = object : Migration(8, 9) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `usage_day` (`date` TEXT NOT NULL, `packageName` TEXT NOT NULL, `label` TEXT NOT NULL, `minutes` INTEGER NOT NULL, PRIMARY KEY(`date`, `packageName`))")
        db.execSQL("CREATE TABLE IF NOT EXISTS `usage_total` (`date` TEXT NOT NULL, `minutes` INTEGER NOT NULL, PRIMARY KEY(`date`))")
    }
}

val MIGRATION_9_10 = object : Migration(9, 10) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `diary_entry` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `localDate` TEXT NOT NULL, `text` TEXT NOT NULL, `moodEmojiId` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_diary_entry_localDate` ON `diary_entry` (`localDate`)")
    }
}

val MIGRATION_10_11 = object : Migration(10, 11) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // diary pin: a plain extra column with a default, so every existing entry stays as it was (unpinned)
        db.execSQL("ALTER TABLE `diary_entry` ADD COLUMN `pinned` INTEGER NOT NULL DEFAULT 0")
    }
}

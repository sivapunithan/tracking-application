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

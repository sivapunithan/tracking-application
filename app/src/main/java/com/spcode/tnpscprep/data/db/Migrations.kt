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

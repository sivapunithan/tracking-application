package com.spcode.tnpscprep

import android.content.Context
import androidx.room.Room
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.MIGRATION_1_2
import com.spcode.tnpscprep.data.db.MIGRATION_2_3
import com.spcode.tnpscprep.data.seed.AffairSeeder
import com.spcode.tnpscprep.data.seed.RoomAffairStore
import com.spcode.tnpscprep.data.seed.RoomSyllabusStore
import com.spcode.tnpscprep.data.seed.SeedResult
import com.spcode.tnpscprep.data.seed.SyllabusSeeder

/**
 * Manual dependency container (what Spring's @Configuration does, minus the framework).
 * One instance, owned by [TnpscApp]. `by lazy` = created on first use.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "tnpscprep.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .build()
    }

    val syllabusRepository by lazy { com.spcode.tnpscprep.data.SyllabusRepository(database) }

    val sessionRepository by lazy { com.spcode.tnpscprep.data.SessionRepository(database) }
    val settingsStore by lazy { com.spcode.tnpscprep.data.SettingsStore(appContext) }

    val affairRepository by lazy { com.spcode.tnpscprep.data.AffairRepository(database) }

    private val seeder by lazy { SyllabusSeeder(RoomSyllabusStore(database)) }
    private val affairSeeder by lazy { AffairSeeder(RoomAffairStore(database)) }

    suspend fun seedSyllabus(): SeedResult {
        val text = appContext.assets.open("syllabus.json").bufferedReader().use { it.readText() }
        return seeder.seedIfNeeded(text)
    }

    suspend fun seedAffairs(): SeedResult {
        val text = appContext.assets.open("current_affairs.json").bufferedReader().use { it.readText() }
        return affairSeeder.seedIfNeeded(text)
    }
}

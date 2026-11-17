package com.spcode.tnpscprep

import android.content.Context
import androidx.room.Room
import com.spcode.tnpscprep.data.db.AppDatabase
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
        Room.databaseBuilder(appContext, AppDatabase::class.java, "tnpscprep.db").build()
    }

    val syllabusRepository by lazy { com.spcode.tnpscprep.data.SyllabusRepository(database) }

    val sessionRepository by lazy { com.spcode.tnpscprep.data.SessionRepository(database) }
    val settingsStore by lazy { com.spcode.tnpscprep.data.SettingsStore(appContext) }

    private val seeder by lazy { SyllabusSeeder(RoomSyllabusStore(database)) }

    suspend fun seedSyllabus(): SeedResult {
        val text = appContext.assets.open("syllabus.json").bufferedReader().use { it.readText() }
        return seeder.seedIfNeeded(text)
    }
}

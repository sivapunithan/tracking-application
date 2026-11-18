package com.spcode.tnpscprep

import android.content.Context
import androidx.room.Room
import com.spcode.tnpscprep.data.NewsRepository
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.MIGRATION_1_2
import com.spcode.tnpscprep.data.db.MIGRATION_2_3
import com.spcode.tnpscprep.data.db.MIGRATION_3_4
import com.spcode.tnpscprep.data.db.MIGRATION_4_5
import com.spcode.tnpscprep.data.db.MIGRATION_5_6
import com.spcode.tnpscprep.data.db.MIGRATION_6_7
import com.spcode.tnpscprep.data.db.MIGRATION_7_8
import com.spcode.tnpscprep.data.db.MIGRATION_8_9
import com.spcode.tnpscprep.data.db.MIGRATION_9_10
import com.spcode.tnpscprep.data.db.MIGRATION_10_11
import com.spcode.tnpscprep.data.db.MIGRATION_11_12
import com.spcode.tnpscprep.data.db.MIGRATION_12_13
import com.spcode.tnpscprep.data.news.FeedListDto
import com.spcode.tnpscprep.data.news.FeedPolicy
import com.spcode.tnpscprep.data.news.HttpFeedSource
import com.spcode.tnpscprep.data.seed.AffairSeeder
import com.spcode.tnpscprep.data.seed.RoomAffairStore
import com.spcode.tnpscprep.data.seed.RoomSyllabusStore
import com.spcode.tnpscprep.data.seed.SeedResult
import com.spcode.tnpscprep.data.seed.SyllabusSeeder
import kotlinx.serialization.json.Json

/**
 * Manual dependency container (what Spring's @Configuration does, minus the framework).
 * One instance, owned by [TnpscApp]. `by lazy` = created on first use.
 */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase by lazy {
        Room.databaseBuilder(appContext, AppDatabase::class.java, "tnpscprep.db")
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
            .build()
    }

    val syllabusRepository by lazy { com.spcode.tnpscprep.data.SyllabusRepository(database) }

    val sessionRepository by lazy { com.spcode.tnpscprep.data.SessionRepository(database) }
    val settingsStore by lazy { com.spcode.tnpscprep.data.SettingsStore(appContext) }

    val healthRepository by lazy { com.spcode.tnpscprep.data.HealthRepository(database) }
    val stepRepository by lazy { com.spcode.tnpscprep.data.StepRepository(database) }
    val usageRepository by lazy { com.spcode.tnpscprep.data.UsageRepository(database, appContext) }
    val foodRepository by lazy { com.spcode.tnpscprep.data.FoodRepository(database) }
    val moodRepository by lazy { com.spcode.tnpscprep.data.MoodRepository(database) }
    val plannedTaskRepository by lazy { com.spcode.tnpscprep.data.PlannedTaskRepository(database) }
    val photoStore by lazy { com.spcode.tnpscprep.data.PhotoStore(appContext) }
    val diaryRepository by lazy { com.spcode.tnpscprep.data.DiaryRepository(database) }
    val backupRepository by lazy { com.spcode.tnpscprep.data.BackupRepository(database, settingsStore, diaryRepository) }
    val activityRepository by lazy { com.spcode.tnpscprep.data.ActivityRepository(database) }

    val affairRepository by lazy { com.spcode.tnpscprep.data.AffairRepository(database) }

    /** Feeds come from assets/feeds.json; the host of each feed url is the network allow-list. */
    val newsRepository by lazy {
        val feeds = Json { ignoreUnknownKeys = true }
            .decodeFromString<FeedListDto>(appContext.assets.open("feeds.json").bufferedReader().use { it.readText() }).feeds
        val hosts = feeds.mapNotNull { FeedPolicy.hostOf(it.url) }.toSet()
        NewsRepository(database, HttpFeedSource(hosts), feeds, settingsStore)
    }

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

    /**
     * "Delete account": wipe the database (progress, notes, sessions, bookmarks, headlines) and every setting,
     * then load the syllabus and current-affairs packs again so the app is usable as on first launch.
     */
    suspend fun resetEverything() {
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { database.clearAllTables() }
        settingsStore.clearAll()
        com.spcode.tnpscprep.reminder.ReminderScheduler.cancel(appContext)
        seedSyllabus()
        seedAffairs()
    }
}

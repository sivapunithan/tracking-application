package com.spcode.tnpscprep.data

import androidx.room.withTransaction
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.domain.ActivityType
import com.spcode.tnpscprep.domain.FoodState
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive

/** Food check-ins live ONLY in activity_event (one FOOD_CHECKIN row per food day). Streaks etc. are derived in domain/FoodCheckin. */
class FoodRepository(private val db: AppDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = db.activityDao()
    private val value = Regex("\"value\"\\s*:\\s*\"(\\w+)\"")

    fun observe(from: LocalDate): Flow<Map<LocalDate, FoodState>> =
        dao.observeByType(ActivityType.FOOD_CHECKIN.name, from.toString()).map { rows ->
            buildMap {
                rows.forEach { e ->
                    val d = runCatching { LocalDate.parse(e.localDate) }.getOrNull() ?: return@forEach
                    val s = value.find(e.details)?.groupValues?.get(1)?.let { n -> FoodState.entries.firstOrNull { it.name == n } } ?: return@forEach
                    put(d, s) // rows are ordered by time, so the latest wins
                }
            }
        }

    /** Sets (or, with null, removes) the check-in of [day]. Logging again REPLACES the old row in the same transaction. */
    suspend fun set(day: LocalDate, state: FoodState?) = db.withTransaction {
        dao.deleteByRef(ActivityType.FOOD_CHECKIN.name, day.toString())
        if (state != null) {
            dao.insert(ActivityEvents.build(ActivityType.FOOD_CHECKIN, day.toString(), now(), localDateOverride = day.toString()) { put("value", JsonPrimitive(state.name)) })
        }
    }
}

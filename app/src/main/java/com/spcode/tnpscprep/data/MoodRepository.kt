package com.spcode.tnpscprep.data

import androidx.room.withTransaction
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.domain.ActivityType
import com.spcode.tnpscprep.domain.Mood
import com.spcode.tnpscprep.domain.MoodCheckins
import com.spcode.tnpscprep.domain.MoodRow
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive

/** Daily mood check-ins live ONLY in activity_event (one MOOD_CHECKIN row per logical day), exactly like the food check-in: no schema change. */
class MoodRepository(private val db: AppDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = db.activityDao()

    fun observe(from: LocalDate): Flow<Map<LocalDate, Mood>> =
        dao.observeByType(ActivityType.MOOD_CHECKIN.name, from.toString()).map { rows -> MoodCheckins.parse(rows.map { MoodRow(it.localDate, it.details) }) }

    /** Sets (or, with null, removes) the mood of [day]. Logging again REPLACES the old row in the same transaction. */
    suspend fun set(day: LocalDate, mood: Mood?) = db.withTransaction {
        dao.deleteByRef(ActivityType.MOOD_CHECKIN.name, day.toString())
        if (mood != null) dao.insert(ActivityEvents.build(ActivityType.MOOD_CHECKIN, day.toString(), now(), localDateOverride = day.toString()) { put("value", JsonPrimitive(mood.name)) })
    }
}

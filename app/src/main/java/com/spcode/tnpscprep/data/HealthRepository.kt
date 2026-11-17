package com.spcode.tnpscprep.data

import androidx.room.withTransaction
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.HealthEntryEntity
import com.spcode.tnpscprep.domain.ActivityType
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

/** Private health log. Stays on the phone; the activity event stores only the KIND (never the value). */
class HealthRepository(private val db: AppDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = db.healthDao()

    fun observeAll(): Flow<List<HealthEntryEntity>> = dao.observeAll()

    suspend fun add(kind: String, value: Double, date: LocalDate) = db.withTransaction {
        dao.insert(HealthEntryEntity(date = date.toString(), kind = kind, value = value, createdAt = now()))
        db.activityDao().insert(ActivityEvents.build(ActivityType.HEALTH_LOG, null, now()) { put("kind", kotlinx.serialization.json.JsonPrimitive(kind)) })
    }

    suspend fun delete(id: Long) = dao.delete(id)

    /** Erases every reading and every HEALTH_LOG event. */
    suspend fun deleteAll() = db.withTransaction {
        dao.deleteAll()
        db.activityDao().deleteType(ActivityType.HEALTH_LOG.name)
    }
}

object HealthKind {
    const val WEIGHT = "WEIGHT_KG"; const val WAIST = "WAIST_CM"; const val HIP = "HIP_CM"
    const val WATER = "WATER_ML"; const val PERIOD = "PERIOD_START"
}

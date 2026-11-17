package com.spcode.tnpscprep.data

import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.DayTotal
import com.spcode.tnpscprep.data.db.StudySessionEntity
import androidx.room.withTransaction
import com.spcode.tnpscprep.domain.ActivityType
import java.time.LocalDate
import kotlinx.serialization.json.put
import kotlinx.coroutines.flow.Flow

class SessionRepository(private val db: AppDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = db.sessionDao()

    /** [date] is decided by the caller when the session finishes and stored as ISO text. */
    suspend fun logFocus(minutes: Int, subjectId: String?, startedAtEpochMs: Long, date: LocalDate) = db.withTransaction {
        dao.insert(StudySessionEntity(date = date.toString(), startedAt = startedAtEpochMs, minutes = minutes, subjectId = subjectId))
        db.activityDao().insert(ActivityEvents.build(ActivityType.FOCUS_SESSION, subjectId, now()) { put("minutes", minutes) })
    }

    fun observeDayTotals(): Flow<List<DayTotal>> = dao.observeDayTotals()
}

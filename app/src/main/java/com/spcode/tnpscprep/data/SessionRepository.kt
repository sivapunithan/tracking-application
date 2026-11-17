package com.spcode.tnpscprep.data

import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.DayTotal
import com.spcode.tnpscprep.data.db.StudySessionEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class SessionRepository(db: AppDatabase) {
    private val dao = db.sessionDao()

    /** [date] is decided by the caller when the session finishes and stored as ISO text. */
    suspend fun logFocus(minutes: Int, subjectId: String?, startedAtEpochMs: Long, date: LocalDate) =
        dao.insert(StudySessionEntity(date = date.toString(), startedAt = startedAtEpochMs, minutes = minutes, subjectId = subjectId))

    fun observeDayTotals(): Flow<List<DayTotal>> = dao.observeDayTotals()
}

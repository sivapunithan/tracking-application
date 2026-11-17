package com.spcode.tnpscprep.data

import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.DayNoteEntity
import com.spcode.tnpscprep.domain.HeatEvent
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Read side of the activity log (heatmap, day sheet) plus her optional one-line note per day. */
class ActivityRepository(db: AppDatabase) {
    private val dao = db.activityDao()

    fun observeHeatEvents(from: LocalDate): Flow<List<HeatEvent>> =
        dao.observeSince(from.toString()).map { l -> l.map { HeatEvent(it.localDate, it.type, it.details) } }

    fun observeDay(date: LocalDate) = dao.observeDay(date.toString())
    fun observeNote(date: LocalDate): Flow<String> = dao.observeNote(date.toString()).map { it?.note.orEmpty() }
    fun observeNoteDates(from: LocalDate): Flow<Set<LocalDate>> =
        dao.observeNoteDates(from.toString()).map { l -> l.mapNotNull { runCatching { LocalDate.parse(it) }.getOrNull() }.toSet() }

    suspend fun saveNote(date: LocalDate, text: String) {
        val t = text.trim().take(200)
        if (t.isEmpty()) dao.deleteNote(date.toString()) else dao.putNote(DayNoteEntity(date.toString(), t, System.currentTimeMillis()))
    }
}

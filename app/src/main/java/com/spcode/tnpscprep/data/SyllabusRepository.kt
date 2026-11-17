package com.spcode.tnpscprep.data

import androidx.room.withTransaction
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.TopicNoteEntity
import com.spcode.tnpscprep.data.db.TopicProgressEntity
import com.spcode.tnpscprep.domain.RevisionLevel
import kotlinx.coroutines.flow.Flow

/** What the UI talks to (Spring: the service layer over DAOs). */
class SyllabusRepository(
    private val db: AppDatabase,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private val syllabus = db.syllabusDao()
    private val progress = db.progressDao()
    private val notes = db.noteDao()

    fun observeSubjects() = syllabus.observeSubjects()
    fun observeUnits() = syllabus.observeUnits()
    fun observeTopics(mask: Int) = syllabus.observeTopics(mask)
    fun observeStudied(mask: Int) = progress.observeStudied(mask)
    fun observeStarred(mask: Int) = notes.observeStarred(mask)

    suspend fun getNote(topicId: String): String = notes.get(topicId)?.note.orEmpty()

    suspend fun toggleStar(topicId: String) = db.withTransaction {
        val cur = notes.get(topicId)
        writeNote(topicId, cur?.note.orEmpty(), starred = !(cur?.starred ?: false))
    }

    suspend fun saveNote(topicId: String, text: String) = db.withTransaction {
        writeNote(topicId, text.trim(), starred = notes.get(topicId)?.starred ?: false)
    }

    /** A row exists only while there is something to keep (a note or a star). */
    private suspend fun writeNote(topicId: String, note: String, starred: Boolean) {
        if (note.isEmpty() && !starred) notes.delete(topicId)
        else notes.upsert(TopicNoteEntity(topicId, note, starred, now()))
    }

    /**
     * Also used for "I revised this again": at the top level the level can't rise, but the timestamp is
     * refreshed so the 60-day review restarts. Reading the CURRENT level from the DB inside a transaction instead of trusting the level the
     * UI showed: two fast taps then advance twice, never "twice from the same stale value".
     */
    suspend fun advance(topicId: String) = db.withTransaction {
        val current = progress.getLevel(topicId) ?: RevisionLevel.NOT_STARTED
        val next = RevisionLevel.advance(current)
        progress.upsert(TopicProgressEntity(topicId, next, now()))
    }

    /** Long-press: delete the row (no row == Not started). */
    suspend fun reset(topicId: String) = progress.reset(topicId)
}

package com.spcode.tnpscprep.data.seed

import androidx.room.withTransaction
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.AppMetaEntity

class RoomSyllabusStore(private val db: AppDatabase) : SyllabusStore {
    private val dao = db.syllabusDao()

    override suspend fun storedVersion(): Int = dao.getMeta(KEY_VERSION)?.toIntOrNull() ?: 0

    /**
     * One transaction: upsert new/changed rows, delete syllabus rows the new file no longer has,
     * record the version. topic_progress is not mentioned anywhere here.
     * Deletes are chunked: older Android SQLite allows only 999 bound variables per statement.
     */
    override suspend fun replace(version: Int, data: SyllabusData) = db.withTransaction {
        dao.upsertSubjects(data.subjects)
        dao.upsertUnits(data.units)
        dao.upsertTopics(data.topics)

        (dao.topicIds() - data.topics.map { it.id }.toSet()).chunked(500).forEach { dao.deleteTopics(it) }
        (dao.unitIds() - data.units.map { it.id }.toSet()).chunked(500).forEach { dao.deleteUnits(it) }
        (dao.subjectIds() - data.subjects.map { it.id }.toSet()).chunked(500).forEach { dao.deleteSubjects(it) }

        dao.putMeta(AppMetaEntity(KEY_VERSION, version.toString()))
    }

    private companion object { const val KEY_VERSION = "syllabus_version" }
}

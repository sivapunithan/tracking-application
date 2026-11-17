package com.spcode.tnpscprep.data.seed

import com.spcode.tnpscprep.data.db.SubjectEntity
import com.spcode.tnpscprep.data.db.SyllabusUnitEntity
import com.spcode.tnpscprep.data.db.TopicEntity
import com.spcode.tnpscprep.domain.Exam
import kotlinx.serialization.json.Json

/** The syllabus tables' content, already converted to entities. Contains NO progress by design. */
data class SyllabusData(
    val subjects: List<SubjectEntity>,
    val units: List<SyllabusUnitEntity>,
    val topics: List<TopicEntity>,
)

/**
 * The seeder's only view of storage. It has no method that can touch topic_progress: so
 * re-seeding structurally cannot wipe progress. (Interface = Spring's repository abstraction;
 * it also lets unit tests run on the JVM without an Android database.)
 */
interface SyllabusStore {
    suspend fun storedVersion(): Int
    /** Must apply everything atomically (all or nothing) and record [version]. */
    suspend fun replace(version: Int, data: SyllabusData)
}

enum class SeedResult { SEEDED, UP_TO_DATE }

class SyllabusSeeder(
    private val store: SyllabusStore,
    private val json: Json = Json { ignoreUnknownKeys = true },
) {
    /**
     * Parse + validate FIRST (a bad file must fail before touching the DB), then re-seed only if
     * the file's version is greater than the stored one.
     */
    suspend fun seedIfNeeded(jsonText: String): SeedResult {
        val dto = json.decodeFromString<SyllabusDto>(jsonText)
        val data = toData(dto)
        if (dto.version <= store.storedVersion()) return SeedResult.UP_TO_DATE
        store.replace(dto.version, data)
        return SeedResult.SEEDED
    }

    companion object {
        /** Public for tests. Fails loudly on typos so bad data from the PDF conversion can't slip in silently. */
        fun toData(dto: SyllabusDto): SyllabusData {
            require(dto.version >= 1) { "syllabus version must be >= 1" }
            val subjects = ArrayList<SubjectEntity>()
            val units = ArrayList<SyllabusUnitEntity>()
            val topics = ArrayList<TopicEntity>()
            dto.subjects.forEachIndexed { si, s ->
                subjects += SubjectEntity(s.id, s.nameEn, s.nameTa, si)
                s.units.forEachIndexed { ui, u ->
                    units += SyllabusUnitEntity(u.id, s.id, u.nameEn, u.nameTa, ui)
                    u.topics.forEachIndexed { ti, t ->
                        require(t.exams.isNotEmpty()) { "topic '${t.id}' has no exams" }
                        topics += TopicEntity(t.id, s.id, u.id, t.nameEn, t.nameTa, Exam.maskOf(t.exams), ti)
                    }
                }
            }
            requireUnique("subject", subjects.map { it.id })
            requireUnique("unit", units.map { it.id })
            requireUnique("topic", topics.map { it.id })
            return SyllabusData(subjects, units, topics)
        }

        private fun requireUnique(kind: String, ids: List<String>) {
            require(ids.none { it.isBlank() }) { "blank $kind id" }
            val dup = ids.groupBy { it }.filterValues { it.size > 1 }.keys
            require(dup.isEmpty()) { "duplicate $kind id(s): $dup" }
        }
    }
}

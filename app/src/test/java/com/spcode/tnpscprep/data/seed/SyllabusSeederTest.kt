package com.spcode.tnpscprep.data.seed

import java.io.File
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.SerializationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/** In-memory stand-in for Room, so these run on the JVM in milliseconds. */
private class FakeStore(var version: Int = 0) : SyllabusStore {
    var data: SyllabusData? = null
    var replaceCalls = 0
    override suspend fun storedVersion() = version
    override suspend fun replace(version: Int, data: SyllabusData) {
        replaceCalls++
        this.version = version
        this.data = data
    }
}

class SyllabusSeederTest {
    private fun syllabus(version: Int, vararg topics: String, exams: String = """["G1","G2"]""") = """
        {"version": $version, "subjects": [{"id":"s","nameEn":"S","nameTa":"S","units":[
          {"id":"u","nameEn":"U","nameTa":"U","topics":[
            ${topics.joinToString(",") { """{"id":"$it","nameEn":"$it","nameTa":"$it","exams":$exams}""" }}
          ]}]}]}
    """.trimIndent()

    @Test fun first_launch_seeds_everything() = runBlocking {
        val store = FakeStore()
        assertEquals(SeedResult.SEEDED, SyllabusSeeder(store).seedIfNeeded(syllabus(1, "a", "b")))
        assertEquals(2, store.data!!.topics.size)
        assertEquals(1, store.version)
    }

    @Test fun same_version_is_a_no_op() = runBlocking {
        val store = FakeStore()
        val seeder = SyllabusSeeder(store)
        seeder.seedIfNeeded(syllabus(1, "a"))
        assertEquals(SeedResult.UP_TO_DATE, seeder.seedIfNeeded(syllabus(1, "a", "b")))
        assertEquals(1, store.replaceCalls)
        assertEquals(1, store.data!!.topics.size) // unchanged
    }

    @Test fun older_file_never_downgrades() = runBlocking {
        val store = FakeStore(version = 5)
        assertEquals(SeedResult.UP_TO_DATE, SyllabusSeeder(store).seedIfNeeded(syllabus(3, "a")))
        assertEquals(0, store.replaceCalls)
    }

    @Test fun higher_version_reseeds_with_added_and_removed_topics() = runBlocking {
        val store = FakeStore()
        val seeder = SyllabusSeeder(store)
        seeder.seedIfNeeded(syllabus(1, "a", "b"))
        assertEquals(SeedResult.SEEDED, seeder.seedIfNeeded(syllabus(2, "b", "c")))
        assertEquals(listOf("b", "c"), store.data!!.topics.map { it.id })
        assertEquals(2, store.version)
    }

    /**
     * The contract that protects her progress: progress is keyed by stable topic id and lives
     * outside everything the seeder can reach (SyllabusStore has no progress methods; the Room
     * topic_progress table has no FK). Here "progress" is a separate map the seeder never sees.
     */
    @Test fun progress_keyed_by_topic_id_survives_a_reseed() = runBlocking {
        val store = FakeStore()
        val seeder = SyllabusSeeder(store)
        seeder.seedIfNeeded(syllabus(1, "a", "b"))
        val progress = mutableMapOf("a" to 3, "b" to 1)

        seeder.seedIfNeeded(syllabus(2, "a", "c")) // 'b' removed, 'c' added, 'a' kept

        assertEquals(mapOf("a" to 3, "b" to 1), progress) // untouched, incl. orphan 'b'
        val ids = store.data!!.topics.map { it.id }.toSet()
        assertTrue("a" in ids && progress["a"] == 3)      // still attached to the same topic
        assertFalse("b" in ids)                            // orphan: harmless, not deleted
    }

    @Test fun exams_become_a_bitmask_and_a_shared_topic_is_one_row() = runBlocking {
        val store = FakeStore()
        SyllabusSeeder(store).seedIfNeeded(syllabus(1, "shared", exams = """["G1","G2","G4"]"""))
        assertEquals(1, store.data!!.topics.size)
        assertEquals(7, store.data!!.topics.single().exams)
    }

    @Test fun single_exam_bits() = runBlocking {
        val store = FakeStore()
        SyllabusSeeder(store).seedIfNeeded(syllabus(1, "x", exams = """["G4"]"""))
        assertEquals(4, store.data!!.topics.single().exams)
    }

    @Test fun unknown_exam_code_is_rejected_and_nothing_is_written() = runBlocking {
        val store = FakeStore()
        try {
            SyllabusSeeder(store).seedIfNeeded(syllabus(1, "x", exams = """["G3"]"""))
            fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) { }
        assertEquals(0, store.replaceCalls)
    }

    @Test fun duplicate_topic_ids_are_rejected() = runBlocking {
        val store = FakeStore()
        try {
            SyllabusSeeder(store).seedIfNeeded(syllabus(1, "dup", "dup"))
            fail("expected IllegalArgumentException")
        } catch (_: IllegalArgumentException) { }
        assertEquals(0, store.replaceCalls)
    }

    @Test fun malformed_json_is_rejected_and_nothing_is_written() = runBlocking {
        val store = FakeStore()
        try {
            SyllabusSeeder(store).seedIfNeeded("{ not json")
            fail("expected SerializationException")
        } catch (_: SerializationException) { }
        assertEquals(0, store.replaceCalls)
    }

    /** Guards the real asset (and later, the file converted from the official PDFs). */
    @Test fun bundled_syllabus_json_is_valid_and_labelled_sample() = runBlocking {
        val text = File("src/main/assets/syllabus.json").readText() // tests run with cwd = app/
        val store = FakeStore()
        assertEquals(SeedResult.SEEDED, SyllabusSeeder(store).seedIfNeeded(text))
        val d = store.data!!
        assertEquals(3, d.subjects.size)
        assertTrue(d.topics.size in 10..15)
        assertTrue(d.topics.any { it.exams == 7 })      // shared by all three
        assertTrue(d.topics.any { it.exams == 1 })      // G1 only
        assertTrue(text.contains("SAMPLE DATA ONLY"))
    }
}

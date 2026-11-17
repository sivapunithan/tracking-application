package com.spcode.tnpscprep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DiaryBackupTest {
    private fun dto(date: String = "2026-11-17", text: String = "hello", created: Long = 1000, updated: Long = 1000, mood: String? = null) = DiaryDto(date, text, mood, created, updated)

    @Test fun exportThenParseRoundTripsTamil() {
        val src = listOf(dto(text = "இன்று நல்ல நாள்", mood = "CHEER"), dto(created = 2000, text = "line1\nline2"))
        val parsed = DiaryBackup.parse(DiaryBackup.encode(src, 5)) as ParseResult.Ok
        assertEquals(src.toSet(), parsed.entries.toSet())
    }

    @Test fun badFilesAreRejectedWholesale() {
        assertTrue(DiaryBackup.parse("not json") is ParseResult.Bad)
        assertTrue(DiaryBackup.parse("""{"app":"Other","version":1,"diary":[]}""") is ParseResult.Bad)
        assertTrue(DiaryBackup.parse("""{"app":"TnpscPrep","version":99,"diary":[]}""") is ParseResult.Bad)
        val oneBad = DiaryBackup.encode(listOf(dto(), dto(date = "2026-13-45", created = 2)), 1)
        assertTrue(DiaryBackup.parse(oneBad) is ParseResult.Bad) // one invalid entry rejects everything
        assertTrue(DiaryBackup.parse(DiaryBackup.encode(listOf(dto(created = 0)), 1)) is ParseResult.Bad)
        assertTrue(DiaryBackup.parse("x".repeat(DiaryBackup.MAX_FILE_CHARS + 1)) is ParseResult.Bad)
    }

    @Test fun unknownFieldsAreIgnoredAndDuplicatesCollapse() {
        val text = """{"app":"TnpscPrep","version":1,"future":"x","diary":[
            {"localDate":"2026-11-17","text":"old","createdAt":5,"updatedAt":1,"extra":1},
            {"localDate":"2026-11-17","text":"new","createdAt":5,"updatedAt":9}]}"""
        val ok = DiaryBackup.parse(text) as ParseResult.Ok
        assertEquals(1, ok.entries.size); assertEquals("new", ok.entries[0].text)
    }

    @Test fun mergeAddsNewKeepsExistingAndNeverDeletes() {
        val existing = listOf(ExistingDiary(1, "2026-11-17", 1000, 1000), ExistingDiary(2, "2026-11-16", 900, 900))
        val incoming = listOf(dto(created = 1000, updated = 1000), dto(date = "2026-11-10", created = 50))
        val plan = DiaryBackup.plan(existing, incoming)
        assertEquals(1, plan.inserts.size); assertEquals("2026-11-10", plan.inserts[0].localDate)
        assertTrue(plan.updates.isEmpty()); assertEquals(1, plan.unchanged)
        // the existing entry 2 is not in the file: the plan has no delete operation at all, so it stays
    }

    @Test fun mergeUpdatesOnlyWhenFileCopyIsNewer() {
        val existing = listOf(ExistingDiary(7, "2026-11-17", 1000, 5000))
        assertEquals(listOf(7L), DiaryBackup.plan(existing, listOf(dto(updated = 6000, text = "edited later"))).updates.map { it.first })
        val older = DiaryBackup.plan(existing, listOf(dto(updated = 4000)))
        assertTrue(older.updates.isEmpty()); assertEquals(1, older.unchanged)
    }

    @Test fun importingTwiceChangesNothingTheSecondTime() {
        val file = listOf(dto(), dto(created = 2000))
        val first = DiaryBackup.plan(emptyList(), file)
        val afterFirst = first.inserts.mapIndexed { i, e -> ExistingDiary(i + 1L, e.localDate, e.createdAt, e.updatedAt) }
        val second = DiaryBackup.plan(afterFirst, file)
        assertTrue(second.inserts.isEmpty() && second.updates.isEmpty()); assertEquals(2, second.unchanged)
    }

    @Test fun v2CarriesPinsAndWrapupsAndV1FilesStillImport() {
        val wr = com.spcode.tnpscprep.domain.Wrapup.encode(com.spcode.tnpscprep.domain.WrapupContent(well = "x"))
        val src = listOf(DiaryDto("2026-11-17", "a", "PROUD", 1000, 1000, pinned = true, wrapup = wr))
        val ok = DiaryBackup.parse(DiaryBackup.encode(src, 1)) as ParseResult.Ok
        assertEquals(src, ok.entries)
        // an old (version 1) file has no such fields: defaults apply
        val v1 = DiaryBackup.parse("""{"app":"TnpscPrep","version":1,"diary":[{"localDate":"2026-11-17","text":"t","createdAt":5,"updatedAt":5}]}""") as ParseResult.Ok
        assertEquals(false, v1.entries[0].pinned); assertEquals(null, v1.entries[0].wrapup)
    }

    @Test fun brokenWrapupJsonRejectsTheFile() {
        val bad = DiaryBackup.encode(listOf(DiaryDto("2026-11-17", "a", null, 1000, 1000, wrapup = "not json")), 1)
        assertTrue(DiaryBackup.parse(bad) is ParseResult.Bad)
    }
}

package com.spcode.tnpscprep.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBackupTest {
    private val diary = DiaryDto("2026-11-17", "இன்று நல்ல நாள்", "CHEER", 1000, 1000, true, null)
    private fun full() = FullBackupFile(
        exportedAt = 5, diary = listOf(diary),
        progress = listOf(ProgressDto("gk.u1.t1", 3, 100)),
        notes = listOf(NoteDto("gk.u1.t1", "note\nline2", true, 200)),
        sessions = listOf(SessionDto("2026-11-17", 300, 25, "gk")),
        affairState = listOf(AffairStateDto(itemId = "ca-1", bookmarked = true, level = 2, lastUpdated = 9), AffairStateDto(userCreatedAt = 77, bookmarked = false, level = 1, lastUpdated = 8)),
        userAffairs = listOf(UserAffairDto("2026-11-10", "TN", "t", "s", 77)),
        events = listOf(EventDto(400, "2026-11-17", "FOCUS_SESSION", null, """{"minutes":25}""")),
        dayNotes = listOf(DayNoteDto("2026-11-17", "good", 11)),
        plans = listOf(PlanDto("2026-11-18", "Read polity", "TOPIC", "p1", false, 12, 0)),
        health = listOf(HealthDto("2026-11-17", "WEIGHT_KG", 58.5, 13)),
        steps = listOf(StepDto("2026-11-17", 1, 2, 3000, 4, 5)),
        settings = SettingsDto(userName = "Mala", targetExam = "G1,G2", dailyGoalMinutes = 60, heightCm = 160),
    )

    @Test fun fullFileRoundTripsIncludingTamil() {
        val ok = AppBackup.parse(AppBackup.encode(full())) as AppBackup.Parsed.Ok
        assertEquals(full().copy(), ok.file)
    }

    @Test fun neverContainsPinOrExcludedData() {
        val text = AppBackup.encode(full()).lowercase()
        listOf("pin_hash", "pin_salt", "pinhash", "salt", "usage", "wrapupdraft").forEach { assertFalse(it, text.contains(it)) }
    }

    @Test fun diaryOnlyImportStillReadsAFullFile() {
        val ok = DiaryBackup.parse(AppBackup.encode(full())) as ParseResult.Ok
        assertEquals(listOf(diary), ok.entries)
    }

    @Test fun oldDiaryFilesAreAcceptedAsAFullFileWithEmptySections() {
        val v1 = """{"app":"TnpscPrep","version":1,"diary":[{"localDate":"2026-11-17","text":"t","createdAt":5,"updatedAt":5}]}"""
        val ok = AppBackup.parse(v1) as AppBackup.Parsed.Ok
        assertEquals(1, ok.file.diary.size); assertTrue(ok.file.progress.isEmpty() && ok.file.settings == null)
    }

    @Test fun badFilesAreRejectedWholesale() {
        assertTrue(AppBackup.parse("nope") is AppBackup.Parsed.Bad)
        assertTrue(AppBackup.parse("""{"app":"Other","version":3}""") is AppBackup.Parsed.Bad)
        assertTrue(AppBackup.parse("""{"app":"TnpscPrep","version":99}""") is AppBackup.Parsed.Bad)
        val f = full()
        listOf(
            f.copy(progress = listOf(ProgressDto("a", 9, 1))),                       // level out of range
            f.copy(sessions = listOf(SessionDto("2026-13-40", 1, 5))),               // bad date
            f.copy(health = listOf(HealthDto("2026-11-17", "", 58.0, 1))),
            f.copy(affairState = listOf(AffairStateDto(bookmarked = true, level = 0, lastUpdated = 1))), // neither id nor createdAt
            f.copy(events = listOf(EventDto(1, "2026-11-17", "DIARY", "5"))),        // local-id event type
            f.copy(diary = listOf(diary.copy(createdAt = 0))),
        ).forEach { assertTrue(AppBackup.parse(AppBackup.encode(it)) is AppBackup.Parsed.Bad) }
    }

    // ---- merge rules ----

    @Test fun progressNeverLowersAndAddsMissing() {
        val w = AppBackup.progressToWrite(mapOf("a" to 3, "b" to 1), listOf(ProgressDto("a", 2, 1), ProgressDto("b", 4, 1), ProgressDto("c", 1, 1), ProgressDto("d", 0, 1)))
        assertEquals(setOf("b", "c"), w.map { it.topicId }.toSet())
    }

    @Test fun notesNewerWinsAndMissingIsAdded() {
        val w = AppBackup.notesToWrite(mapOf("a" to 100L, "b" to 100L), listOf(NoteDto("a", "x", false, 50), NoteDto("b", "y", false, 150), NoteDto("c", "z", true, 1)))
        assertEquals(setOf("b", "c"), w.map { it.topicId }.toSet())
    }

    @Test fun sessionsAndHealthAreNotDoubleCounted() {
        assertTrue(AppBackup.sessionsToAdd(setOf(300L to 25), listOf(SessionDto("2026-11-17", 300, 25))).isEmpty())
        assertEquals(1, AppBackup.sessionsToAdd(setOf(300L to 25), listOf(SessionDto("2026-11-17", 301, 25), SessionDto("2026-11-17", 301, 25))).size)
        assertTrue(AppBackup.healthToAdd(setOf(Triple("WEIGHT_KG", "2026-11-17", 13L)), full().health).isEmpty())
    }

    @Test fun oneAnswerPerDayForCheckinsButAllOtherEventsKept() {
        val local = setOf(Triple("FOOD_CHECKIN", "2026-11-17" as String?, 1000L))
        val latest = mapOf(("FOOD_CHECKIN" to "2026-11-17") to 1000L)
        val inc = listOf(
            EventDto(900, "2026-11-17", "FOOD_CHECKIN", "2026-11-17"),   // older answer: dropped
            EventDto(1100, "2026-11-18", "FOOD_CHECKIN", "2026-11-18"),  // new day
            EventDto(1200, "2026-11-18", "MOOD_CHECKIN", "2026-11-18"), EventDto(1300, "2026-11-18", "MOOD_CHECKIN", "2026-11-18"), // two in file: newest only
            EventDto(50, "2026-11-17", "FOCUS_SESSION"), EventDto(50, "2026-11-17", "FOCUS_SESSION"), // exact dup collapses
        )
        val add = AppBackup.eventsToAdd(local, latest, inc)
        assertEquals(setOf("FOOD_CHECKIN@1100", "MOOD_CHECKIN@1300", "FOCUS_SESSION@50"), add.map { "${it.type}@${it.timestamp}" }.toSet())
        assertEquals(3, add.size)
    }

    @Test fun affairStateKeysSeparateUserAndPackItems() {
        val a = AffairStateDto(itemId = "ca-1", bookmarked = true, level = 1, lastUpdated = 5)
        val u = AffairStateDto(userCreatedAt = 77, bookmarked = true, level = 1, lastUpdated = 5)
        assertEquals("ca-1", AppBackup.stateKey(a)); assertEquals("user:77", AppBackup.stateKey(u))
        assertEquals(listOf(u), AppBackup.statesToWrite(mapOf("ca-1" to 9L, "user:77" to 4L), listOf(a, u)))
    }

    @Test fun plansDayNotesStepsMerge() {
        assertTrue(AppBackup.plansToAdd(setOf(Triple("2026-11-18", 12L, "Read polity")), full().plans).isEmpty())
        assertEquals(1, AppBackup.dayNotesToWrite(mapOf("2026-11-17" to 5L), full().dayNotes).size)
        assertTrue(AppBackup.stepsToWrite(mapOf("2026-11-17" to 99L), full().steps).isEmpty())
    }
}

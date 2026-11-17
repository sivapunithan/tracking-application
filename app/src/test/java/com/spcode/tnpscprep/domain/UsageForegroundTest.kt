package com.spcode.tnpscprep.domain

import com.spcode.tnpscprep.domain.UsageKind.BG
import com.spcode.tnpscprep.domain.UsageKind.FG
import com.spcode.tnpscprep.domain.UsageKind.SCREEN_OFF
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UsageForegroundTest {
    private val min = 60_000L
    private fun ev(p: String, k: UsageKind, m: Long) = UsageEv(p, k, m * min)
    private fun run(vararg e: UsageEv, start: Long = 0, end: Long = 1000) = UsageForeground.compute(e.toList(), start * min, end * min)

    @Test fun simpleSpan() = assertEquals(mapOf("a" to 10 * min), run(ev("a", FG, 5), ev("a", BG, 15)))

    @Test fun twoSpansAdd() =
        assertEquals(mapOf("a" to 15 * min), run(ev("a", FG, 0), ev("a", BG, 10), ev("a", FG, 20), ev("a", BG, 25)))

    @Test fun alreadyInFrontWhenWindowStarts() =
        assertEquals(mapOf("a" to 7 * min), run(ev("a", BG, 107), start = 100, end = 200)) // counted from the window start

    @Test fun spanOpenAtTheEndIsClippedToTheEnd() =
        assertEquals(mapOf("a" to 20 * min), run(ev("a", FG, 80), start = 0, end = 100))

    @Test fun eventsBeforeWindowAreClipped() =
        assertEquals(mapOf("a" to 5 * min), run(ev("a", FG, 50), ev("a", BG, 105), start = 100, end = 200))

    @Test fun eventsAfterTheWindowAreIgnored() =
        assertEquals(mapOf("a" to 10 * min), run(ev("a", FG, 90), ev("a", BG, 150), start = 0, end = 100))

    @Test fun repeatedForegroundDoesNotDoubleCount() =
        assertEquals(mapOf("a" to 10 * min), run(ev("a", FG, 0), ev("a", FG, 4), ev("a", BG, 10)))

    @Test fun screenOffClosesOpenSpans() =
        assertEquals(mapOf("a" to 20 * min, "b" to 10 * min), run(ev("a", FG, 0), ev("b", FG, 20), ev("x", SCREEN_OFF, 30)))

    @Test fun interleavedAppsAreIndependent() {
        val r = run(ev("a", FG, 0), ev("a", BG, 5), ev("b", FG, 5), ev("b", BG, 12), ev("a", FG, 12), ev("a", BG, 14))
        assertEquals(mapOf("a" to 7 * min, "b" to 7 * min), r)
    }

    @Test fun unsortedInputAndEmpty() {
        assertEquals(mapOf("a" to 4 * min), run(ev("a", BG, 9), ev("a", FG, 5)))
        assertTrue(run().isEmpty()); assertTrue(UsageForeground.compute(emptyList(), 10, 10).isEmpty())
    }

    @Test fun neverMoreThanTheWindowForOneApp() {
        // a weekly-bucket style mistake can't happen: one app's time is bounded by the window length
        val r = UsageForeground.compute(listOf(UsageEv("a", FG, 0), UsageEv("a", BG, 7 * 24 * 60 * min)), 0, 24 * 60 * min)
        assertEquals(24 * 60 * min, r["a"])
    }

    @Test fun pausedThenStoppedIsNotCountedTwice() {
        // Android sends BG twice for one close; the second must be ignored, not counted from the window start
        val r = run(ev("a", FG, 500), ev("a", BG, 510), ev("a", BG, 511), start = 0, end = 1000)
        assertEquals(mapOf("a" to 10 * min), r)
    }

    @Test fun noAppAndNoDayEverExceeds24Hours() {
        val day = 24 * 60 * min
        val rnd = java.util.Random(42)
        repeat(200) {
            val pkgs = listOf("a", "b", "c")
            val evs = (1..rnd.nextInt(60)).map { UsageEv(pkgs[rnd.nextInt(3)], UsageKind.entries[rnd.nextInt(3)], rnd.nextInt(30 * 60) * min) } // random, even nonsense, sequences
            val perApp = UsageForeground.compute(evs, 0, day)
            perApp.values.forEach { assertTrue("one app over 24h: $perApp", it <= day) }
            assertTrue("all apps together over 24h: $perApp", perApp.values.sum() <= day) // one app in front at a time
        }
    }

    @Test fun realisticDayStaysUnder24Hours() {
        // 200 short sessions of 3 apps, each close sent as paused + stopped
        val evs = ArrayList<UsageEv>()
        for (i in 0 until 200) { val p = "app${i % 3}"; evs += ev(p, FG, i * 7L); evs += ev(p, BG, i * 7L + 3); evs += ev(p, BG, i * 7L + 3) }
        val r = UsageForeground.compute(evs, 0, 24 * 60 * min)
        assertTrue(r.values.sum() <= 24 * 60 * min)
        assertEquals(200 * 3 * min, r.values.sum())
    }

    @Test fun aLostPausedEventCannotRunOnForHours() {
        // "a" is never paused (lost event). When "b" comes to the front 10 minutes later, a's time ends there, not at the end of the day.
        val r = run(ev("a", FG, 0), ev("b", FG, 10), ev("b", BG, 20), start = 0, end = 24 * 60)
        assertEquals(mapOf("a" to 10 * min, "b" to 10 * min), r)
    }

    @Test fun screenOffEndsTheSpanEvenWithoutAPausedEvent() {
        assertEquals(mapOf("a" to 30 * min), run(ev("a", FG, 0), ev("x", SCREEN_OFF, 30), start = 0, end = 24 * 60))
    }
}

package com.spcode.tnpscprep.domain

import java.time.LocalDate
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class PhoneUsageTest {
    private val min = 60_000L

    @Test fun dropsSubMinuteAndSums() {
        val u = PhoneUsage.summarise(mapOf("a" to 10 * min, "b" to 59_999L, "c" to 5 * min + 30_000))
        assertEquals(15, u.totalMinutes)
        assertEquals(listOf("a" to 10, "c" to 5), u.topApps)
    }

    @Test fun topNKeepsBiggestButTotalCountsAll() {
        val raw = (1..12).associate { "app$it" to it * min }
        val u = PhoneUsage.summarise(raw, topN = 3)
        assertEquals(78, u.totalMinutes)
        assertEquals(listOf("app12", "app11", "app10"), u.topApps.map { it.first })
    }

    @Test fun tiesAreStable() {
        val u = PhoneUsage.summarise(mapOf("z" to 2 * min, "a" to 2 * min))
        assertEquals(listOf("a", "z"), u.topApps.map { it.first })
    }

    @Test fun emptyIsZero() = assertEquals(DayUsage(0, emptyList()), PhoneUsage.summarise(emptyMap()))

    @Test fun windowEndsAtNowForToday() {
        val z = ZoneId.of("Asia/Kolkata"); val d = LocalDate.of(2026, 11, 17)
        val start = d.atStartOfDay(z).toInstant().toEpochMilli()
        assertEquals(start to start + 5 * 3_600_000, PhoneUsage.window(d, start + 5 * 3_600_000, z))
        assertEquals(start to start + 24 * 3_600_000, PhoneUsage.window(d, start + 99 * 3_600_000, z))
        assertEquals(start to start, PhoneUsage.window(d, start - 1000, z)) // future day: empty
    }

    @Test fun readsSevenDaysOldestFirst() {
        val d = PhoneUsage.datesToRead(LocalDate.of(2026, 11, 17))
        assertEquals(7, d.size); assertEquals(LocalDate.of(2026, 11, 11), d.first()); assertEquals(LocalDate.of(2026, 11, 17), d.last())
    }

    @Test fun labels() { assertEquals("45m", PhoneUsage.label(45)); assertEquals("3h 5m", PhoneUsage.label(185)); assertEquals("Instagram", PhoneUsage.fallbackName("com.instagram.android".replace("android", "instagram"))) }
}

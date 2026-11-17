package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupNudgeTest {
    private val day = 24L * 60 * 60 * 1000
    private val now = 100 * day

    @Test fun showsAfterFourteenDaysSinceLastBackup() {
        assertFalse(BackupNudge.shouldShow(now, now - 13 * day, 0, 0, true))
        assertTrue(BackupNudge.shouldShow(now, now - 14 * day, 0, 0, true))
    }
    @Test fun neverBackedUpCountsFromFirstSight() {
        assertFalse(BackupNudge.shouldShow(now, 0, 0, 0, true))              // not stamped yet: nothing
        assertFalse(BackupNudge.shouldShow(now, 0, now - 3 * day, 0, true))  // new install is not nagged
        assertTrue(BackupNudge.shouldShow(now, 0, now - 15 * day, 0, true))
    }
    @Test fun switchOffAndSnoozeHideIt() {
        assertFalse(BackupNudge.shouldShow(now, now - 30 * day, 0, 0, false))
        assertFalse(BackupNudge.shouldShow(now, now - 30 * day, 0, BackupNudge.snoozeUntil(now - day), true))
        assertTrue(BackupNudge.shouldShow(now + 8 * day, now - 30 * day, 0, BackupNudge.snoozeUntil(now), true))
    }
    @Test fun daysSince() {
        assertEquals(null, BackupNudge.daysSince(now, 0)); assertEquals(20, BackupNudge.daysSince(now, now - 20 * day))
    }
}

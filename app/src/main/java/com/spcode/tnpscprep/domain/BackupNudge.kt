package com.spcode.tnpscprep.domain

/**
 * When to gently suggest a backup. Pure so it is unit-tested. Rules: switched off = never; the clock starts at the last backup, or (if she
 * has never backed up) at the day the app first checked, so a brand-new install is not nagged at once; after [DAYS] days the card shows;
 * "Later" hides it for [SNOOZE_DAYS] days. No notification: it is only a card on Today (ColorOS kills background work anyway).
 */
object BackupNudge {
    const val DAYS = 14
    const val SNOOZE_DAYS = 7
    private const val DAY_MS = 24L * 60 * 60 * 1000

    fun shouldShow(nowMs: Long, lastBackupMs: Long, baseMs: Long, snoozeUntilMs: Long, enabled: Boolean): Boolean {
        if (!enabled) return false
        if (nowMs < snoozeUntilMs) return false
        val since = if (lastBackupMs > 0) lastBackupMs else baseMs
        if (since <= 0) return false // the baseline is stamped on first sight; show nothing before that
        return nowMs - since >= DAYS * DAY_MS
    }

    fun snoozeUntil(nowMs: Long): Long = nowMs + SNOOZE_DAYS * DAY_MS
    fun daysSince(nowMs: Long, lastBackupMs: Long): Int? = if (lastBackupMs <= 0) null else ((nowMs - lastBackupMs) / DAY_MS).toInt()
}

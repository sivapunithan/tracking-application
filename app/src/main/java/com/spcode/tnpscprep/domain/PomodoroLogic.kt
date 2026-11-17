package com.spcode.tnpscprep.domain

enum class Phase { FOCUS, BREAK }

data class TimerConfig(val focusMinutes: Int, val breakMinutes: Int) {
    fun durationMs(phase: Phase): Long = (if (phase == Phase.FOCUS) focusMinutes else breakMinutes) * 60_000L
}

/** [endAtMs] is only meaningful while [running]; [remainingMs] is refreshed on every tick. */
data class TimerState(
    val phase: Phase,
    val running: Boolean,
    val remainingMs: Long,
    val endAtMs: Long = 0L,
)

data class TickResult(val state: TimerState, val focusCompleted: Boolean)

/**
 * Pure state machine (no Android, no coroutines). The caller passes `now` from a monotonic clock
 * (SystemClock.elapsedRealtime). Counting against an END TIME instead of decrementing a counter
 * means a late or skipped tick can never make the timer drift.
 */
object PomodoroLogic {
    private fun other(p: Phase) = if (p == Phase.FOCUS) Phase.BREAK else Phase.FOCUS

    fun idle(phase: Phase, config: TimerConfig) = TimerState(phase, running = false, remainingMs = config.durationMs(phase))

    fun start(s: TimerState, now: Long): TimerState =
        if (s.running || s.remainingMs <= 0) s else s.copy(running = true, endAtMs = now + s.remainingMs)

    fun pause(s: TimerState, now: Long): TimerState =
        if (!s.running) s else s.copy(running = false, remainingMs = (s.endAtMs - now).coerceAtLeast(0))

    /**
     * When a phase ends, move to the OTHER phase, stopped and ready: the next phase never starts by
     * itself (nobody may be looking). Only a finished FOCUS phase counts as a completed session.
     */
    fun tick(s: TimerState, now: Long, config: TimerConfig): TickResult {
        if (!s.running) return TickResult(s, false)
        val remaining = s.endAtMs - now
        if (remaining > 0) return TickResult(s.copy(remainingMs = remaining), false)
        return TickResult(idle(other(s.phase), config), focusCompleted = s.phase == Phase.FOCUS)
    }

    fun reset(s: TimerState, config: TimerConfig) = idle(s.phase, config)
    fun skip(s: TimerState, config: TimerConfig) = idle(other(s.phase), config)

    /** Round UP so the display shows 00:01 until the very end instead of 00:00 while still running. */
    fun displaySeconds(ms: Long): Int = ((ms.coerceAtLeast(0) + 999) / 1000).toInt()
}

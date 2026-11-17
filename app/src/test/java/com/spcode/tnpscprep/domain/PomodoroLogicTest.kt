package com.spcode.tnpscprep.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PomodoroLogicTest {
    private val cfg = TimerConfig(focusMinutes = 25, breakMinutes = 5)
    private val focusMs = 25 * 60_000L
    private val breakMs = 5 * 60_000L

    @Test fun idle_uses_the_configured_duration_per_phase() {
        assertEquals(focusMs, PomodoroLogic.idle(Phase.FOCUS, cfg).remainingMs)
        assertEquals(breakMs, PomodoroLogic.idle(Phase.BREAK, cfg).remainingMs)
    }

    @Test fun start_sets_an_end_time_and_ticks_count_down_against_it() {
        val s = PomodoroLogic.start(PomodoroLogic.idle(Phase.FOCUS, cfg), now = 1_000)
        assertTrue(s.running)
        assertEquals(1_000 + focusMs, s.endAtMs)
        val r = PomodoroLogic.tick(s, now = 1_000 + 60_000, config = cfg)
        assertEquals(focusMs - 60_000, r.state.remainingMs)
        assertFalse(r.focusCompleted)
    }

    @Test fun a_late_tick_does_not_drift() { // tick arrives after 10 minutes of nothing: still exact
        val s = PomodoroLogic.start(PomodoroLogic.idle(Phase.FOCUS, cfg), now = 0)
        assertEquals(focusMs - 600_000, PomodoroLogic.tick(s, 600_000, cfg).state.remainingMs)
    }

    @Test fun finishing_focus_reports_completion_and_moves_to_a_stopped_break() {
        val s = PomodoroLogic.start(PomodoroLogic.idle(Phase.FOCUS, cfg), now = 0)
        val r = PomodoroLogic.tick(s, now = focusMs, config = cfg)
        assertTrue(r.focusCompleted)
        assertEquals(Phase.BREAK, r.state.phase)
        assertFalse(r.state.running) // never auto-starts
        assertEquals(breakMs, r.state.remainingMs)
    }

    @Test fun finishing_a_break_is_not_a_logged_session() {
        val s = PomodoroLogic.start(PomodoroLogic.idle(Phase.BREAK, cfg), now = 0)
        val r = PomodoroLogic.tick(s, now = breakMs + 5, config = cfg)
        assertFalse(r.focusCompleted)
        assertEquals(Phase.FOCUS, r.state.phase)
        assertFalse(r.state.running)
    }

    @Test fun pause_freezes_remaining_and_resume_continues_from_it() {
        val running = PomodoroLogic.start(PomodoroLogic.idle(Phase.FOCUS, cfg), now = 0)
        val paused = PomodoroLogic.pause(running, now = 100_000)
        assertFalse(paused.running)
        assertEquals(focusMs - 100_000, paused.remainingMs)
        val resumed = PomodoroLogic.start(paused, now = 5_000_000) // long pause in between
        assertEquals(5_000_000 + focusMs - 100_000, resumed.endAtMs)
    }

    @Test fun pause_never_goes_negative() {
        val running = PomodoroLogic.start(PomodoroLogic.idle(Phase.FOCUS, cfg), now = 0)
        assertEquals(0, PomodoroLogic.pause(running, now = focusMs + 999).remainingMs)
    }

    @Test fun tick_does_nothing_when_not_running() {
        val s = PomodoroLogic.idle(Phase.FOCUS, cfg)
        val r = PomodoroLogic.tick(s, now = 9_999_999, config = cfg)
        assertEquals(s, r.state)
        assertFalse(r.focusCompleted)
    }

    @Test fun reset_keeps_phase_and_skip_switches_it() {
        val mid = PomodoroLogic.pause(PomodoroLogic.start(PomodoroLogic.idle(Phase.FOCUS, cfg), 0), 50_000)
        assertEquals(PomodoroLogic.idle(Phase.FOCUS, cfg), PomodoroLogic.reset(mid, cfg))
        assertEquals(PomodoroLogic.idle(Phase.BREAK, cfg), PomodoroLogic.skip(mid, cfg))
    }

    @Test fun display_seconds_round_up() {
        assertEquals(0, PomodoroLogic.displaySeconds(0))
        assertEquals(1, PomodoroLogic.displaySeconds(1))
        assertEquals(1, PomodoroLogic.displaySeconds(1000))
        assertEquals(2, PomodoroLogic.displaySeconds(1001))
        assertEquals(0, PomodoroLogic.displaySeconds(-50))
    }
}

package com.spcode.tnpscprep.domain

/** The only things we need from Android's usage events. FG = app came to the front, BG = it left, SCREEN_OFF = screen off / lock screen / shutdown (nothing is in front any more). */
enum class UsageKind { FG, BG, SCREEN_OFF }

data class UsageEv(val pkg: String, val kind: UsageKind, val timeMs: Long)

/**
 * Foreground time per app from raw events, clipped EXACTLY to [startMs, endMs). This replaces queryAndAggregateUsageStats, which returns whole
 * overlapping buckets (a weekly bucket inside a one-day query = a "day" with 54 hours).
 *
 *  - FG opens a span, BG closes it. A repeated FG while open is ignored.
 *  - A BG with no open span counts from [startMs] ONLY if it is the very FIRST event of the window (that app was in front when the window
 *    began; only one app can be). Any other BG with nothing open is a duplicate (Android sends "paused" then "stopped") and is ignored.
 *  - Only ONE app is in front at a time: an app coming to the front ends every other open span at that moment. SCREEN_OFF ends all open spans.
 *    So a lost "paused" event can never make time run on for hours, and the total across apps never exceeds the window.
 *  - Spans still open at [endMs] end there.
 */
object UsageForeground {
    fun compute(events: List<UsageEv>, startMs: Long, endMs: Long): Map<String, Long> {
        if (endMs <= startMs) return emptyMap()
        val open = HashMap<String, Long>()
        val total = HashMap<String, Long>()
        var first = true
        fun add(pkg: String, from: Long, to: Long) {
            val a = maxOf(from, startMs); val b = minOf(to, endMs)
            if (b > a) total.merge(pkg, b - a, Long::plus)
        }
        for (e in events.sortedBy { it.timeMs }) {
            if (e.timeMs >= endMs) break
            val isFirst = first; first = false
            when (e.kind) {
                UsageKind.FG -> {
                    // someone else comes to the front: everything else open ends now
                    open.keys.filter { it != e.pkg }.forEach { other -> add(other, open.remove(other)!!, e.timeMs) }
                    if (e.pkg !in open) open[e.pkg] = e.timeMs
                }
                UsageKind.BG -> {
                    val from = open.remove(e.pkg)
                    if (from != null) add(e.pkg, from, e.timeMs) else if (isFirst) add(e.pkg, startMs, e.timeMs)
                }
                UsageKind.SCREEN_OFF -> { open.forEach { (p, from) -> add(p, from, e.timeMs) }; open.clear() }
            }
        }
        open.forEach { (p, from) -> add(p, from, endMs) }
        return total
    }
}

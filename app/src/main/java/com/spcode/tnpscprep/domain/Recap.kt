package com.spcode.tnpscprep.domain

import java.time.LocalDate
import java.time.ZoneId

/** One logged event as the recap needs it (a plain copy of an activity_event row). */
data class RecapEvent(val timestamp: Long, val localDate: String, val type: String, val refId: String?, val details: String)

/** What happened on one day. A section with nothing in it is null / 0 / empty and is simply not shown. Never a score. */
data class DayRecap(
    val studyMinutes: Int,
    val goalMinutes: Int,
    val focusSessions: Int,
    val longestSessionMinutes: Int,
    val topicsAdvanced: List<String>,
    /** How many distinct topics moved forward (counts topics whose name is not known too). */
    val topicsAdvancedCount: Int,
    val affairsRevised: Int,
    val food: FoodState?,
    /** Approximate: counted only when the app is opened. */
    val steps: Int?,
    val diaryEntries: Int,
) {
    /** True when there is nothing to show at all (the UI then shows a friendly empty recap). */
    val isEmpty: Boolean
        get() = studyMinutes == 0 && focusSessions == 0 && topicsAdvancedCount == 0 && affairsRevised == 0 && food == null && steps == null && diaryEntries == 0
}

/**
 * Derives a day's recap from the activity log: NO stored recap, so it can never disagree with the log.
 * Study events (sessions, topics, affairs) belong to the day by TIMESTAMP with the 3 AM rollover; food, diary and steps already carry the
 * right day in `localDate` (food/diary use the rollover when written, steps are the calendar day).
 * Songs listened to: there is no such data yet, so that section does not exist.
 */
object RecapBuilder {
    private val MINUTES = Regex("\"minutes\"\\s*:\\s*(\\d+)")
    private val FROM = Regex("\"from\"\\s*:\\s*(\\d+)")
    private val TO = Regex("\"to\"\\s*:\\s*(\\d+)")
    private val STEPS = Regex("\"steps\"\\s*:\\s*(\\d+)")
    private val FOOD = Regex("\"value\"\\s*:\\s*\"(\\w+)\"")

    /** [events] may be a wider window (e.g. the day and the next calendar date); this picks what belongs to [day]. [topicNames] maps topic id -> display name. */
    fun build(day: LocalDate, events: List<RecapEvent>, goalMinutes: Int, topicNames: Map<String, String>, zone: ZoneId): DayRecap {
        fun studyDay(e: RecapEvent) = LogicalDay.of(e.timestamp, zone) == day
        val sessions = events.filter { it.type == ActivityType.FOCUS_SESSION.name && studyDay(it) }
            .map { MINUTES.find(it.details)?.groupValues?.get(1)?.toIntOrNull() ?: 0 }
        val advanced = events.filter { it.type == ActivityType.TOPIC_PROGRESS.name && studyDay(it) && !it.details.contains("backfilled") }
            .filter { e ->
                val from = FROM.find(e.details)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                val to = TO.find(e.details)?.groupValues?.get(1)?.toIntOrNull() ?: 0
                to > from // a reset (to = 0) or a step down is not "advanced"
            }
            .mapNotNull { it.refId }.distinct()
        val advancedNamed = advanced.mapNotNull { topicNames[it] }
        val affairs = events.filter { it.type == ActivityType.AFFAIR_REVISED.name && studyDay(it) }.mapNotNull { it.refId }.distinct().size
        val dateStr = day.toString()
        val food = events.lastOrNull { it.type == ActivityType.FOOD_CHECKIN.name && it.localDate == dateStr }
            ?.let { FOOD.find(it.details)?.groupValues?.get(1) }?.let { v -> FoodState.entries.firstOrNull { it.name == v } }
        val steps = events.lastOrNull { it.type == ActivityType.STEPS.name && it.localDate == dateStr }
            ?.let { STEPS.find(it.details)?.groupValues?.get(1)?.toIntOrNull() }?.takeIf { it > 0 }
        val diary = events.filter { it.type == ActivityType.DIARY.name && it.localDate == dateStr }.mapNotNull { it.refId }.distinct().size
        return DayRecap(
            studyMinutes = sessions.sum(), goalMinutes = goalMinutes, focusSessions = sessions.size, longestSessionMinutes = sessions.maxOrNull() ?: 0,
            topicsAdvanced = advancedNamed, topicsAdvancedCount = advanced.size, affairsRevised = affairs, food = food, steps = steps, diaryEntries = diary,
        )
    }
}

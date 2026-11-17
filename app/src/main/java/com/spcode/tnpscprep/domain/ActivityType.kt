package com.spcode.tnpscprep.domain

/** What kind of thing happened. Stored by NAME in activity_event: never rename, only add. */
enum class ActivityType {
    FOCUS_SESSION, TOPIC_PROGRESS, NOTE, AFFAIR_REVISED, AFFAIR_ADDED,
    MOCK_TEST, HEALTH_LOG,
    DIARY, // one row per diary entry (refId = entry id), replaced on every save; holds only the character count, never the text
    FOOD_CHECKIN, // one row per food day (refId = date), replaced when she changes it; not a study event
    STEPS, // one row per day (refId = date), replaced on every read; not a study event // reserved for later increments
    MOOD_CHECKIN, // one row per logical day (refId = date), replaced when she changes it; holds only the mood name; not a study event
}

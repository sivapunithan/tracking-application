package com.spcode.tnpscprep.domain

/** What kind of thing happened. Stored by NAME in activity_event: never rename, only add. */
enum class ActivityType {
    FOCUS_SESSION, TOPIC_PROGRESS, NOTE, AFFAIR_REVISED, AFFAIR_ADDED,
    MOCK_TEST, HEALTH_LOG,
    STEPS, // one row per day (refId = date), replaced on every read; not a study event // reserved for later increments
}

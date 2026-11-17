package com.spcode.tnpscprep.data

import com.spcode.tnpscprep.data.db.ActivityEventEntity
import com.spcode.tnpscprep.domain.ActivityType
import java.time.Instant
import java.time.ZoneId
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.buildJsonObject

/** Pure builder (no database): turns "something happened" into the row we store. */
object ActivityEvents {
    fun build(
        type: ActivityType, refId: String?, nowMs: Long, zone: ZoneId = ZoneId.systemDefault(),
        localDateOverride: String? = null, // e.g. the food day (3 AM rollover) differs from the calendar date
        details: JsonObjectBuilder.() -> Unit = {},
    ): ActivityEventEntity = ActivityEventEntity(
        timestamp = nowMs,
        localDate = localDateOverride ?: Instant.ofEpochMilli(nowMs).atZone(zone).toLocalDate().toString(),
        type = type.name,
        refId = refId,
        details = buildJsonObject(details).toString(),
    )
}

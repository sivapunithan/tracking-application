package com.spcode.tnpscprep.data

import androidx.room.withTransaction
import com.spcode.tnpscprep.data.db.AppDatabase
import com.spcode.tnpscprep.data.db.StepDayEntity
import com.spcode.tnpscprep.domain.ActivityType
import com.spcode.tnpscprep.domain.StepCounter
import com.spcode.tnpscprep.domain.StepDay
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.JsonPrimitive

class StepRepository(private val db: AppDatabase, private val now: () -> Long = System::currentTimeMillis) {
    private val dao = db.stepDao()

    /** Last [days] days, oldest first. */
    fun observeRecent(from: LocalDate): Flow<List<StepDayEntity>> = dao.observeSince(from.toString())

    /** Feed one sensor reading in; also refreshes that day's single STEPS event, in the same transaction. */
    suspend fun record(reading: Long, bootMs: Long, today: LocalDate) = db.withTransaction {
        val prev = dao.latest()?.let { StepDay(LocalDate.parse(it.date), it.baseline, it.lastReading, it.steps, it.bootMs) }
        val next = StepCounter.update(prev, reading, bootMs, today)
        dao.upsert(StepDayEntity(next.date.toString(), next.baseline, next.lastReading, next.steps, next.bootMs, now()))
        val act = db.activityDao()
        val ref = next.date.toString()
        act.deleteByRef(ActivityType.STEPS.name, ref)
        // timestamp = now, but localDate is the counted day; ActivityEvents derives it from the clock, which is the same day here.
        act.insert(ActivityEvents.build(ActivityType.STEPS, ref, now()) { put("steps", JsonPrimitive(next.steps)) })
    }
}

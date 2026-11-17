package com.spcode.tnpscprep.domain

import java.time.LocalDate

/** Small pure helpers for the Care page. Numbers only: no scores, no judgement. */
object CareStats {
    const val DEFAULT_WATER_ML = 2000

    /** Daily water target: about 35 ml per kg of the latest logged weight, else [DEFAULT_WATER_ML]. */
    fun waterTarget(latestWeightKg: Double?): Int = latestWeightKg?.let { Health.waterMl(it) } ?: DEFAULT_WATER_ML

    /** Millilitres per day for the [days] days ending [today], oldest first. [entries] = (date, ml) pairs; several entries per day add up. */
    fun waterDays(entries: List<Pair<LocalDate, Int>>, today: LocalDate, days: Int = 7): List<Pair<LocalDate, Int>> {
        val byDay = entries.groupBy({ it.first }, { it.second }).mapValues { it.value.sum() }
        return MoodCheckins.lastDays(today, days).map { it to (byDay[it] ?: 0) }
    }

    /** Fill of the water ring, 0..1 (a full ring is simply "reached", never "more is better"). */
    fun fraction(ml: Int, target: Int): Float = if (target <= 0) 0f else (ml.toFloat() / target).coerceIn(0f, 1f)
}

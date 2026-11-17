package com.spcode.tnpscprep.domain

import java.time.LocalDate
import kotlin.math.roundToInt

enum class Sex { FEMALE, MALE }
enum class BmiBand { UNDER, NORMAL, OVER, OBESE }
enum class ActivityLevel(val factor: Double) { SEDENTARY(1.2), LIGHT(1.375), MODERATE(1.55), ACTIVE(1.725), VERY_ACTIVE(1.9) }

/** Offline health maths. Everything here is a general ESTIMATE, not medical advice (the UI must say so). */
object Health {
    /** Body-mass index = kg / m^2. Null when an input is not plausible. */
    fun bmi(kg: Double, cm: Double): Double? =
        if (kg !in 20.0..300.0 || cm !in 100.0..250.0) null else kg / ((cm / 100) * (cm / 100))

    /** WHO adult bands (South-Asian guidance uses lower cut-offs; the UI notes that). */
    fun bmiBand(bmi: Double): BmiBand = when {
        bmi < 18.5 -> BmiBand.UNDER
        bmi < 25.0 -> BmiBand.NORMAL
        bmi < 30.0 -> BmiBand.OVER
        else -> BmiBand.OBESE
    }

    /** Mifflin-St Jeor basal metabolic rate, kcal/day. */
    fun bmr(kg: Double, cm: Double, age: Int, sex: Sex): Double? =
        if (kg !in 20.0..300.0 || cm !in 100.0..250.0 || age !in 10..100) null
        else 10 * kg + 6.25 * cm - 5 * age + if (sex == Sex.MALE) 5 else -161

    fun tdee(bmr: Double, level: ActivityLevel): Int = (bmr * level.factor).roundToInt()

    fun waistHipRatio(waistCm: Double, hipCm: Double): Double? =
        if (waistCm !in 40.0..200.0 || hipCm !in 50.0..200.0) null else waistCm / hipCm

    /** WHO: raised risk above 0.90 (men) / 0.85 (women). */
    fun whrRaised(ratio: Double, sex: Sex): Boolean = ratio > if (sex == Sex.MALE) 0.90 else 0.85

    /** About 35 ml per kg per day, rounded to 100 ml: a rough target, more in heat or exercise. */
    fun waterMl(kg: Double): Int? = if (kg !in 20.0..300.0) null else ((kg * 35) / 100).roundToInt() * 100
}

/** Estimated dates from the last period start and a typical cycle length. Cycles vary: never a promise. */
data class CycleEstimate(val nextPeriod: LocalDate, val ovulation: LocalDate, val fertileFrom: LocalDate, val fertileTo: LocalDate)

object Cycle {
    const val MIN_DAYS = 21
    const val MAX_DAYS = 45

    fun estimate(lastStart: LocalDate, cycleDays: Int): CycleEstimate? {
        if (cycleDays !in MIN_DAYS..MAX_DAYS) return null
        val next = lastStart.plusDays(cycleDays.toLong())
        val ov = next.minusDays(14)
        return CycleEstimate(next, ov, ov.minusDays(5), ov.plusDays(1))
    }

    /** Average of the gaps between logged period starts (needs two or more), clamped; else null. */
    fun averageLength(starts: List<LocalDate>): Int? {
        val s = starts.distinct().sorted()
        if (s.size < 2) return null
        val gaps = s.zipWithNext { a, b -> java.time.temporal.ChronoUnit.DAYS.between(a, b).toInt() }.filter { it in MIN_DAYS..MAX_DAYS }
        return if (gaps.isEmpty()) null else gaps.average().roundToInt()
    }
}

/** Break nudge: suggest standing up after this much unbroken focus. */
object BreakNudge {
    fun due(focusMinutesSinceBreak: Int, everyMinutes: Int = 50): Boolean = focusMinutesSinceBreak >= everyMinutes
}

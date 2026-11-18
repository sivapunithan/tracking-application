package com.spcode.tnpscprep.domain

/** The vibration vocabulary and its strength scaling. Pure Kotlin (the Android part is in ui/feedback/Haptics). */
enum class HapticKind { TICK, CLICK, STRONG, SOFT_BUZZ, SUCCESS, ERROR }

enum class HapticStrength(val scale: Float) { LIGHT(0.45f), NORMAL(0.75f), STRONG(1.0f);
    companion object { fun of(i: Int) = entries.getOrElse(i) { NORMAL } }
}

/** One vibration = alternating [timings] (off, on, off, on ...) in ms and the strength [amps] 0..255 of each step (0 for "off" steps). */
class HapticPattern(val timings: LongArray, val amps: IntArray)

object HapticRules {
    private fun p(vararg steps: Pair<Long, Int>) = HapticPattern(steps.map { it.first }.toLongArray(), steps.map { it.second }.toIntArray())

    /** Base pattern at full strength. */
    fun base(k: HapticKind): HapticPattern = when (k) {
        HapticKind.TICK -> p(0L to 0, 9L to 200)
        HapticKind.CLICK -> p(0L to 0, 16L to 230)
        HapticKind.STRONG -> p(0L to 0, 32L to 255)
        HapticKind.SOFT_BUZZ -> p(0L to 0, 45L to 170)
        HapticKind.SUCCESS -> p(0L to 0, 22L to 230, 60L to 0, 34L to 255)
        HapticKind.ERROR -> p(0L to 0, 32L to 255, 45L to 0, 32L to 255, 45L to 0, 50L to 255)
    }

    /** The pattern at a strength: the amplitude of every "on" step is scaled (never below 1), the timings stay. */
    fun scaled(k: HapticKind, s: HapticStrength): HapticPattern {
        val b = base(k)
        return HapticPattern(b.timings, IntArray(b.amps.size) { i -> if (b.amps[i] == 0) 0 else (b.amps[i] * s.scale).toInt().coerceIn(1, 255) })
    }

    /**
     * For phones whose motor has NO amplitude control (this Oppo): strength is shown by the pulse LENGTH instead
     * (Light 0.6x, Normal 1x, Strong 1.5x of the "on" steps; the gaps stay), amplitudes are not used.
     */
    fun scaledByLength(k: HapticKind, s: HapticStrength): HapticPattern {
        val b = base(k)
        val f = when (s) { HapticStrength.LIGHT -> 0.6f; HapticStrength.NORMAL -> 1.0f; HapticStrength.STRONG -> 1.5f }
        return HapticPattern(LongArray(b.timings.size) { i -> if (b.amps[i] == 0) b.timings[i] else maxOf(4L, (b.timings[i] * f).toLong()) }, b.amps)
    }

    /** Which vibration goes with a sound clip (so the sound and the buzz agree). */
    fun forClip(clip: String?): HapticKind? = when (clip) {
        "celebrate", "boom" -> HapticKind.SUCCESS
        "cheer", "laugh", "proud", "photo" -> HapticKind.CLICK
        "cry", "worried", "fail", "later" -> HapticKind.SOFT_BUZZ
        "angry", "slap" -> HapticKind.ERROR
        else -> null
    }
}

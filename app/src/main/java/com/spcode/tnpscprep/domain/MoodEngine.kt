package com.spcode.tnpscprep.domain

/**
 * The reactions the avatar can show (final set, chosen by the owner). Names are used for picture file names
 * ("mood_<name>"): never rename, only add.
 */
enum class Mood {
    LAUGH, CHEER, PROUD, WORRIED, CRY, ANGRY, CELEBRATE;

    val imageName: String get() = "mood_" + name.lowercase()
}

data class MoodInput(
    val streak: Int,
    val goalMet: Boolean,
    /** 0 = studied today, 1 = last studied yesterday, ... ; null = has never studied (new user). */
    val daysSinceStudy: Int?,
    val overdueCount: Int,
    val hour: Int,
)

/**
 * Picks how the avatar feels from her streak and what she has covered. Order matters: the strongest signal wins.
 * Tone is playful, never shaming. PROUD (thumbs-up) is the everyday default.
 */
object MoodEngine {
    fun pick(i: MoodInput): Mood {
        val away = i.daysSinceStudy ?: return Mood.PROUD // brand-new user: an encouraging thumbs-up
        return when {
            away >= 7 -> Mood.ANGRY                    // a week away
            away >= 3 -> Mood.CRY                      // 3-6 days away
            away == 2 -> Mood.WORRIED                  // skipped yesterday and not studied today yet
            i.streak >= 14 -> Mood.CELEBRATE           // two-week streak
            i.streak >= 7 -> Mood.LAUGH                // week streak
            i.goalMet && i.streak >= 3 -> Mood.CHEER   // goal met and on a roll
            i.goalMet -> Mood.PROUD
            i.overdueCount >= 8 -> Mood.WORRIED        // a pile of topics waiting for revision
            i.hour >= 19 -> Mood.WORRIED               // evening and today's goal is not reached yet
            else -> Mood.PROUD
        }
    }
}

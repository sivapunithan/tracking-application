package com.spcode.tnpscprep.domain

/** What she just did that is worth a kind word. Stable names. */
enum class PraiseEvent { TASK_DONE, PLAN_COMPLETE, FOCUS_DONE, WRAPUP_DONE, FOOD_HEALTHY }

/** Which face and which of the written variants a praise notification uses. Pure. */
object PraiseRules {
    const val VARIANTS = 3

    fun moodFor(e: PraiseEvent, goalMet: Boolean = false): Mood = when (e) {
        PraiseEvent.TASK_DONE -> Mood.CHEER
        PraiseEvent.PLAN_COMPLETE -> Mood.CELEBRATE
        PraiseEvent.FOCUS_DONE -> if (goalMet) Mood.CELEBRATE else Mood.PROUD
        PraiseEvent.WRAPUP_DONE -> Mood.PROUD
        PraiseEvent.FOOD_HEALTHY -> Mood.LAUGH
    }

    /** A different variant each time, in turn (never the same wording twice in a row). */
    fun variant(counter: Int): Int = Math.floorMod(counter, VARIANTS)

    /** The face for the evening reminder, from how long she has been away (the same ladder as the avatar). */
    fun reminderMood(daysSinceStudy: Int?, hour: Int): Mood = MoodEngine.pick(MoodInput(streak = 0, goalMet = false, daysSinceStudy = daysSinceStudy, overdueCount = 0, hour = hour))
}

/** Which clip plays for which action. null = silent. Clip names are res/raw/snd_<name>. Pure. */
object MoodSoundRules {
    fun forMood(mood: String?): String? = when (mood) { "CRY" -> "cry"; "ANGRY" -> "angry"; "WORRIED" -> "worried"; else -> null }
    /** Choosing "junk" for the day's food is a playful fail. */
    fun forFood(state: String?): String? = if (state == "JUNK") "fail" else null
    const val RECAP = "later"
}

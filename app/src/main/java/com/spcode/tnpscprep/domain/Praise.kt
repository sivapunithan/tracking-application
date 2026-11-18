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

    /** The clip that comes with the notification (res/raw/snd_<name>); each event also has its own face above. */
    fun soundFor(e: PraiseEvent, goalMet: Boolean = false): String = when (e) {
        PraiseEvent.TASK_DONE -> "cheer"
        PraiseEvent.PLAN_COMPLETE -> "boom"
        PraiseEvent.FOCUS_DONE -> if (goalMet) "celebrate" else "proud"
        PraiseEvent.WRAPUP_DONE -> "proud"
        PraiseEvent.FOOD_HEALTHY -> "laugh"
    }

    /** A different variant each time, in turn (never the same wording twice in a row). */
    fun variant(counter: Int): Int = Math.floorMod(counter, VARIANTS)

    /** The face for the evening reminder, from how long she has been away (the same ladder as the avatar). */
    fun reminderMood(daysSinceStudy: Int?, hour: Int): Mood = MoodEngine.pick(MoodInput(streak = 0, goalMet = false, daysSinceStudy = daysSinceStudy, overdueCount = 0, hour = hour))
}

/** Which clip plays for which action. null = silent. Clip names are res/raw/snd_<name>. Pure. */
object MoodSoundRules {
    fun forMood(mood: String?): String? = when (mood) { "CRY" -> "cry"; "ANGRY" -> "angry"; "WORRIED" -> "worried"
        "LAUGH" -> "laugh"; "CHEER" -> "cheer"; "PROUD" -> "proud"; "CELEBRATE" -> "celebrate"; else -> null }
    /** Choosing "junk" for the day's food is a playful fail. */
    fun forFood(state: String?): String? = when (state) { "JUNK" -> "fail"; "MIXED" -> "worried"; else -> null }   // "huh" for a so-so day
    const val RECAP = "later"
    /** A topic moved up a level; reaching the top level ("Revision 3") is a small celebration. */
    fun forTopicLevelUp(newLevel: Int): String? = when { newLevel >= 4 -> "celebrate"; newLevel >= 1 -> "cheer"; else -> null }
    /** Water: only crossing the day's target makes a sound (every glass would be too much). */
    fun forWater(beforeMl: Int, addMl: Int, targetMl: Int): String? = if (targetMl > 0 && beforeMl < targetMl && beforeMl + addMl >= targetMl) "celebrate" else null
    const val DELETED = "worried"        // "huh?": something was removed
    const val UNDONE = "cheer"
    const val BACKUP_SAVED = "cheer"
    const val RESTORED = "celebrate"
    const val EXAM_DATE_SET = "boom"
    /** A wrong PIN: a playful slap (the dots shake too). */
    const val WRONG_PIN = "slap"
    /** The whole day's plan ticked off. */
    const val PLAN_DONE = "boom"
    /** A photo was placed in a diary entry. */
    const val PHOTO = "photo"
}

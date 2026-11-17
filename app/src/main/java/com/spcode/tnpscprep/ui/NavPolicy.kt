package com.spcode.tnpscprep.ui

/** Pure navigation decisions (testable without Android). */
object NavPolicy {
    const val FOCUS = "focus"
    const val PROFILE = "profile"
    const val EDITOR_PREFIX = "journal/editor"

    /** Full-screen pages hide the floating bar: the timer, the profile and the diary editor. */
    fun isFullScreen(route: String?): Boolean = route == FOCUS || route == PROFILE || route?.startsWith(EDITOR_PREFIX) == true
}

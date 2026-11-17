package com.spcode.tnpscprep.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavPolicyTest {
    @Test fun tabRootsShowTheBar() {
        listOf("today", "study", "care", "journal", null).forEach { assertFalse(it.toString(), NavPolicy.isFullScreen(it)) }
    }

    @Test fun focusProfileAndTheEditorHideTheBar() {
        assertTrue(NavPolicy.isFullScreen("focus"))
        assertTrue(NavPolicy.isFullScreen("profile"))
        assertTrue(NavPolicy.isFullScreen("journal/editor?id={id}&date={date}"))
        assertTrue(NavPolicy.isFullScreen("journal/editor"))
    }
}

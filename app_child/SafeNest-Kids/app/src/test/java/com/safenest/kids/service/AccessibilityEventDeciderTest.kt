package com.safenest.kids.service

import android.view.accessibility.AccessibilityEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityEventDeciderTest {
    @Test
    fun foregroundWindowEventsIncludeStateAndWindowsTransitionsOnly() {
        assertTrue(AccessibilityEventDecider.isForegroundWindowEvent(AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED))
        assertTrue(AccessibilityEventDecider.isForegroundWindowEvent(AccessibilityEvent.TYPE_WINDOWS_CHANGED))
        assertFalse(AccessibilityEventDecider.isForegroundWindowEvent(AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED))
    }
}

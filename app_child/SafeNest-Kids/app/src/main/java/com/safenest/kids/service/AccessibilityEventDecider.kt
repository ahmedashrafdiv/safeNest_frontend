package com.safenest.kids.service

import android.view.accessibility.AccessibilityEvent

/** Identifies event types that represent a foreground application transition. */
internal object AccessibilityEventDecider {
    fun isForegroundWindowEvent(eventType: Int): Boolean =
        eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
            eventType == AccessibilityEvent.TYPE_WINDOWS_CHANGED
}

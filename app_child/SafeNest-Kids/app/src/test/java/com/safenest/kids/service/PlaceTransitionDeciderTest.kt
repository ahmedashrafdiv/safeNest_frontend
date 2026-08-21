package com.safenest.kids.service

import com.google.android.gms.location.Geofence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaceTransitionDeciderTest {
    @Test
    fun supported_transitions_are_named_for_the_backend_contract() {
        assertEquals("enter", PlaceTransitionDecider.transitionName(Geofence.GEOFENCE_TRANSITION_ENTER))
        assertEquals("exit", PlaceTransitionDecider.transitionName(Geofence.GEOFENCE_TRANSITION_EXIT))
        assertEquals(null, PlaceTransitionDecider.transitionName(99))
    }

    @Test
    fun queueing_requires_paired_active_session_and_real_place() {
        assertTrue(PlaceTransitionDecider.shouldQueue(true, false, "enter", listOf("place-1")))
        assertFalse(PlaceTransitionDecider.shouldQueue(false, false, "enter", listOf("place-1")))
        assertFalse(PlaceTransitionDecider.shouldQueue(true, true, "enter", listOf("place-1")))
        assertFalse(PlaceTransitionDecider.shouldQueue(true, false, "dwell", listOf("place-1")))
        assertFalse(PlaceTransitionDecider.shouldQueue(true, false, "exit", emptyList()))
    }

    @Test
    fun permission_denial_is_a_settled_degraded_state_not_a_retry_outcome() {
        assertEquals(PlaceTransitionDecider.SyncOutcome.SUCCESS, PlaceTransitionDecider.syncOutcome("active"))
        assertEquals(PlaceTransitionDecider.SyncOutcome.PERMISSION_DENIED, PlaceTransitionDecider.syncOutcome("permission_denied"))
        assertEquals(PlaceTransitionDecider.SyncOutcome.RETRY, PlaceTransitionDecider.syncOutcome("failed"))
    }
}

package com.safenest.kids.service

import com.google.android.gms.location.Geofence

/** Android-free decision surface for supported parent-visible place transitions. */
object PlaceTransitionDecider {
    enum class SyncOutcome { SUCCESS, PERMISSION_DENIED, RETRY }
    fun transitionName(value: Int): String? = when (value) {
        Geofence.GEOFENCE_TRANSITION_ENTER -> "enter"
        Geofence.GEOFENCE_TRANSITION_EXIT -> "exit"
        else -> null
    }

    fun shouldQueue(isPaired: Boolean, isProtectionSuspended: Boolean, transition: String?, placeIds: List<String>): Boolean =
        isPaired && !isProtectionSuspended && transition in setOf("enter", "exit") && placeIds.isNotEmpty()

    fun syncOutcome(status: String): SyncOutcome = when (status) {
        "active" -> SyncOutcome.SUCCESS
        "permission_denied" -> SyncOutcome.PERMISSION_DENIED
        else -> SyncOutcome.RETRY
    }
}

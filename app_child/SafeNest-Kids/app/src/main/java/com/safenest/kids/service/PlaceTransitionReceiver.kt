package com.safenest.kids.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import com.safenest.kids.util.PrefsHelper
import java.util.UUID

class PlaceTransitionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val event = GeofencingEvent.fromIntent(intent) ?: return
        if (event.hasError()) return
        val transition = PlaceTransitionDecider.transitionName(event.geofenceTransition) ?: return
        val placeIds = event.triggeringGeofences?.map { it.requestId }.orEmpty()
        val prefs = PrefsHelper(context)
        if (!PlaceTransitionDecider.shouldQueue(prefs.isPaired(), prefs.isProtectionSuspended(), transition, placeIds)) return
        val occurredAt = System.currentTimeMillis()
        placeIds.forEach { placeId ->
            PlaceTransitionUploadWorker.enqueue(context, UUID.randomUUID().toString(), placeId, transition, occurredAt)
        }
        prefs.setPlaceLastTransitionId(placeIds.last())
    }
}

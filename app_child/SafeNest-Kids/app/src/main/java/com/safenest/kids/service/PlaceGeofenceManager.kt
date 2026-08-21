package com.safenest.kids.service

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.safenest.kids.network.ChildPlacesResponse
import com.safenest.kids.util.PrefsHelper

/** Registers only Parent-supplied circular boundaries. The child does not author or evaluate policies. */
object PlaceGeofenceManager {
    private const val ACTION_TRANSITION = "com.safenest.kids.PLACE_GEOFENCE_TRANSITION"
    enum class ApplyResult { ACTIVE, PERMISSION_DENIED, TRANSIENT_FAILURE }

    fun apply(context: Context, policy: ChildPlacesResponse, onComplete: (ApplyResult) -> Unit) {
        val prefs = PrefsHelper(context)
        if (!hasRequiredPermission(context)) {
            prefs.setPlaceGeofenceStatus(PrefsHelper.PLACE_GEOFENCE_PERMISSION_DENIED)
            onComplete(ApplyResult.PERMISSION_DENIED)
            return
        }
        val client = LocationServices.getGeofencingClient(context)
        client.removeGeofences(pendingIntent(context)).addOnCompleteListener {
            if (policy.places.isEmpty()) {
                prefs.setPlacePolicyVersion(policy.placeVersion)
                prefs.setPlaceGeofenceStatus(PrefsHelper.PLACE_GEOFENCE_ACTIVE)
                onComplete(ApplyResult.ACTIVE)
                return@addOnCompleteListener
            }
            val geofences = policy.places.take(50).map { place ->
                Geofence.Builder()
                    .setRequestId(place.placeId)
                    .setCircularRegion(place.latitude, place.longitude, place.radiusMeters.toFloat())
                    .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER or Geofence.GEOFENCE_TRANSITION_EXIT)
                    .setExpirationDuration(Geofence.NEVER_EXPIRE)
                    .setNotificationResponsiveness(60_000)
                    .build()
            }
            val request = GeofencingRequest.Builder()
                .setInitialTrigger(0)
                .addGeofences(geofences)
                .build()
            try {
                client.addGeofences(request, pendingIntent(context)).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        prefs.setPlacePolicyVersion(policy.placeVersion)
                        prefs.setPlaceGeofenceStatus(PrefsHelper.PLACE_GEOFENCE_ACTIVE)
                    } else {
                        prefs.setPlaceGeofenceStatus(PrefsHelper.PLACE_GEOFENCE_FAILED)
                    }
                    onComplete(if (task.isSuccessful) ApplyResult.ACTIVE else ApplyResult.TRANSIENT_FAILURE)
                }
            } catch (_: SecurityException) {
                prefs.setPlaceGeofenceStatus(PrefsHelper.PLACE_GEOFENCE_PERMISSION_DENIED)
                onComplete(ApplyResult.PERMISSION_DENIED)
            }
        }
    }

    fun clear(context: Context) {
        LocationServices.getGeofencingClient(context).removeGeofences(pendingIntent(context))
        PrefsHelper(context).setPlaceGeofenceStatus(PrefsHelper.PLACE_GEOFENCE_UNAVAILABLE)
    }

    private fun hasRequiredPermission(context: Context): Boolean {
        val foreground = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val background = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q || ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        return foreground && background
    }

    private fun pendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, PlaceTransitionReceiver::class.java).setAction(ACTION_TRANSITION)
        return PendingIntent.getBroadcast(context, 3013, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }
}

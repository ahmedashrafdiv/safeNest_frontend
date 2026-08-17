package com.safenest.kids.service

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

object PhoneLocationDecider {
    const val DEFAULT_MIN_INTERVAL_MILLIS = 30_000L
    const val DEFAULT_MIN_DISPLACEMENT_METERS = 25.0
    const val DEFAULT_MAX_ACCURACY_METERS = 1_000f
    const val DEFAULT_STALE_AFTER_MILLIS = 900_000L

    data class LocationSample(
        val latitude: Double,
        val longitude: Double,
        val accuracyMeters: Float,
        val capturedAtMillis: Long,
        val altitudeMeters: Double? = null,
        val speedMps: Float? = null
    )

    enum class TrackingStatus {
        ACTIVE,
        PERMISSION_DENIED,
        SERVICE_STOPPED,
        OFFLINE,
        STALE,
        DISABLED,
        UNAVAILABLE
    }

    fun isValid(sample: LocationSample, nowMillis: Long): Boolean {
        if (sample.latitude !in -90.0..90.0 || sample.longitude !in -180.0..180.0) return false
        if (sample.accuracyMeters <= 0f || sample.accuracyMeters > DEFAULT_MAX_ACCURACY_METERS) return false
        if (sample.capturedAtMillis > nowMillis + 5 * 60_000L) return false
        return true
    }

    fun shouldUpload(
        sample: LocationSample,
        previous: LocationSample?,
        nowMillis: Long,
        minIntervalMillis: Long = DEFAULT_MIN_INTERVAL_MILLIS,
        minDisplacementMeters: Double = DEFAULT_MIN_DISPLACEMENT_METERS
    ): Boolean {
        if (!isValid(sample, nowMillis)) return false
        if (previous == null) return true
        val elapsed = sample.capturedAtMillis - previous.capturedAtMillis
        if (elapsed < 0L) return false
        if (elapsed >= minIntervalMillis) return true
        if (sample.accuracyMeters < previous.accuracyMeters && sample.accuracyMeters <= DEFAULT_MAX_ACCURACY_METERS / 2f) {
            return true
        }
        return distanceMeters(previous, sample) >= minDisplacementMeters
    }

    fun status(
        enabled: Boolean,
        permissionGranted: Boolean,
        serviceActive: Boolean,
        networkAvailable: Boolean,
        lastSuccessfulUploadMillis: Long?,
        nowMillis: Long,
        staleAfterMillis: Long = DEFAULT_STALE_AFTER_MILLIS
    ): TrackingStatus {
        if (!enabled) return TrackingStatus.DISABLED
        if (!permissionGranted) return TrackingStatus.PERMISSION_DENIED
        if (!serviceActive) return TrackingStatus.SERVICE_STOPPED
        if (!networkAvailable) return TrackingStatus.OFFLINE
        if (lastSuccessfulUploadMillis == null) return TrackingStatus.UNAVAILABLE
        return if (nowMillis - lastSuccessfulUploadMillis > staleAfterMillis) TrackingStatus.STALE else TrackingStatus.ACTIVE
    }

    fun distanceMeters(first: LocationSample, second: LocationSample): Double {
        val earthRadiusMeters = 6_371_000.0
        val lat1 = Math.toRadians(first.latitude)
        val lat2 = Math.toRadians(second.latitude)
        val deltaLat = Math.toRadians(second.latitude - first.latitude)
        val deltaLon = Math.toRadians(second.longitude - first.longitude)
        val a = sin(deltaLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(deltaLon / 2).pow(2)
        return earthRadiusMeters * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}

package com.safenest.kids.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.safenest.kids.util.PermissionsHelper
import com.safenest.kids.util.PrefsHelper
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class PhoneLocationService : Service(), LocationListener {
    private lateinit var locationManager: LocationManager
    private lateinit var prefs: PrefsHelper

    override fun onCreate() {
        super.onCreate()
        prefs = PrefsHelper(this)
        createNotificationChannel()
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!prefs.isPhoneTrackingEnabled() || !PermissionsHelper.hasLocationPermission(this)) {
            prefs.setPhoneTrackingServiceState(PrefsHelper.PHONE_LOCATION_SERVICE_FAILED)
            prefs.setPhoneTrackingStatus(
                if (PermissionsHelper.hasLocationPermission(this)) PrefsHelper.PHONE_LOCATION_STATUS_DISABLED
                else PrefsHelper.PHONE_LOCATION_STATUS_PERMISSION_DENIED
            )
            stopSelf()
            return START_NOT_STICKY
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(NOTIFICATION_ID, buildNotification(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            prefs.setPhoneTrackingPermissionState(PrefsHelper.PHONE_LOCATION_PERMISSION_GRANTED)
            prefs.setPhoneTrackingServiceState(PrefsHelper.PHONE_LOCATION_SERVICE_ACTIVE)
            requestUpdates()
        } catch (security: SecurityException) {
            Log.e(TAG, "Location permission was lost while starting service", security)
            prefs.setPhoneTrackingPermissionState(PrefsHelper.PHONE_LOCATION_PERMISSION_DENIED)
            prefs.setPhoneTrackingServiceState(PrefsHelper.PHONE_LOCATION_SERVICE_FAILED)
            prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_PERMISSION_DENIED)
            stopSelf()
        } catch (error: Exception) {
            Log.e(TAG, "Could not start phone location service", error)
            prefs.setPhoneTrackingServiceState(PrefsHelper.PHONE_LOCATION_SERVICE_FAILED)
            prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_UNAVAILABLE)
            stopSelf()
        }
        return START_STICKY
    }

    private fun requestUpdates() {
        val hasFine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        if (!hasFine && !hasCoarse) throw SecurityException("Location permission is not granted")
        val providers = buildList {
            if (hasFine && locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) add(LocationManager.GPS_PROVIDER)
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) add(LocationManager.NETWORK_PROVIDER)
        }
        if (providers.isEmpty()) {
            prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_UNAVAILABLE)
            return
        }
        providers.forEach { provider ->
            locationManager.requestLocationUpdates(provider, UPDATE_INTERVAL_MILLIS, MIN_DISPLACEMENT_METERS, this)
        }
    }

    override fun onLocationChanged(location: Location) {
        val sample = PhoneLocationDecider.LocationSample(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracyMeters = location.accuracy,
            capturedAtMillis = location.time,
            altitudeMeters = if (location.hasAltitude()) location.altitude else null,
            speedMps = if (location.hasSpeed()) location.speed else null
        )
        val previous = previousSample()
        if (!PhoneLocationDecider.shouldUpload(sample, previous, System.currentTimeMillis())) return
        PhoneLocationSyncWorker.enqueue(this, sample)
        prefs.setPhoneTrackingLastLatitude(sample.latitude)
        prefs.setPhoneTrackingLastLongitude(sample.longitude)
        prefs.setPhoneTrackingLastAccuracy(sample.accuracyMeters)
        prefs.setPhoneTrackingLastCapturedAt(isoUtc(sample.capturedAtMillis))
        prefs.setPhoneTrackingPermissionState(PrefsHelper.PHONE_LOCATION_PERMISSION_GRANTED)
        prefs.setPhoneTrackingServiceState(PrefsHelper.PHONE_LOCATION_SERVICE_ACTIVE)
    }

    private fun previousSample(): PhoneLocationDecider.LocationSample? {
        val latitude = prefs.getPhoneTrackingLastLatitude() ?: return null
        val longitude = prefs.getPhoneTrackingLastLongitude() ?: return null
        val accuracy = prefs.getPhoneTrackingLastAccuracy() ?: return null
        val capturedAt = prefs.getPhoneTrackingLastCapturedAt()?.let(::parseIso) ?: return null
        return PhoneLocationDecider.LocationSample(latitude, longitude, accuracy, capturedAt)
    }

    override fun onProviderEnabled(provider: String) = Unit
    override fun onProviderDisabled(provider: String) {
        prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_UNAVAILABLE)
    }
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit

    override fun onDestroy() {
        if (::locationManager.isInitialized) locationManager.removeUpdates(this)
        if (::prefs.isInitialized) {
            prefs.setPhoneTrackingServiceState(PrefsHelper.PHONE_LOCATION_SERVICE_STOPPED)
            if (prefs.isPhoneTrackingEnabled()) prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_SERVICE_STOPPED)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Child location tracking", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification =
        NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle("Layngo location tracking")
            .setContentText("Child phone location protection is active")
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()

    private fun parseIso(value: String): Long? = runCatching {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(value)?.time
    }.getOrNull()

    private fun isoUtc(millis: Long): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }.format(Date(millis))

    companion object {
        private const val TAG = "PhoneLocationService"
        private const val CHANNEL_ID = "phone_location_tracking"
        private const val NOTIFICATION_ID = 7301
        private const val UPDATE_INTERVAL_MILLIS = 60_000L
        private const val MIN_DISPLACEMENT_METERS = 25f

        fun startIfPermissionGranted(context: Context): Boolean {
            val prefs = PrefsHelper(context)
            if (!prefs.isPhoneTrackingEnabled()) {
                prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_DISABLED)
                return false
            }
            if (!PermissionsHelper.hasLocationPermission(context)) {
                prefs.setPhoneTrackingPermissionState(PrefsHelper.PHONE_LOCATION_PERMISSION_DENIED)
                prefs.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_PERMISSION_DENIED)
                return false
            }
            prefs.setPhoneTrackingServiceState(PrefsHelper.PHONE_LOCATION_SERVICE_STARTING)
            val intent = Intent(context, PhoneLocationService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) ContextCompat.startForegroundService(context, intent)
            else context.startService(intent)
            return true
        }
    }
}

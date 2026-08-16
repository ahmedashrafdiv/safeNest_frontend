package com.safenest.kids.util

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import java.util.Calendar

object AppUsageHelper {

    /**
     * Returns today's per-app foreground usage in **minutes**.
     *
     * Uses [UsageEvents] (requires the PACKAGE_USAGE_STATS permission
     * already granted in Phase 3 via the Usage Access settings screen).
     *
     * Event-based calculation clamped to [midnight-today, now].
     *
     * **Session pairing**: ONLY [MOVE_TO_FOREGROUND] (type 1) opens a
     * session and ONLY [MOVE_TO_BACKGROUND] (type 2) closes it.
     * [ACTIVITY_STOPPED] (type 23) is intentionally ignored because it
     * arrives late and refers to the *previous* activity, causing it to
     * incorrectly close the *new* activity's session (keyed by package).
     *
     * **Midnight-boundary fix**: looks back 24 h before midnight to
     * discover which apps were already in the foreground, and seeds
     * their session start at exactly startTime.
     */
    fun getTodayUsageStats(context: Context): Map<String, Long> {
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE)
                as UsageStatsManager

        // Midnight today, device timezone
        val startTime = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val endTime = System.currentTimeMillis()

        // ── Diagnostic: log the query window ──
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.US)
        Log.d("AppUsageReport",
            "QUERY WINDOW: start=${sdf.format(java.util.Date(startTime))} end=${sdf.format(java.util.Date(endTime))}")

        // ── STEP 1: determine which packages were already in foreground at midnight,
        //    using ONLY MOVE_TO_FOREGROUND / MOVE_TO_BACKGROUND (not ACTIVITY_STOPPED) ──
        val lookbackStart = startTime - 24 * 60 * 60 * 1000L
        val preEvents = usm.queryEvents(lookbackStart, startTime)
        val lastState = mutableMapOf<String, Int>()
        val preEvent = UsageEvents.Event()
        while (preEvents.hasNextEvent()) {
            preEvents.getNextEvent(preEvent)
            val pkg = preEvent.packageName ?: continue
            if (pkg == context.packageName) continue
            when (preEvent.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND ->
                    lastState[pkg] = UsageEvents.Event.MOVE_TO_FOREGROUND
                UsageEvents.Event.MOVE_TO_BACKGROUND ->
                    lastState[pkg] = UsageEvents.Event.MOVE_TO_BACKGROUND
                // ACTIVITY_STOPPED intentionally ignored — unreliable for pairing
            }
        }

        val sessionStart = mutableMapOf<String, Long>()
        lastState.forEach { (pkg, state) ->
            if (state == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                sessionStart[pkg] = startTime
            }
        }
        Log.d("AppUsageReport", "=== ALREADY OPEN AT MIDNIGHT: ${sessionStart.keys} ===")

        // ── DIAGNOSTIC: raw event trace for target packages ──
        val traceEvents = usm.queryEvents(startTime, endTime)
        val traceEvent = UsageEvents.Event()
        val sdf3 = java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.US)
        val targets = setOf("com.whatsapp", "com.pinterest")
        Log.d("AppUsageReport", "=== RAW EVENT TRACE (whatsapp + pinterest) ===")
        while (traceEvents.hasNextEvent()) {
            traceEvents.getNextEvent(traceEvent)
            if (traceEvent.packageName in targets) {
                Log.d("AppUsageReport",
                    "TRACE pkg=${traceEvent.packageName} type=${traceEvent.eventType} " +
                    "class=${traceEvent.className} time=${sdf3.format(java.util.Date(traceEvent.timeStamp))}")
            }
        }
        Log.d("AppUsageReport", "=== END TRACE ===")

        // ── STEP 2: process today's events — ONLY type 1 opens, ONLY type 2 closes ──
        val events = usm.queryEvents(startTime, endTime)
        val totalMillis = mutableMapOf<String, Long>()
        val event = UsageEvents.Event()

        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val pkg = event.packageName ?: continue
            if (pkg == context.packageName) continue

            when (event.eventType) {
                UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                    sessionStart[pkg] = event.timeStamp.coerceAtLeast(startTime)
                }
                UsageEvents.Event.MOVE_TO_BACKGROUND -> {
                    val start = sessionStart.remove(pkg)
                    if (start != null) {
                        val end = event.timeStamp.coerceAtMost(endTime)
                        if (end > start) {
                            totalMillis[pkg] = (totalMillis[pkg] ?: 0L) + (end - start)
                        }
                    }
                }
                // ACTIVITY_STOPPED (23) and everything else (notifications, foreground
                // services, standby bucket changes) are intentionally ignored — they
                // do not represent reliable foreground/background transitions per package
            }
        }

        // ── STEP 3: sessions still open at endTime ──
        sessionStart.forEach { (pkg, start) ->
            if (endTime > start) {
                totalMillis[pkg] = (totalMillis[pkg] ?: 0L) + (endTime - start)
            }
        }

        // ── Diagnostic ──
        Log.d("AppUsageReport",
            "=== EVENT-BASED CALC: whatsapp raw millis = ${totalMillis["com.whatsapp"]} ===")
        Log.d("AppUsageReport",
            "=== EVENT-BASED CALC: pinterest raw millis = ${totalMillis["com.pinterest"]} ===")

        // Convert millis → minutes
        return totalMillis
            .mapValues { (_, millis) -> millis / 60_000L }
            .filter { it.value > 0L }   // drop apps that round down to 0 min
    }
}

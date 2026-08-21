package com.safenest.kids.service

class CaptureScheduler(
    private val minimumIntervalMillis: Long = 333L,
) {
    private var nextRequestId = 1L
    private var lastRequestAtMillis: Long = Long.MIN_VALUE
    private var inFlight = false

    @Synchronized
    fun request(nowMillis: Long): CaptureRequest? {
        if (inFlight) return null
        if (lastRequestAtMillis != Long.MIN_VALUE && nowMillis - lastRequestAtMillis < minimumIntervalMillis) {
            return null
        }
        inFlight = true
        lastRequestAtMillis = nowMillis
        return CaptureRequest(nextRequestId++, nowMillis)
    }

    @Synchronized
    fun complete() {
        inFlight = false
    }

    @Synchronized
    fun fail() {
        inFlight = false
    }

    fun isInFlight(): Boolean = inFlight
}

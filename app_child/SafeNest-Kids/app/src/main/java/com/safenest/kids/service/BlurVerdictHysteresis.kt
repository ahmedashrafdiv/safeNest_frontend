package com.safenest.kids.service

class BlurVerdictHysteresis(
    private val requiredSafeObservations: Int = 3,
) {
    private var safeCount = 0

    fun observe(verdict: Verdict): Boolean {
        if (verdict != Verdict.SAFE) {
            safeCount = 0
            return false
        }
        safeCount += 1
        return safeCount >= requiredSafeObservations
    }

    fun reset() {
        safeCount = 0
    }

    fun safeObservationCount(): Int = safeCount
}

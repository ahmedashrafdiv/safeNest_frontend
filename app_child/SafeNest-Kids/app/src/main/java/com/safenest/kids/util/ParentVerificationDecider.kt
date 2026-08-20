package com.safenest.kids.util

import com.google.gson.Gson
import com.google.gson.JsonObject

/**
 * Maps the response of `POST /api/child-devices/{device_id}/parent-verification` onto the outcome
 * the dialog reports.
 */
object ParentVerificationDecider {

    enum class Outcome {
        VERIFIED,
        WRONG_PASSWORD,

        /** Verification is locked after five consecutive failures. */
        LOCKED,

        /** The device token itself was rejected; the pairing needs attention, not the password. */
        SESSION_EXPIRED,

        /** The token is valid but does not govern this device or child. */
        NOT_AUTHORIZED,

        /** The bound parent record or its password is missing on the Backend. */
        ACCOUNT_UNAVAILABLE,

        /** Offline, timed out, or an error the dialog cannot explain more precisely. */
        UNAVAILABLE,
    }

    private const val WRONG_PASSWORD_CODE = "invalid_parent_password"

    /**
     * [verified] is the response body's own affirmation, and [errorCode] is the `detail.code` of the
     * error body, or `null` when the response carried none.
     *
     * This decides whether a device may be unpaired or its protection switched off, so a success is
     * only a success when the body says so: a 2xx that does not affirm verification fails closed
     * rather than being read as consent.
     *
     * The [errorCode] distinction matters at 401: the endpoint answers 401 for a wrong parent
     * password, and the device-token dependency in front of it answers 401 for an expired token.
     * Reporting "wrong password" for an expired pairing would send the parent hunting for a typo
     * that is not there.
     */
    fun outcome(httpCode: Int, verified: Boolean, errorCode: String?): Outcome = when {
        httpCode in 200..299 && verified -> Outcome.VERIFIED
        httpCode in 200..299 -> Outcome.UNAVAILABLE
        httpCode == 401 && errorCode == WRONG_PASSWORD_CODE -> Outcome.WRONG_PASSWORD
        httpCode == 401 -> Outcome.SESSION_EXPIRED
        httpCode == 429 -> Outcome.LOCKED
        httpCode == 403 -> Outcome.NOT_AUTHORIZED
        httpCode == 404 -> Outcome.ACCOUNT_UNAVAILABLE
        else -> Outcome.UNAVAILABLE
    }

    /** The outcome for a request that never produced a response. */
    fun offlineOutcome(): Outcome = Outcome.UNAVAILABLE

    /**
     * Pull `detail.code` out of a FastAPI error body. Returns `null` for a body that is absent,
     * malformed, or shaped differently, so a parsing accident degrades to the status-only mapping
     * instead of throwing inside the dialog.
     */
    fun errorCodeOf(errorBody: String?): String? {
        if (errorBody.isNullOrBlank()) return null
        return try {
            val detail = Gson().fromJson(errorBody, JsonObject::class.java)?.get("detail")
            if (detail == null || !detail.isJsonObject) {
                null
            } else {
                val code = detail.asJsonObject.get("code")
                if (code == null || !code.isJsonPrimitive) null else code.asString
            }
        } catch (e: Exception) {
            null
        }
    }
}

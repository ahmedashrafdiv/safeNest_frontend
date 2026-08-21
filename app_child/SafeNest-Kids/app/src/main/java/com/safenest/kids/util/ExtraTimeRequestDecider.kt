package com.safenest.kids.util

import com.safenest.kids.network.AccessRequestCreateRequest
import com.safenest.kids.network.ExtraTimeRequest

/** Pure contract for the single daily extra-time request exposed by the Child Home screen. */
object ExtraTimeRequestDecider {
    const val DEFAULT_REQUESTED_SECONDS = 30 * 60

    enum class Outcome {
        SUBMITTED,
        DUPLICATE,
        FAILED,
    }

    fun build(clientRequestId: String): AccessRequestCreateRequest = AccessRequestCreateRequest(
        requestType = ExtraTimeRequest.REQUEST_TYPE,
        scopeType = ExtraTimeRequest.SCOPE_TYPE,
        scopeValue = ExtraTimeRequest.SCOPE_VALUE,
        requestedSeconds = DEFAULT_REQUESTED_SECONDS,
        clientRequestId = clientRequestId,
    )

    fun outcome(httpCode: Int, duplicate: Boolean, requestId: String?): Outcome = when {
        httpCode !in 200..299 -> Outcome.FAILED
        duplicate -> Outcome.DUPLICATE
        requestId.isNullOrBlank() -> Outcome.FAILED
        else -> Outcome.SUBMITTED
    }
}

package com.safenest.kids.util

import com.safenest.kids.network.ExtraTimeRequest
import org.junit.Assert.assertEquals
import org.junit.Test

class ExtraTimeRequestDeciderTest {
    @Test
    fun test_builds_daily_screen_time_contract_within_backend_bounds() {
        val request = ExtraTimeRequestDecider.build("request-1")

        assertEquals(ExtraTimeRequest.REQUEST_TYPE, request.requestType)
        assertEquals(ExtraTimeRequest.SCOPE_TYPE, request.scopeType)
        assertEquals(ExtraTimeRequest.SCOPE_VALUE, request.scopeValue)
        assertEquals(ExtraTimeRequestDecider.DEFAULT_REQUESTED_SECONDS, request.requestedSeconds)
        assertEquals("request-1", request.clientRequestId)
    }

    @Test
    fun test_duplicate_response_is_not_misreported_as_a_new_request() {
        assertEquals(ExtraTimeRequestDecider.Outcome.DUPLICATE, ExtraTimeRequestDecider.outcome(201, true, null))
    }

    @Test
    fun test_successful_response_with_server_id_is_submitted() {
        assertEquals(ExtraTimeRequestDecider.Outcome.SUBMITTED, ExtraTimeRequestDecider.outcome(201, false, "server-id"))
    }

    @Test
    fun test_empty_or_unsuccessful_response_fails_closed() {
        assertEquals(ExtraTimeRequestDecider.Outcome.FAILED, ExtraTimeRequestDecider.outcome(201, false, null))
        assertEquals(ExtraTimeRequestDecider.Outcome.FAILED, ExtraTimeRequestDecider.outcome(422, false, null))
    }
}

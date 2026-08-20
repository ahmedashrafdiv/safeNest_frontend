package com.safenest.kids.util

import com.safenest.kids.util.ParentVerificationDecider.Outcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ParentVerificationDeciderTest {

    @Test
    fun successVerifies() {
        assertEquals(Outcome.VERIFIED, ParentVerificationDecider.outcome(200, true, null))
    }

    @Test
    fun successWithoutTheVerifiedFlagFailsClosed() {
        assertEquals(Outcome.UNAVAILABLE, ParentVerificationDecider.outcome(200, false, null))
    }

    @Test
    fun anySuccessStatusWithTheFlagVerifies() {
        assertEquals(Outcome.VERIFIED, ParentVerificationDecider.outcome(204, true, null))
    }

    @Test
    fun wrongPasswordIsReportedAsSuch() {
        assertEquals(
            Outcome.WRONG_PASSWORD,
            ParentVerificationDecider.outcome(401, false, "invalid_parent_password"),
        )
    }

    @Test
    fun unauthenticatedDeviceIsNotReportedAsAWrongPassword() {
        assertEquals(Outcome.SESSION_EXPIRED, ParentVerificationDecider.outcome(401, false, null))
        assertEquals(Outcome.SESSION_EXPIRED, ParentVerificationDecider.outcome(401, false, "invalid_token"))
    }

    @Test
    fun lockoutIsDistinctFromAWrongPassword() {
        assertEquals(Outcome.LOCKED, ParentVerificationDecider.outcome(429, false, "verification_locked"))
    }

    @Test
    fun deviceScopeMismatchIsNotAuthorized() {
        assertEquals(Outcome.NOT_AUTHORIZED, ParentVerificationDecider.outcome(403, false, "device_not_authorized"))
    }

    @Test
    fun missingParentRecordIsAnAccountProblem() {
        assertEquals(Outcome.ACCOUNT_UNAVAILABLE, ParentVerificationDecider.outcome(404, false, "parent_not_found"))
    }

    @Test
    fun serverErrorsDegradeToUnavailable() {
        assertEquals(Outcome.UNAVAILABLE, ParentVerificationDecider.outcome(500, false, null))
        assertEquals(Outcome.UNAVAILABLE, ParentVerificationDecider.outcome(502, false, null))
        assertEquals(Outcome.UNAVAILABLE, ParentVerificationDecider.offlineOutcome())
    }

    @Test
    fun errorCodeIsReadFromTheFastApiDetailObject() {
        val body = """{"detail":{"code":"invalid_parent_password","message":"Parent password is incorrect"}}"""

        assertEquals("invalid_parent_password", ParentVerificationDecider.errorCodeOf(body))
    }

    @Test
    fun unparsableErrorBodiesDegradeToNull() {
        assertNull(ParentVerificationDecider.errorCodeOf(null))
        assertNull(ParentVerificationDecider.errorCodeOf(""))
        assertNull(ParentVerificationDecider.errorCodeOf("   "))
        assertNull(ParentVerificationDecider.errorCodeOf("<html>502 Bad Gateway</html>"))
        assertNull(ParentVerificationDecider.errorCodeOf("""{"detail":"Not authenticated"}"""))
        assertNull(ParentVerificationDecider.errorCodeOf("""{"message":"boom"}"""))
    }

    @Test
    fun aStringDetailFallsBackToTheStatusMapping() {
        val body = """{"detail":"Not authenticated"}"""

        assertEquals(
            Outcome.SESSION_EXPIRED,
            ParentVerificationDecider.outcome(401, false, ParentVerificationDecider.errorCodeOf(body)),
        )
    }
}

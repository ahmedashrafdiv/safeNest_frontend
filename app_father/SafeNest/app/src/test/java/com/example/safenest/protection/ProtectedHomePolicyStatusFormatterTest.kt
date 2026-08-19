package com.example.safenest.protection

import org.junit.Assert.assertEquals
import org.junit.Test

class ProtectedHomePolicyStatusFormatterTest {
    @Test
    fun requestedAndReportedStatesDescribeTheActualActivationBoundary() {
        val cases = listOf(
            Triple(false, null, "Original launcher"),
            Triple(true, "denied", "Activation required on child phone"),
            Triple(true, "granted", "Layngo Protected Home active"),
            Triple(false, "granted", "Original launcher requested"),
        )

        cases.forEach { (requested, permissionState, expectedTitle) ->
            assertEquals(
                expectedTitle,
                ProtectedHomePolicyStatusFormatter.format(requested, permissionState).title,
            )
        }
    }
}

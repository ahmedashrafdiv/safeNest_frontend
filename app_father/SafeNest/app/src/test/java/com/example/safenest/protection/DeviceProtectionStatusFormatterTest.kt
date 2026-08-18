package com.example.safenest.protection

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceProtectionStatusFormatterTest {
    @Test
    fun reportedStatusesDescribeTheirActualProtectionBoundary() {
        val cases = listOf(
            Triple("consumer_best_effort", "consumer_unmanaged", false) to "Removal warning active — system uninstall lock unavailable",
            Triple("provisioning_required", "provisioning_required", false) to "Device management setup required for system uninstall prevention",
            Triple("management_lost", "tampered_or_unknown", false) to "Device management lost — protection needs attention",
            Triple("healthy", "device_owner", true) to "System uninstall prevention confirmed",
        )

        cases.forEach { (input, expectedText) ->
            assertEquals(
                expectedText,
                DeviceProtectionStatusFormatter.format(input.first, input.second, input.third).text,
            )
        }
    }

    @Test
    fun absentReportIsNotInferredFromPairingState() {
        val status = DeviceProtectionStatusFormatter.format(null, null, null)

        assertEquals("Protection status not yet reported by this device", status.text)
    }
}

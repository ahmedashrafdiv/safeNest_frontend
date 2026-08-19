package com.example.safenest.protection

/**
 * Parent-facing text for one reported Child device. Missing status is deliberately
 * shown as unreported rather than being inferred from pairing or app activity.
 */
data class DeviceProtectionStatus(
    val text: String,
    val colorHex: String,
)

object DeviceProtectionStatusFormatter {
    private val managedOwnerModes = setOf("device_owner", "profile_owner")

    fun format(
        health: String?,
        mode: String?,
        uninstallProtectionConfirmed: Boolean?,
        permissionStates: Map<String, String> = emptyMap(),
    ): DeviceProtectionStatus {
        val normalizedHealth = health?.trim()?.lowercase()
        val normalizedMode = mode?.trim()?.lowercase()

        if (permissionStates["protected_home"] == "denied") {
            return DeviceProtectionStatus(
                text = "Protected Home needs reactivation — pre-delete launcher protection is inactive",
                colorHex = "#B94040",
            )
        }
        if (permissionStates["accessibility"] == "denied") {
            return DeviceProtectionStatus(
                text = "Accessibility is off — app blocking needs recovery",
                colorHex = "#B94040",
            )
        }
        if (permissionStates["device_admin"] == "denied") {
            return DeviceProtectionStatus(
                text = "Device Admin is inactive — removal barrier needs recovery",
                colorHex = "#B7791F",
            )
        }

        if (uninstallProtectionConfirmed == true && normalizedMode in managedOwnerModes) {
            return DeviceProtectionStatus(
                text = "System uninstall prevention confirmed",
                colorHex = "#2CA39D",
            )
        }

        return when (normalizedHealth) {
            "consumer_best_effort" -> DeviceProtectionStatus(
                text = "Removal warning active — system uninstall lock unavailable",
                colorHex = "#B7791F",
            )
            "provisioning_required" -> DeviceProtectionStatus(
                text = "Device management setup required for system uninstall prevention",
                colorHex = "#B7791F",
            )
            "policy_not_confirmed" -> DeviceProtectionStatus(
                text = "Managed device detected — uninstall policy not confirmed",
                colorHex = "#B7791F",
            )
            "management_lost" -> DeviceProtectionStatus(
                text = "Device management lost — protection needs attention",
                colorHex = "#B94040",
            )
            "stale" -> DeviceProtectionStatus(
                text = "Protection status is stale — waiting for device update",
                colorHex = "#B7791F",
            )
            "healthy" -> DeviceProtectionStatus(
                text = "Protection reported healthy — management mode unconfirmed",
                colorHex = "#2CA39D",
            )
            else -> when (normalizedMode) {
                "device_admin_only" -> DeviceProtectionStatus(
                    text = "Device Admin active — system uninstall prevention not confirmed",
                    colorHex = "#B7791F",
                )
                "device_owner", "profile_owner" -> DeviceProtectionStatus(
                    text = "Managed device — waiting for uninstall policy confirmation",
                    colorHex = "#B7791F",
                )
                else -> DeviceProtectionStatus(
                    text = "Protection status not yet reported by this device",
                    colorHex = "#6B7280",
                )
            }
        }
    }
}

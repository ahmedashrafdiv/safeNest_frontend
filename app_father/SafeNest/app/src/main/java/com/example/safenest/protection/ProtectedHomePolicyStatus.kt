package com.example.safenest.protection

data class ProtectedHomePolicyStatus(
    val title: String,
    val detail: String,
    val colorHex: String,
)

/** Parent policy intent and Child-reported Android role state are deliberately displayed separately. */
object ProtectedHomePolicyStatusFormatter {
    fun format(requested: Boolean, permissionState: String?): ProtectedHomePolicyStatus = when {
        !requested && permissionState == "granted" -> ProtectedHomePolicyStatus(
            title = "Original launcher requested",
            detail = "Layngo is still the active Home app until it is changed on the child phone.",
            colorHex = "#B7791F",
        )
        !requested -> ProtectedHomePolicyStatus(
            title = "Original launcher",
            detail = "Layngo Protected Home is off for this device. Removal warnings remain best effort.",
            colorHex = "#15385F",
        )
        permissionState == "granted" -> ProtectedHomePolicyStatus(
            title = "Layngo Protected Home active",
            detail = "Long-press protection is active inside Layngo’s Home screen.",
            colorHex = "#2CA39D",
        )
        permissionState == "denied" -> ProtectedHomePolicyStatus(
            title = "Activation required on child phone",
            detail = "Open Layngo Kids and choose it as the Android Home app to finish activation.",
            colorHex = "#E8796A",
        )
        else -> ProtectedHomePolicyStatus(
            title = "Waiting for device confirmation",
            detail = "The child phone has not yet reported whether Android granted the Home role.",
            colorHex = "#B7791F",
        )
    }
}

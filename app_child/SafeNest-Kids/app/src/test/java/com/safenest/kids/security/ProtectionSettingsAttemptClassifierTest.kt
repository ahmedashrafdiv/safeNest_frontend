package com.safenest.kids.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtectionSettingsAttemptClassifierTest {
    private val ownPackage = "com.safenest.kids"

    private fun classify(sourcePackage: String, visibleText: String) =
        ProtectionSettingsAttemptClassifier.isProtectionSettingsAttempt(
            sourcePackage = sourcePackage,
            visibleText = visibleText,
            ownPackage = ownPackage,
        )

    @Test
    fun accessibilityEntryNamingLayngoIsClassified() {
        assertTrue(classify("com.android.settings", "Accessibility  Layngo Kids  Off"))
    }

    @Test
    fun arabicAccessibilityEntryNamingLayngoIsClassified() {
        assertTrue(classify("com.android.settings", "إمكانية الوصول  Layngo Kids  إيقاف"))
    }

    @Test
    fun deviceAdminDeactivationNamingLayngoIsClassified() {
        assertTrue(classify("com.android.settings", "Deactivate device admin app  Layngo Kids"))
    }

    @Test
    fun oemSettingsPackagesAreCoveredToo() {
        // The paired handset is a Realme running ColorOS, which routes some of these pages
        // through its own settings package rather than com.android.settings.
        assertTrue(classify("com.coloros.settings", "Accessibility  Layngo Kids"))
        assertTrue(classify("com.oplus.settings", "إمكانية الوصول  Layngo Kids"))
    }

    @Test
    fun accessibilityListWithoutLayngoOnScreenStaysReachable() {
        // The parent must still be able to browse into Accessibility; only Layngo's own row
        // is defended, so an unrelated service's page is not a false positive.
        assertFalse(classify("com.android.settings", "Accessibility  TalkBack  Select to Speak"))
    }

    @Test
    fun ordinarySettingsPagesNamingLayngoAreNotClassified() {
        // Battery/storage/notification pages legitimately name the app and must not trigger.
        assertFalse(classify("com.android.settings", "Battery usage  Layngo Kids  2%"))
        assertFalse(classify("com.android.settings", "Notifications  Layngo Kids"))
    }

    @Test
    fun nonSettingsSurfacesAreIgnored() {
        assertFalse(classify("com.whatsapp", "Accessibility Layngo Kids"))
        assertFalse(classify("com.android.chrome", "device admin Layngo Kids"))
    }
}

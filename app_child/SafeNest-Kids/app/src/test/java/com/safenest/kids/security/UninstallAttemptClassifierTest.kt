package com.safenest.kids.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UninstallAttemptClassifierTest {
    @Test
    fun packageInstallerNamingLayngoIsClassifiedAsTargetedRemovalAttempt() {
        assertTrue(
            UninstallAttemptClassifier.isLayngoRemovalAttempt(
                sourcePackage = "com.android.packageinstaller",
                visibleText = "Do you want to uninstall Layngo Kids?",
                ownPackage = "com.safenest.kids",
            ),
        )
    }

    @Test
    fun homeRolePermissionControllerNamingLayngoWithoutRemovalActionIsNotClassified() {
        // Regression: selecting Layngo as the Android Home app must not open BlockedAppActivity.
        assertFalse(
            UninstallAttemptClassifier.isLayngoRemovalAttempt(
                sourcePackage = "com.google.android.permissioncontroller",
                visibleText = "Allow Layngo Kids to be your Home app?",
                ownPackage = "com.safenest.kids",
            ),
        )
    }

    @Test
    fun generalSettingsForLayngoWithoutRemovalActionIsNotClassified() {
        assertFalse(
            UninstallAttemptClassifier.isLayngoRemovalAttempt(
                sourcePackage = "com.android.settings",
                visibleText = "Layngo Kids",
                ownPackage = "com.safenest.kids",
            ),
        )
    }

    @Test
    fun layngoSettingsDetailsWithUninstallActionIsClassifiedAsRemovalAttempt() {
        assertTrue(
            UninstallAttemptClassifier.isLayngoRemovalAttempt(
                sourcePackage = "com.android.settings",
                visibleText = "Layngo Kids App info Uninstall Force stop",
                ownPackage = "com.safenest.kids",
            ),
        )
    }

    @Test
    fun realmeArabicLayngoRemovalScreenIsClassifiedAsRemovalAttempt() {
        assertTrue(
            UninstallAttemptClassifier.isLayngoRemovalAttempt(
                sourcePackage = "com.android.settings",
                visibleText = "معلومات التطبيق Layngo Kids إلغاء التثبيت فرض الإيقاف",
                ownPackage = "com.safenest.kids",
            ),
        )
    }

    @Test
    fun installerFlowForAnotherAppIsNotClassifiedAsLayngoAttempt() {
        assertFalse(
            UninstallAttemptClassifier.isLayngoRemovalAttempt(
                sourcePackage = "com.android.packageinstaller",
                visibleText = "Do you want to uninstall Another App?",
                ownPackage = "com.safenest.kids",
            ),
        )
    }
}

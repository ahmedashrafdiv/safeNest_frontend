package com.safenest.kids.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UninstallAttemptClassifierTest {
    @Test
    fun packageInstallerNamingLayngoIsClassifiedAsTargetedAttempt() {
        assertTrue(
            UninstallAttemptClassifier.isLayngoUninstallAttempt(
                sourcePackage = "com.android.packageinstaller",
                visibleText = "Do you want to uninstall Layngo Kids?",
                ownPackage = "com.safenest.kids",
            ),
        )
    }

    @Test
    fun generalSettingsForLayngoIsNotClassifiedAsUninstallAttempt() {
        assertFalse(
            UninstallAttemptClassifier.isLayngoUninstallAttempt(
                sourcePackage = "com.android.settings",
                visibleText = "Layngo Kids",
                ownPackage = "com.safenest.kids",
            ),
        )
    }

    @Test
    fun installerFlowForAnotherAppIsNotClassifiedAsLayngoAttempt() {
        assertFalse(
            UninstallAttemptClassifier.isLayngoUninstallAttempt(
                sourcePackage = "com.android.packageinstaller",
                visibleText = "Do you want to uninstall Another App?",
                ownPackage = "com.safenest.kids",
            ),
        )
    }
}

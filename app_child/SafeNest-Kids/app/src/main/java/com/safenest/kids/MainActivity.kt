package com.safenest.kids

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.safenest.kids.network.ApiClient
import com.safenest.kids.util.PrefsHelper
import com.safenest.kids.security.ProtectionPolicyManager
import com.safenest.kids.security.LauncherEntryDecider
import com.safenest.kids.security.SetupCapabilityEvaluator
import com.safenest.kids.security.SetupCapabilityProvider

class MainActivity : AppCompatActivity() {

    private lateinit var prefsHelper: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApiClient.init(this)
        prefsHelper = PrefsHelper(this)
        // Apply uninstall/lock-task policy only when Android confirms managed ownership.
        // Consumer mode remains functional and explicitly reports no guarantee.
        ProtectionPolicyManager.apply(this)

        val baselineReady = SetupCapabilityEvaluator
            .evaluate(SetupCapabilityProvider.read(this, prefsHelper))
            .baselineReady

        if (LauncherEntryDecider.shouldRedirectToProtection(prefsHelper.isPaired() && baselineReady)) {
            startActivity(Intent(this, BlockedAppActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra("blocked_package", packageName)
                putExtra("blocked_reason", "child_app_launch_protection")
            })
            finish()
            return
        }

        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            if (!prefsHelper.isPaired()) {
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, PairingFragment())
                    .commit()
            } else {
                if (baselineReady) {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, HomeFragment())
                        .commit()
                } else {
                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, PermissionsFragment())
                        .commit()
                }
            }
        }
    }
}



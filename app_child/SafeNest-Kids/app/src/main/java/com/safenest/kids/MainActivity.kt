package com.safenest.kids

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.safenest.kids.network.ApiClient
import com.safenest.kids.util.PermissionsHelper
import com.safenest.kids.util.PrefsHelper
import com.safenest.kids.security.ProtectionPolicyManager
import com.safenest.kids.security.LauncherEntryDecider

class MainActivity : AppCompatActivity() {

    private lateinit var prefsHelper: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ApiClient.init(this)
        prefsHelper = PrefsHelper(this)
        // Apply uninstall/lock-task policy only when Android confirms managed ownership.
        // Consumer mode remains functional and explicitly reports no guarantee.
        ProtectionPolicyManager.apply(this)

        if (LauncherEntryDecider.shouldRedirectToProtection(prefsHelper.isPaired())) {
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
                if (PermissionsHelper.hasAllPermissions(this)) {
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



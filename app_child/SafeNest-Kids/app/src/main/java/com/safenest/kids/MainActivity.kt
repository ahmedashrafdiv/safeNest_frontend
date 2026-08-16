package com.safenest.kids

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.safenest.kids.network.ApiClient
import com.safenest.kids.util.PermissionsHelper
import com.safenest.kids.util.PrefsHelper

class MainActivity : AppCompatActivity() {

    private lateinit var prefsHelper: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ApiClient.init(this)
        prefsHelper = PrefsHelper(this)

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

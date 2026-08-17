package com.safenest.kids

import android.content.Intent
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.InstalledApp
import com.safenest.kids.network.InstalledAppsRequest
import com.safenest.kids.service.WebsiteDnsVpnService
import com.safenest.kids.util.InstalledAppsHelper
import com.safenest.kids.util.PermissionsHelper
import com.safenest.kids.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PermissionsFragment : Fragment() {

    private lateinit var tvUsageStatus: TextView
    private lateinit var tvAccessibilityStatus: TextView
    private lateinit var tvBatteryStatus: TextView
    private lateinit var tvWebsiteVpnStatus: TextView
    private lateinit var btnEnableUsage: Button
    private lateinit var btnEnableAccessibility: Button
    private lateinit var btnEnableBattery: Button
    private lateinit var btnEnableWebsiteVpn: Button
    private lateinit var btnContinue: Button
    private lateinit var progressSync: ProgressBar
    private lateinit var prefsHelper: PrefsHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_permissions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsHelper = PrefsHelper(requireContext())

        tvUsageStatus = view.findViewById(R.id.tv_usage_status)
        tvAccessibilityStatus = view.findViewById(R.id.tv_accessibility_status)
        tvBatteryStatus = view.findViewById(R.id.tv_battery_status)
        tvWebsiteVpnStatus = view.findViewById(R.id.tv_website_vpn_status)
        btnEnableUsage = view.findViewById(R.id.btn_enable_usage)
        btnEnableAccessibility = view.findViewById(R.id.btn_enable_accessibility)
        btnEnableBattery = view.findViewById(R.id.btn_enable_battery)
        btnEnableWebsiteVpn = view.findViewById(R.id.btn_enable_website_vpn)
        btnContinue = view.findViewById(R.id.btn_continue)
        progressSync = view.findViewById(R.id.progress_sync)

        btnEnableUsage.setOnClickListener {
            val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            startActivity(intent)
        }

        btnEnableAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        btnEnableWebsiteVpn.setOnClickListener {
            val consentIntent = VpnService.prepare(requireContext())
            if (consentIntent != null) {
                startActivityForResult(consentIntent, VPN_PERMISSION_REQUEST)
            } else {
                startWebsiteVpnIfReady()
            }
        }

        btnEnableBattery.setOnClickListener {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            }
            startActivity(intent)
        }

        btnContinue.setOnClickListener {
            sendInstalledApps()
        }
    }

    override fun onResume() {
        super.onResume()
        checkPermissions()
    }

    private fun checkPermissions() {
        val hasUsage = PermissionsHelper.hasUsageAccess(requireContext())
        val hasAccessibility = PermissionsHelper.hasAccessibilityService(requireContext())
        val hasBattery = PermissionsHelper.hasBatteryOptimizationExemption(requireContext())
        val hasVpn = PermissionsHelper.hasVpnPermission(requireContext())

        if (hasUsage) {
            tvUsageStatus.text = "✓"
            tvUsageStatus.setTextColor(resources.getColor(R.color.green_success, null))
            btnEnableUsage.isEnabled = false
        } else {
            tvUsageStatus.text = "✗"
            tvUsageStatus.setTextColor(resources.getColor(R.color.red_warning, null))
            btnEnableUsage.isEnabled = true
        }

        if (hasAccessibility) {
            tvAccessibilityStatus.text = "✓"
            tvAccessibilityStatus.setTextColor(resources.getColor(R.color.green_success, null))
            btnEnableAccessibility.isEnabled = false
        } else {
            tvAccessibilityStatus.text = "✗"
            tvAccessibilityStatus.setTextColor(resources.getColor(R.color.red_warning, null))
            btnEnableAccessibility.isEnabled = true
        }

        if (hasVpn) {
            tvWebsiteVpnStatus.text = "✓"
            tvWebsiteVpnStatus.setTextColor(resources.getColor(R.color.green_success, null))
            btnEnableWebsiteVpn.isEnabled = false
            if (prefsHelper.getWebsitePolicySnapshotJson() != null) startWebsiteVpnIfReady()
        } else {
            tvWebsiteVpnStatus.text = "✗"
            tvWebsiteVpnStatus.setTextColor(resources.getColor(R.color.red_warning, null))
            btnEnableWebsiteVpn.isEnabled = true
        }

        if (hasBattery) {
            tvBatteryStatus.text = "✓"
            tvBatteryStatus.setTextColor(resources.getColor(R.color.green_success, null))
            btnEnableBattery.isEnabled = false
        } else {
            tvBatteryStatus.text = "✗"
            tvBatteryStatus.setTextColor(resources.getColor(R.color.red_warning, null))
            btnEnableBattery.isEnabled = true
        }

        val allGranted = hasUsage && hasAccessibility && hasBattery
        btnContinue.isEnabled = allGranted
        if (allGranted) {
            btnContinue.setBackgroundColor(resources.getColor(R.color.purple_dark, null))
        } else {
            btnContinue.setBackgroundColor(resources.getColor(R.color.gray_medium, null))
        }
    }

    private fun sendInstalledApps() {
        val childId = prefsHelper.getChildId() ?: return
        
        btnContinue.isEnabled = false
        progressSync.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val rawApps = InstalledAppsHelper.getInstalledApps(requireContext())
                val request = InstalledAppsRequest(rawApps.map { InstalledApp(it.first, it.second) })

                val response = ApiClient.apiService.updateInstalledApps(childId, request)
                if (response.isSuccessful) {
                    Log.d("InstalledApps", "Successfully synced installed apps.")
                } else {
                    Log.e("InstalledApps", "Failed to sync installed apps: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("InstalledApps", "Error syncing apps", e)
            } finally {
                withContext(Dispatchers.Main) {
                    // Best effort: flag it sent and navigate
                    prefsHelper.setLastAppsSent(true)
                    progressSync.visibility = View.GONE
                    navigateToHome()
                }
            }
        }
    }

    private fun startWebsiteVpnIfReady() {
        if (prefsHelper.getWebsitePolicySnapshotJson() == null) {
            prefsHelper.setWebsiteVpnHealth(PrefsHelper.WEBSITE_VPN_UNAVAILABLE)
            return
        }
        if (!WebsiteDnsVpnService.startIfPermissionGranted(requireContext())) {
            prefsHelper.setWebsiteVpnHealth(PrefsHelper.WEBSITE_VPN_DENIED)
        }
    }

    private fun navigateToHome() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, HomeFragment())
            .commit()
    }

    companion object {
        private const val VPN_PERMISSION_REQUEST = 4202
    }
}

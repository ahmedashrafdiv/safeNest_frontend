package com.safenest.kids

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.InstalledApp
import com.safenest.kids.network.InstalledAppsRequest
import com.safenest.kids.service.AppUsageReportWorker
import com.safenest.kids.service.PhoneLocationService
import com.safenest.kids.service.ProtectionHealthWorker
import com.safenest.kids.service.ProtectedHomePolicySyncWorker
import com.safenest.kids.service.RuleSyncWorker
import com.safenest.kids.service.ScreenTimePolicySyncWorker
import com.safenest.kids.service.WebsiteDnsVpnService
import com.safenest.kids.util.InstalledAppsHelper
import com.safenest.kids.util.PermissionsHelper
import com.safenest.kids.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var tvProtectionStatus: TextView
    private lateinit var tvWebsiteStatus: TextView
    private lateinit var tvPhoneLocationStatus: TextView
    private lateinit var btnFixPermissions: Button
    private lateinit var btnRefreshApps: Button
    private lateinit var btnTestUsage: Button      // DEBUG: remove before release
    private lateinit var progressSync: ProgressBar
    private lateinit var prefsHelper: PrefsHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsHelper = PrefsHelper(requireContext())

        tvProtectionStatus = view.findViewById(R.id.tv_protection_status)
        tvWebsiteStatus = view.findViewById(R.id.tv_website_status)
        tvPhoneLocationStatus = view.findViewById(R.id.tv_phone_location_status)
        btnFixPermissions = view.findViewById(R.id.btn_fix_permissions)
        btnRefreshApps = view.findViewById(R.id.btn_refresh_apps)
        progressSync = view.findViewById(R.id.progress_sync)
        btnTestUsage = view.findViewById(R.id.btn_test_usage)  // DEBUG

        btnRefreshApps.setOnClickListener {
            sendInstalledApps()
        }

        // DEBUG: Manual trigger for testing app-usage reporting â€” remove before release
        btnTestUsage.setOnClickListener {
            WorkManager.getInstance(requireContext())
                .enqueue(OneTimeWorkRequestBuilder<AppUsageReportWorker>().build())
            Toast.makeText(requireContext(), "ØªÙ… Ø¥Ø±Ø³Ø§Ù„ Ø·Ù„Ø¨ Ø§Ø®ØªØ¨Ø§Ø± â€” ØªØ­Ù‚Ù‚ Ù…Ù† Logcat", Toast.LENGTH_SHORT).show()
        }

        btnFixPermissions.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PermissionsFragment())
                .commit()
        }

        // Register periodic rule sync (idempotent â€” KEEP policy means this
        // is safe to call every time the fragment loads)
        registerRuleSyncWorker()
        // A Parent policy write must not wait for the periodic interval before this Child can enforce it.
        RuleSyncWorker.enqueueImmediate(requireContext())
        ProtectedHomePolicySyncWorker.enqueuePeriodic(requireContext())
        ProtectedHomePolicySyncWorker.enqueueImmediate(requireContext())
        ProtectionHealthWorker.enqueuePeriodic(requireContext())
        ProtectionHealthWorker.enqueueImmediate(requireContext())
        registerScreenTimePolicySyncWorker()
        registerAppUsageReportWorker()
        // A newly opened Home screen must not wait up to 15 minutes before the Parent sees fresh usage.
        AppUsageReportWorker.enqueueImmediate(requireContext())

        // Auto-send installed apps if this is a fresh pairing or if apps
        // were never successfully sent before
        if (prefsHelper.isJustPaired() || !prefsHelper.getLastAppsSent()) {
            sendInstalledApps()
        }
    }

    override fun onResume() {
        super.onResume()
        updateProtectionStatus()
    }

    private fun updateProtectionStatus() {
        val context = requireContext()
        val allGranted = PermissionsHelper.hasAllPermissions(context)
        if (allGranted) {
            tvProtectionStatus.text = "Ø­Ù…Ø§ÙŠØ© Ø§Ù„ØªØ·Ø¨ÙŠÙ‚Ø§Øª Ù…ÙØ¹Ù‘Ù„Ø© âœ“"
            tvProtectionStatus.setTextColor(resources.getColor(R.color.green_success, null))
        } else {
            tvProtectionStatus.text = "Ø­Ù…Ø§ÙŠØ© Ø§Ù„ØªØ·Ø¨ÙŠÙ‚Ø§Øª Ù…ØªÙˆÙ‚ÙØ© âœ—"
            tvProtectionStatus.setTextColor(resources.getColor(R.color.red_warning, null))
        }

        if (PermissionsHelper.hasVpnPermission(context) && prefsHelper.getWebsitePolicySnapshotJson() != null) {
            if (prefsHelper.getWebsiteVpnHealth() != PrefsHelper.WEBSITE_VPN_ACTIVE) {
                WebsiteDnsVpnService.startIfPermissionGranted(context)
            }
        }
        updatePhoneLocationStatus(context)

        when (prefsHelper.getWebsiteVpnHealth()) {
            PrefsHelper.WEBSITE_VPN_ACTIVE -> {
                tvWebsiteStatus.text = "Ø­Ù…Ø§ÙŠØ© Ø§Ù„Ù…ÙˆØ§Ù‚Ø¹ Ù…ÙØ¹Ù‘Ù„Ø© (DNS) âœ“"
                tvWebsiteStatus.setTextColor(resources.getColor(R.color.green_success, null))
            }
            PrefsHelper.WEBSITE_VPN_DENIED -> {
                tvWebsiteStatus.text = "Ø­Ù…Ø§ÙŠØ© Ø§Ù„Ù…ÙˆØ§Ù‚Ø¹ ØºÙŠØ± Ù…ØªØ§Ø­Ø©: Ø¥Ø°Ù† VPN Ù…Ø·Ù„ÙˆØ¨ âœ—"
                tvWebsiteStatus.setTextColor(resources.getColor(R.color.red_warning, null))
            }
            PrefsHelper.WEBSITE_VPN_FAILED -> {
                tvWebsiteStatus.text = "Ø­Ù…Ø§ÙŠØ© Ø§Ù„Ù…ÙˆØ§Ù‚Ø¹ ØºÙŠØ± Ù…ØªØ§Ø­Ø©: ØªØ¹Ø°Ø± ØªØ´ØºÙŠÙ„ VPN âœ—"
                tvWebsiteStatus.setTextColor(resources.getColor(R.color.red_warning, null))
            }
            else -> {
                if (prefsHelper.getWebsitePolicySnapshotJson() == null) {
                    tvWebsiteStatus.text = "Ø­Ù…Ø§ÙŠØ© Ø§Ù„Ù…ÙˆØ§Ù‚Ø¹ ÙÙŠ Ø§Ù†ØªØ¸Ø§Ø± Ø³ÙŠØ§Ø³Ø© Ø§Ù„ÙˆØ§Ù„Ø¯"
                    tvWebsiteStatus.setTextColor(resources.getColor(R.color.gray_medium, null))
                } else {
                    tvWebsiteStatus.text = "Ø­Ù…Ø§ÙŠØ© Ø§Ù„Ù…ÙˆØ§Ù‚Ø¹ ØºÙŠØ± Ù…ØªØ§Ø­Ø© Ø­Ø§Ù„ÙŠØ§Ù‹ âœ—"
                    tvWebsiteStatus.setTextColor(resources.getColor(R.color.red_warning, null))
                }
            }
        }
        val phoneNeedsAttention = prefsHelper.getPhoneTrackingStatus() != PrefsHelper.PHONE_LOCATION_STATUS_ACTIVE
        btnFixPermissions.visibility = if (!allGranted || phoneNeedsAttention || prefsHelper.getWebsiteVpnHealth() != PrefsHelper.WEBSITE_VPN_ACTIVE) View.VISIBLE else View.GONE
    }

    private fun updatePhoneLocationStatus(context: android.content.Context) {
        if (prefsHelper.isPaired() && PermissionsHelper.hasLocationPermission(context) &&
            prefsHelper.getPhoneTrackingServiceState() != PrefsHelper.PHONE_LOCATION_SERVICE_ACTIVE
        ) {
            PhoneLocationService.startIfPermissionGranted(context)
        }
        when (prefsHelper.getPhoneTrackingStatus()) {
            PrefsHelper.PHONE_LOCATION_STATUS_ACTIVE -> {
                tvPhoneLocationStatus.text = "ØªØªØ¨Ø¹ Ù…ÙˆÙ‚Ø¹ Ø§Ù„Ù‡Ø§ØªÙ Ù…ÙØ¹Ù‘Ù„ âœ“"
                tvPhoneLocationStatus.setTextColor(resources.getColor(R.color.green_success, null))
            }
            PrefsHelper.PHONE_LOCATION_STATUS_PERMISSION_DENIED -> {
                tvPhoneLocationStatus.text = "ØªØªØ¨Ø¹ Ø§Ù„Ù…ÙˆÙ‚Ø¹ ØºÙŠØ± Ù…ØªØ§Ø­: Ø¥Ø°Ù† Ø§Ù„Ù…ÙˆÙ‚Ø¹ Ù…Ø·Ù„ÙˆØ¨ âœ—"
                tvPhoneLocationStatus.setTextColor(resources.getColor(R.color.red_warning, null))
            }
            PrefsHelper.PHONE_LOCATION_STATUS_OFFLINE -> {
                tvPhoneLocationStatus.text = "ØªØªØ¨Ø¹ Ø§Ù„Ù…ÙˆÙ‚Ø¹ ÙŠÙ†ØªØ¸Ø± Ø§Ù„Ø§ØªØµØ§Ù„Ø› Ø¢Ø®Ø± ØªØ­Ø¯ÙŠØ« Ù‚Ø¯ÙŠÙ… âœ—"
                tvPhoneLocationStatus.setTextColor(resources.getColor(R.color.orange_accent, null))
            }
            PrefsHelper.PHONE_LOCATION_STATUS_STALE -> {
                tvPhoneLocationStatus.text = "ØªØªØ¨Ø¹ Ø§Ù„Ù…ÙˆÙ‚Ø¹: Ø¢Ø®Ø± Ù†Ù‚Ø·Ø© Ù‚Ø¯ÙŠÙ…Ø© âœ—"
                tvPhoneLocationStatus.setTextColor(resources.getColor(R.color.orange_accent, null))
            }
            PrefsHelper.PHONE_LOCATION_STATUS_DISABLED -> {
                tvPhoneLocationStatus.text = "ØªØªØ¨Ø¹ Ù…ÙˆÙ‚Ø¹ Ø§Ù„Ù‡Ø§ØªÙ Ù…ØªÙˆÙ‚Ù Ø­Ø³Ø¨ Ø³ÙŠØ§Ø³Ø© Ø§Ù„ÙˆØ§Ù„Ø¯"
                tvPhoneLocationStatus.setTextColor(resources.getColor(R.color.gray_medium, null))
            }
            else -> {
                tvPhoneLocationStatus.text = "ØªØªØ¨Ø¹ Ù…ÙˆÙ‚Ø¹ Ø§Ù„Ù‡Ø§ØªÙ ØºÙŠØ± Ù…ØªØ§Ø­ Ø­Ø§Ù„ÙŠØ§Ù‹ âœ—"
                tvPhoneLocationStatus.setTextColor(resources.getColor(R.color.red_warning, null))
            }
        }
    }

    private fun registerRuleSyncWorker() {
        RuleSyncWorker.enqueuePeriodic(requireContext())
        Log.d("HomeFragment", "RuleSyncWorker periodic registration enqueued.")
    }

    private fun registerAppUsageReportWorker() {
        AppUsageReportWorker.enqueuePeriodic(requireContext())
        Log.d("HomeFragment", "AppUsageReportWorker periodic registration enqueued.")
    }

    private fun sendInstalledApps() {
        val childId = prefsHelper.getChildId() ?: return

        btnRefreshApps.isEnabled = false
        progressSync.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val rawApps = InstalledAppsHelper.getInstalledApps(requireContext())
                val request = InstalledAppsRequest(rawApps.map { InstalledApp(it.first, it.second) })

                val response = ApiClient.apiService.updateInstalledApps(childId, request)
                if (response.isSuccessful) {
                    Log.d("InstalledApps", "Successfully synced installed apps.")
                    prefsHelper.setLastAppsSent(true)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "ØªÙ… Ø§Ù„ØªØ­Ø¯ÙŠØ« Ø¨Ù†Ø¬Ø§Ø­", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Log.e("InstalledApps", "Failed to sync installed apps: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("InstalledApps", "Error syncing apps", e)
            } finally {
                // Always clear the transient just_paired flag after attempting sync
                prefsHelper.setJustPaired(false)
                withContext(Dispatchers.Main) {
                    btnRefreshApps.isEnabled = true
                    progressSync.visibility = View.GONE
                }
            }
        }
    }

    private fun registerScreenTimePolicySyncWorker() {
        val syncRequest = PeriodicWorkRequestBuilder<ScreenTimePolicySyncWorker>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            ScreenTimePolicySyncWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }}

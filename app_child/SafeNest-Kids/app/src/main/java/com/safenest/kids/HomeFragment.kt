package com.safenest.kids

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
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
import com.safenest.kids.util.AppUsageHelper
import com.safenest.kids.util.ChildGreeting
import com.safenest.kids.util.InstalledAppsHelper
import com.safenest.kids.util.PermissionsHelper
import com.safenest.kids.util.PrefsHelper
import com.safenest.kids.util.ScreenTimeBudget
import com.safenest.kids.view.BudgetRingView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var tvGreeting: TextView
    private lateinit var tvRemainingMinutes: TextView
    private lateinit var ringBudget: BudgetRingView
    private lateinit var groupBudget: Group
    private lateinit var groupSuspended: Group
    private lateinit var cardExtraTime: View
    private lateinit var btnMenu: ImageButton
    private lateinit var progressSync: ProgressBar
    private lateinit var prefsHelper: PrefsHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsHelper = PrefsHelper(requireContext())

        tvGreeting = view.findViewById(R.id.tv_greeting)
        tvRemainingMinutes = view.findViewById(R.id.tv_remaining_minutes)
        ringBudget = view.findViewById(R.id.ring_budget)
        groupBudget = view.findViewById(R.id.group_budget)
        groupSuspended = view.findViewById(R.id.group_suspended)
        cardExtraTime = view.findViewById(R.id.card_extra_time)
        btnMenu = view.findViewById(R.id.btn_menu)
        progressSync = view.findViewById(R.id.progress_sync)

        // Show the cached identity immediately so the greeting does not flash a fallback on every
        // launch while the profile request is still in flight.
        renderGreeting(prefsHelper.getChildName())

        if (prefsHelper.isProtectionSuspended()) {
            // Scheduling the syncs here would refetch the policy that suspending just cleared.
            Log.d(TAG, "Protection is suspended; periodic policy sync is not scheduled.")
        } else {
            registerPolicyWorkers()
        }

        // Auto-send installed apps if this is a fresh pairing or if apps
        // were never successfully sent before
        if (prefsHelper.isJustPaired() || !prefsHelper.getLastAppsSent()) {
            sendInstalledApps()
        }
    }

    /**
     * Every registration the previous Home screen performed, unchanged. This screen remains the
     * place the periodic syncs are scheduled, and the immediate enqueues exist so a Parent policy
     * write is not held up by the periodic interval.
     */
    private fun registerPolicyWorkers() {
        RuleSyncWorker.enqueuePeriodic(requireContext())
        Log.d(TAG, "RuleSyncWorker periodic registration enqueued.")
        RuleSyncWorker.enqueueImmediate(requireContext())
        ProtectedHomePolicySyncWorker.enqueuePeriodic(requireContext())
        ProtectedHomePolicySyncWorker.enqueueImmediate(requireContext())
        ProtectionHealthWorker.enqueuePeriodic(requireContext())
        ProtectionHealthWorker.enqueueImmediate(requireContext())
        registerScreenTimePolicySyncWorker()
        AppUsageReportWorker.enqueuePeriodic(requireContext())
        Log.d(TAG, "AppUsageReportWorker periodic registration enqueued.")
        // A newly opened Home screen must not wait up to 15 minutes before the Parent sees fresh usage.
        AppUsageReportWorker.enqueueImmediate(requireContext())
    }

    override fun onResume() {
        super.onResume()
        val suspended = prefsHelper.isProtectionSuspended()
        renderSuspendedState(suspended)
        if (!suspended) {
            restartProtectionServicesIfNeeded()
        }
        loadSessionProfile()
        loadScreenTimeBudget()
    }

    private fun renderSuspendedState(suspended: Boolean) {
        groupSuspended.visibility = if (suspended) View.VISIBLE else View.GONE
        groupBudget.visibility = if (suspended) View.GONE else View.VISIBLE
        // Requesting more of a budget that is not being enforced would be meaningless.
        cardExtraTime.visibility = if (suspended) View.GONE else View.VISIBLE
    }

    /**
     * The status lines the previous Home screen rendered are gone from the design, but the restarts
     * they triggered as a side effect are load-bearing: this is where a DNS VPN or location service
     * killed by the system gets brought back.
     */
    private fun restartProtectionServicesIfNeeded() {
        val context = requireContext()
        if (PermissionsHelper.hasVpnPermission(context) && prefsHelper.getWebsitePolicySnapshotJson() != null) {
            if (prefsHelper.getWebsiteVpnHealth() != PrefsHelper.WEBSITE_VPN_ACTIVE) {
                WebsiteDnsVpnService.startIfPermissionGranted(context)
            }
        }
        if (prefsHelper.isPaired() && PermissionsHelper.hasLocationPermission(context) &&
            prefsHelper.getPhoneTrackingServiceState() != PrefsHelper.PHONE_LOCATION_SERVICE_ACTIVE
        ) {
            PhoneLocationService.startIfPermissionGranted(context)
        }
    }

    private fun renderGreeting(childName: String?) {
        val displayName = ChildGreeting.displayName(childName)
        tvGreeting.text = if (displayName == null) {
            getString(R.string.home_greeting_fallback)
        } else {
            getString(R.string.home_greeting_named, displayName)
        }
    }

    private fun loadSessionProfile() {
        val deviceId = prefsHelper.getDeviceId()
        if (!prefsHelper.isPaired()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.apiService.getSessionProfile(deviceId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    prefsHelper.setChildName(body.childName)
                    // Cached so the verification dialog can name the account it is checking even
                    // when the device is offline at the moment the parent opens it.
                    prefsHelper.setParentEmail(body.parentEmail)
                    withContext(Dispatchers.Main) {
                        if (isAdded) renderGreeting(body.childName)
                    }
                } else {
                    Log.w(TAG, "Session profile request failed: ${response.code()}")
                }
            } catch (e: Exception) {
                // The cached name is already on screen; an offline launch is not an error worth
                // showing the child.
                Log.w(TAG, "Session profile request failed", e)
            }
        }
    }

    private fun loadScreenTimeBudget() {
        val deviceId = prefsHelper.getDeviceId()
        if (!prefsHelper.isPaired() || prefsHelper.isProtectionSuspended()) return

        lifecycleScope.launch(Dispatchers.IO) {
            val localUsedMinutes = try {
                AppUsageHelper.getTodayUsageStats(requireContext()).values.sum()
            } catch (e: Exception) {
                Log.w(TAG, "Local usage unavailable", e)
                0L
            }

            val ring = try {
                val response = ApiClient.apiService.getScreenTimeDecision(deviceId)
                val body = response.body()
                if (response.isSuccessful && body != null) {
                    ScreenTimeBudget.fromDecision(
                        body.decision,
                        body.remainingSeconds,
                        body.effectiveLimitSeconds,
                        localUsedMinutes,
                    )
                } else {
                    // 404 policy_not_found is the ordinary case for a child whose parent has set no
                    // daily limit, so it takes the same fallback as any other unusable answer.
                    ScreenTimeBudget.fromDefaultBudget(localUsedMinutes)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Screen-time decision request failed", e)
                ScreenTimeBudget.fromDefaultBudget(localUsedMinutes)
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                tvRemainingMinutes.text = ring.remainingMinutes.toString()
                ringBudget.sweepFraction = ring.sweepFraction
            }
        }
    }

    private fun sendInstalledApps() {
        val childId = prefsHelper.getChildId() ?: return

        progressSync.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val rawApps = InstalledAppsHelper.getInstalledApps(requireContext())
                val request = InstalledAppsRequest(rawApps.map { InstalledApp(it.first, it.second) })

                val response = ApiClient.apiService.updateInstalledApps(childId, request)
                if (response.isSuccessful) {
                    Log.d("InstalledApps", "Successfully synced installed apps.")
                    prefsHelper.setLastAppsSent(true)
                } else {
                    Log.e("InstalledApps", "Failed to sync installed apps: ${response.code()} ${response.message()}")
                }
            } catch (e: Exception) {
                Log.e("InstalledApps", "Error syncing apps", e)
            } finally {
                // Always clear the transient just_paired flag after attempting sync
                prefsHelper.setJustPaired(false)
                withContext(Dispatchers.Main) {
                    if (isAdded) progressSync.visibility = View.GONE
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
    }

    private companion object {
        const val TAG = "HomeFragment"
    }
}

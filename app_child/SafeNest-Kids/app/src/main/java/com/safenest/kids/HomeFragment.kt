package com.safenest.kids

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.AccessRequestCreateResponse
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
import com.safenest.kids.util.ExtraTimeRequestDecider
import com.safenest.kids.util.InstalledAppsHelper
import com.safenest.kids.util.PermissionsHelper
import com.safenest.kids.util.PrefsHelper
import com.safenest.kids.util.ProtectionLifecycleCoordinator
import com.safenest.kids.util.ScreenTimeBudget
import com.safenest.kids.view.BudgetRingView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import java.util.UUID

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
    private lateinit var btnRequestExtraTime: View
    private var extraTimeRequestId: String? = null
    private var requestingExtraTime = false

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
        btnRequestExtraTime = view.findViewById(R.id.btn_request_extra_time)
        extraTimeRequestId = savedInstanceState?.getString(STATE_EXTRA_TIME_REQUEST_ID)
            ?: prefsHelper.getPendingExtraTimeRequestId()

        parentFragmentManager.setFragmentResultListener(
            ParentControlsSheet.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, result ->
            result.getString(ParentControlsSheet.BUNDLE_ACTION)
                ?.let { runCatching { ParentControlAction.valueOf(it) }.getOrNull() }
                ?.let(::showParentVerification)
        }
        parentFragmentManager.setFragmentResultListener(
            ParentVerificationDialog.REQUEST_KEY,
            viewLifecycleOwner,
        ) { _, result ->
            result.getString(ParentVerificationDialog.BUNDLE_ACTION)
                ?.let { runCatching { ParentControlAction.valueOf(it) }.getOrNull() }
                ?.let(::onParentVerified)
        }
        btnMenu.setOnClickListener {
            ParentControlsSheet().show(parentFragmentManager, ParentControlsSheet.TAG)
        }
        view.findViewById<View>(R.id.btn_enable_protection).setOnClickListener {
            showParentVerification(ParentControlAction.REENABLE_PROTECTION)
        }
        view.findViewById<View>(R.id.btn_delete_app).setOnClickListener {
            showParentVerification(ParentControlAction.DELETE_APPLICATION)
        }
        btnRequestExtraTime.setOnClickListener { submitExtraTimeRequest() }
        view.findViewById<View>(R.id.btn_help).setOnClickListener { showHelp() }
        view.findViewById<View>(R.id.nav_help).setOnClickListener { showHelp() }
        view.findViewById<View>(R.id.nav_today).setOnClickListener { loadScreenTimeBudget() }

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

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(STATE_EXTRA_TIME_REQUEST_ID, extraTimeRequestId)
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

    private fun showParentVerification(action: ParentControlAction) {
        val parentEmail = prefsHelper.getParentEmail()
        if (parentEmail.isNullOrBlank()) {
            loadSessionProfile()
            Toast.makeText(requireContext(), R.string.parent_verification_email_unavailable, Toast.LENGTH_SHORT).show()
            return
        }
        ParentVerificationDialog.newInstance(action, parentEmail)
            .show(parentFragmentManager, ParentVerificationDialog.TAG)
    }

    /** Every action reaching this point has passed the Backend parent-password verification gate. */
    private fun onParentVerified(action: ParentControlAction) {
        when (action) {
            ParentControlAction.SIGN_OUT -> {
                val homeRoleHeld = ProtectionLifecycleCoordinator.signOut(requireContext())
                if (homeRoleHeld) ProtectionLifecycleCoordinator.openHomeSettings(requireContext())
                requireActivity().recreate()
            }
            ParentControlAction.SUSPEND_PROTECTION -> {
                ProtectionLifecycleCoordinator.suspendProtection(requireContext())
                renderSuspendedState(suspended = true)
            }
            ParentControlAction.REENABLE_PROTECTION -> {
                ProtectionLifecycleCoordinator.resumeProtection(requireContext())
                renderSuspendedState(suspended = false)
                loadScreenTimeBudget()
            }
            ParentControlAction.DELETE_APPLICATION -> {
                ProtectionLifecycleCoordinator.requestUninstall(requireContext())
            }
            ParentControlAction.OPEN_LOCATION_SETTINGS -> Unit
            ParentControlAction.OPEN_ACCESSIBILITY_SETTINGS -> Unit
        }
    }

    private fun submitExtraTimeRequest() {
        if (requestingExtraTime || prefsHelper.isProtectionSuspended()) return
        val clientRequestId = extraTimeRequestId ?: UUID.randomUUID().toString().also {
            extraTimeRequestId = it
            prefsHelper.setPendingExtraTimeRequestId(it)
        }
        requestingExtraTime = true
        btnRequestExtraTime.isEnabled = false
        btnRequestExtraTime.contentDescription = getString(R.string.home_extra_time_submitting)

        lifecycleScope.launch(Dispatchers.IO) {
            val outcome = try {
                val response = ApiClient.apiService.createAccessRequest(
                    prefsHelper.getDeviceId(),
                    ExtraTimeRequestDecider.build(clientRequestId),
                )
                val body: AccessRequestCreateResponse? = response.body()
                ExtraTimeRequestDecider.outcome(
                    httpCode = response.code(),
                    duplicate = body?.duplicate == true,
                    requestId = body?.requestId,
                )
            } catch (_: Exception) {
                ExtraTimeRequestDecider.Outcome.FAILED
            }
            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                requestingExtraTime = false
                btnRequestExtraTime.isEnabled = true
                btnRequestExtraTime.contentDescription = getString(R.string.home_extra_time_action)
                val message = when (outcome) {
                    ExtraTimeRequestDecider.Outcome.SUBMITTED -> R.string.home_extra_time_submitted
                    ExtraTimeRequestDecider.Outcome.DUPLICATE -> R.string.home_extra_time_duplicate
                    ExtraTimeRequestDecider.Outcome.FAILED -> R.string.home_extra_time_failed
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
                if (outcome != ExtraTimeRequestDecider.Outcome.FAILED) {
                    extraTimeRequestId = null
                    prefsHelper.setPendingExtraTimeRequestId(null)
                }
            }
        }
    }

    private fun showHelp() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.home_help_title)
            .setMessage(R.string.home_help_message)
            .setPositiveButton(R.string.home_help_close, null)
            .show()
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
        ScreenTimePolicySyncWorker.enqueuePeriodic(requireContext())
        ScreenTimePolicySyncWorker.enqueueImmediate(requireContext())
    }

    private companion object {
        const val TAG = "HomeFragment"
        const val STATE_EXTRA_TIME_REQUEST_ID = "extra_time_request_id"
    }
}

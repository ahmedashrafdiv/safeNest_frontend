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
import com.safenest.kids.service.RuleSyncWorker
import com.safenest.kids.util.InstalledAppsHelper
import com.safenest.kids.util.PermissionsHelper
import com.safenest.kids.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class HomeFragment : Fragment() {

    private lateinit var tvProtectionStatus: TextView
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
        btnFixPermissions = view.findViewById(R.id.btn_fix_permissions)
        btnRefreshApps = view.findViewById(R.id.btn_refresh_apps)
        progressSync = view.findViewById(R.id.progress_sync)
        btnTestUsage = view.findViewById(R.id.btn_test_usage)  // DEBUG

        btnRefreshApps.setOnClickListener {
            sendInstalledApps()
        }

        // DEBUG: Manual trigger for testing app-usage reporting — remove before release
        btnTestUsage.setOnClickListener {
            WorkManager.getInstance(requireContext())
                .enqueue(OneTimeWorkRequestBuilder<AppUsageReportWorker>().build())
            Toast.makeText(requireContext(), "تم إرسال طلب اختبار — تحقق من Logcat", Toast.LENGTH_SHORT).show()
        }

        btnFixPermissions.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, PermissionsFragment())
                .commit()
        }

        // Register periodic rule sync (idempotent — KEEP policy means this
        // is safe to call every time the fragment loads)
        registerRuleSyncWorker()
        registerAppUsageReportWorker()

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
        val allGranted = PermissionsHelper.hasAllPermissions(requireContext())
        if (allGranted) {
            tvProtectionStatus.text = "الحماية مفعّلة ✓"
            tvProtectionStatus.setTextColor(resources.getColor(R.color.green_success, null))
            btnFixPermissions.visibility = View.GONE
        } else {
            tvProtectionStatus.text = "الحماية متوقفة ✗"
            tvProtectionStatus.setTextColor(resources.getColor(R.color.red_warning, null))
            btnFixPermissions.visibility = View.VISIBLE
        }
    }

    private fun registerRuleSyncWorker() {
        val syncRequest = PeriodicWorkRequestBuilder<RuleSyncWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "rule_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
        Log.d("HomeFragment", "RuleSyncWorker periodic registration enqueued.")
    }

    private fun registerAppUsageReportWorker() {
        val usageRequest = PeriodicWorkRequestBuilder<AppUsageReportWorker>(
            1, TimeUnit.HOURS
        ).build()

        WorkManager.getInstance(requireContext()).enqueueUniquePeriodicWork(
            "app_usage_report",
            ExistingPeriodicWorkPolicy.KEEP,
            usageRequest
        )
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
                        Toast.makeText(requireContext(), "تم التحديث بنجاح", Toast.LENGTH_SHORT).show()
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
}

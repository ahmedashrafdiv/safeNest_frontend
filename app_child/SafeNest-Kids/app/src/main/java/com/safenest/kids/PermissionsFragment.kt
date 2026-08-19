package com.safenest.kids

import android.Manifest
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.net.VpnService
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.InstalledApp
import com.safenest.kids.network.InstalledAppsRequest
import com.safenest.kids.security.LayngoDeviceAdminReceiver
import com.safenest.kids.security.ProtectedHomeRoleManager
import com.safenest.kids.security.SetupCapability
import com.safenest.kids.security.SetupCapabilityEvaluator
import com.safenest.kids.security.SetupCapabilityProvider
import com.safenest.kids.security.SetupCapabilityStatus
import com.safenest.kids.security.SetupReadiness
import com.safenest.kids.service.PhoneLocationService
import com.safenest.kids.service.WebsiteDnsVpnService
import com.safenest.kids.util.InstalledAppsHelper
import com.safenest.kids.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Guided, scrollable parent setup journey. All status values come from Android's live capability state. */
class PermissionsFragment : Fragment() {

    private lateinit var prefsHelper: PrefsHelper
    private lateinit var progressSync: ProgressBar
    private lateinit var btnContinue: Button
    private lateinit var tvJourneySummary: TextView
    private lateinit var tvCompletion: TextView
    private lateinit var optionalWebsiteCard: View
    private lateinit var optionalLocationCard: View

    private val statusViews = mutableMapOf<SetupCapability, TextView>()
    private val actionButtons = mutableMapOf<SetupCapability, Button>()

    private val homeRoleLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { refreshCapabilityStates() }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_permissions, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsHelper = PrefsHelper(requireContext())
        progressSync = view.findViewById(R.id.progress_sync)
        btnContinue = view.findViewById(R.id.btn_continue)
        tvJourneySummary = view.findViewById(R.id.tv_journey_summary)
        tvCompletion = view.findViewById(R.id.tv_completion)
        optionalWebsiteCard = view.findViewById(R.id.card_website_vpn)
        optionalLocationCard = view.findViewById(R.id.card_phone_location)

        bindCapability(view, SetupCapability.DEVICE_ADMIN, R.id.tv_device_admin_status, R.id.btn_enable_device_admin) {
            requestDeviceAdminActivation()
        }
        bindCapability(view, SetupCapability.PROTECTED_HOME, R.id.tv_protected_home_status, R.id.btn_enable_protected_home) {
            requestProtectedHomeRole()
        }
        bindCapability(view, SetupCapability.OVERLAY, R.id.tv_overlay_status, R.id.btn_enable_overlay) {
            requestOverlayPermission()
        }
        bindCapability(view, SetupCapability.USAGE_ACCESS, R.id.tv_usage_status, R.id.btn_enable_usage) {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        }
        bindCapability(view, SetupCapability.ACCESSIBILITY, R.id.tv_accessibility_status, R.id.btn_enable_accessibility) {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        bindCapability(view, SetupCapability.BACKGROUND_RELIABILITY, R.id.tv_battery_status, R.id.btn_enable_battery) {
            startActivity(Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                data = Uri.parse("package:${requireContext().packageName}")
            })
        }
        bindCapability(view, SetupCapability.WEBSITE_PROTECTION, R.id.tv_website_vpn_status, R.id.btn_enable_website_vpn) {
            requestWebsiteVpnPermission()
        }
        bindCapability(view, SetupCapability.LOCATION_MONITORING, R.id.tv_phone_location_status, R.id.btn_enable_phone_location) {
            requestPhoneLocationPermission()
        }
        btnContinue.setOnClickListener { sendInstalledAppsAndComplete() }
    }

    override fun onResume() {
        super.onResume()
        refreshCapabilityStates()
    }

    private fun bindCapability(
        root: View,
        capability: SetupCapability,
        statusId: Int,
        actionId: Int,
        onAction: () -> Unit,
    ) {
        statusViews[capability] = root.findViewById(statusId)
        actionButtons[capability] = root.findViewById<Button>(actionId).apply {
            setOnClickListener { onAction() }
        }
    }

    private fun refreshCapabilityStates() {
        if (!isAdded) return
        val snapshot = SetupCapabilityProvider.read(requireContext(), prefsHelper)
        val readiness = SetupCapabilityEvaluator.evaluate(snapshot)
        optionalWebsiteCard.visibility = if (snapshot.websiteProtectionRequired) View.VISIBLE else View.GONE
        optionalLocationCard.visibility = if (snapshot.locationMonitoringRequired) View.VISIBLE else View.GONE
        renderCapabilityGroup(readiness.requiredStates)
        renderCapabilityGroup(readiness.optionalStates)
        renderJourney(readiness)

        if (snapshot.websiteProtectionRequired && snapshot.vpnGranted) startWebsiteVpnIfReady()
        if (snapshot.locationMonitoringRequired && snapshot.locationGranted) startPhoneLocationIfReady()
    }

    private fun renderCapabilityGroup(states: Map<SetupCapability, SetupCapabilityStatus>) {
        states.forEach { (capability, status) ->
            val statusView = statusViews[capability] ?: return@forEach
            val action = actionButtons[capability] ?: return@forEach
            val (text, color) = when (status) {
                SetupCapabilityStatus.READY -> "✓ مفعّل" to R.color.green_success
                SetupCapabilityStatus.REQUIRES_ACTION -> "مطلوب" to R.color.coral_action
                SetupCapabilityStatus.UNAVAILABLE -> "غير متاح على هذا الإصدار" to R.color.gray_medium
                SetupCapabilityStatus.NOT_REQUIRED -> "اختياري حالياً" to R.color.gray_medium
            }
            statusView.text = text
            statusView.setTextColor(ContextCompat.getColor(requireContext(), color))
            action.visibility = if (status == SetupCapabilityStatus.NOT_REQUIRED) View.GONE else View.VISIBLE
            action.isEnabled = status == SetupCapabilityStatus.REQUIRES_ACTION
            action.alpha = if (action.isEnabled) 1f else 0.55f
        }
    }

    private fun renderJourney(readiness: SetupReadiness) {
        val readyCount = readiness.requiredStates.values.count { it == SetupCapabilityStatus.READY }
        tvJourneySummary.text = "تم تفعيل $readyCount من ${readiness.requiredStates.size} خطوات الحماية الأساسية"
        btnContinue.isEnabled = readiness.baselineReady
        btnContinue.alpha = if (readiness.baselineReady) 1f else 0.5f
        tvCompletion.text = if (readiness.baselineReady) {
            "الحماية الأساسية جاهزة. يمكنك الآن إكمال إعداد جهاز الطفل."
        } else {
            "أكمل الخطوات الأساسية أولاً. ستعيد Layngo التحقق تلقائياً عند عودتك من إعدادات Android."
        }
    }

    private fun requestDeviceAdminActivation() {
        val component = ComponentName(requireContext(), LayngoDeviceAdminReceiver::class.java)
        startActivity(Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, component)
            putExtra(
                DevicePolicyManager.EXTRA_ADD_EXPLANATION,
                "Layngo requires Device Administrator activation to add a system removal barrier.",
            )
        })
    }

    private fun requestProtectedHomeRole() {
        val request = ProtectedHomeRoleManager.createRequestIntent(requireContext())
        if (request == null) {
            Toast.makeText(requireContext(), "لا يدعم هذا الإصدار اختيار الشاشة الرئيسية من Layngo", Toast.LENGTH_LONG).show()
        } else {
            homeRoleLauncher.launch(request)
        }
    }

    private fun requestOverlayPermission() {
        startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
            data = Uri.parse("package:${requireContext().packageName}")
        })
    }

    private fun requestWebsiteVpnPermission() {
        VpnService.prepare(requireContext())?.let { startActivityForResult(it, VPN_PERMISSION_REQUEST) }
            ?: startWebsiteVpnIfReady()
    }

    private fun requestPhoneLocationPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            LOCATION_PERMISSION_REQUEST,
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST) {
            if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) {
                prefsHelper.setPhoneTrackingPermissionState(PrefsHelper.PHONE_LOCATION_PERMISSION_GRANTED)
                startPhoneLocationIfReady()
            } else {
                prefsHelper.setPhoneTrackingPermissionState(PrefsHelper.PHONE_LOCATION_PERMISSION_DENIED)
                prefsHelper.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_PERMISSION_DENIED)
            }
            refreshCapabilityStates()
        }
    }

    private fun startWebsiteVpnIfReady() {
        if (!prefsHelper.isPaired()) return
        if (!WebsiteDnsVpnService.startIfPermissionGranted(requireContext())) {
            prefsHelper.setWebsiteVpnHealth(PrefsHelper.WEBSITE_VPN_DENIED)
        }
    }

    private fun startPhoneLocationIfReady() {
        if (!prefsHelper.isPaired()) return
        if (!PhoneLocationService.startIfPermissionGranted(requireContext())) {
            prefsHelper.setPhoneTrackingStatus(PrefsHelper.PHONE_LOCATION_STATUS_UNAVAILABLE)
        }
    }

    private fun sendInstalledAppsAndComplete() {
        val childId = prefsHelper.getChildId() ?: return
        btnContinue.isEnabled = false
        progressSync.visibility = View.VISIBLE
        lifecycleScope.launch(Dispatchers.IO) {
            val response = runCatching {
                val apps = InstalledAppsHelper.getInstalledApps(requireContext())
                ApiClient.apiService.updateInstalledApps(childId, InstalledAppsRequest(apps.map { InstalledApp(it.first, it.second) }))
            }.getOrNull()
            withContext(Dispatchers.Main) {
                prefsHelper.setLastAppsSent(response?.isSuccessful == true)
                progressSync.visibility = View.GONE
                if (ProtectedHomeRoleManager.isActive(requireContext())) {
                    startActivity(Intent(Intent.ACTION_MAIN).apply { addCategory(Intent.CATEGORY_HOME) })
                    requireActivity().finish()
                } else {
                    parentFragmentManager.beginTransaction().replace(R.id.fragment_container, HomeFragment()).commit()
                }
            }
        }
    }

    private companion object {
        const val VPN_PERMISSION_REQUEST = 4202
        const val LOCATION_PERMISSION_REQUEST = 7302
    }
}

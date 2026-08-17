package com.example.safenest.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.R
import com.example.safenest.network.AllowedAppItem
import com.example.safenest.network.InstalledAppItem
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.MonitoringViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class InstalledAppsFragment : Fragment() {

    companion object {
        private const val TAG = "InstalledAppsFragment"
    }

    private val viewModel: MonitoringViewModel by viewModels()

    private var progressBar: ProgressBar? = null
    private var emptyText: TextView? = null
    private var availableAppsCard: MaterialCardView? = null
    private var blockedAppsCard: MaterialCardView? = null
    private var allowedAppsCard: MaterialCardView? = null
    private var availableAppsList: LinearLayout? = null
    private var blockedAppsList: LinearLayout? = null
    private var allowedAppsList: LinearLayout? = null
    private var addBlockedAppBtn: MaterialButton? = null
    private var addAllowedAppBtn: MaterialButton? = null

    private var blockedApps: MutableList<String> = mutableListOf()
    private var allowedApps: MutableList<AllowedAppItem> = mutableListOf()
    private var allowedPackages: MutableList<String> = mutableListOf()
    private var installedApps: List<InstalledAppItem> = emptyList()
    private var appControlMode: String = "blocklist"
    private var modeGroup: android.widget.RadioGroup? = null
    private var modeSummary: TextView? = null

    // Holds the DigitalRule rule ID returned from the server, needed for updates
    private var ruleId: String? = null
    private var policyUpdateInFlight: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_installed_apps, container, false)

        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)
        availableAppsCard = view.findViewById(R.id.availableAppsCard)
        availableAppsList = view.findViewById(R.id.availableAppsList)
        blockedAppsCard = view.findViewById(R.id.blockedAppsCard)
        allowedAppsCard = view.findViewById(R.id.allowedAppsCard)
        blockedAppsList = view.findViewById(R.id.blockedAppsList)
        allowedAppsList = view.findViewById(R.id.allowedAppsList)
        addBlockedAppBtn = view.findViewById(R.id.addBlockedAppBtn)
        addAllowedAppBtn = view.findViewById(R.id.addAllowedAppBtn)
        modeGroup = view.findViewById(R.id.appControlModeGroup)
        modeSummary = view.findViewById(R.id.appControlModeSummary)
        modeGroup?.setOnCheckedChangeListener { _, checkedId ->
            val selectedMode = if (checkedId == R.id.allowlistMode) "allowlist" else "blocklist"
            if (selectedMode != appControlMode) {
                appControlMode = selectedMode
                renderAvailableApps()
                updateModeSummary()
                saveChangesToServer()
            }
        }
        modeGroup?.check(R.id.blocklistMode)
        updateModeSummary()

        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        addBlockedAppBtn?.setOnClickListener { showAddAppDialog(isBlocked = true) }
        addAllowedAppBtn?.setOnClickListener { showAddAppDialog(isBlocked = false) }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.installedAppsState.collect { state ->
                        when (state) {
                            is Result.Loading -> {
                                progressBar?.visibility = View.VISIBLE
                                emptyText?.visibility = View.GONE
                            }
                            is Result.Success -> {
                                installedApps = state.data.apps.sortedBy { it.appName.lowercase() }
                                progressBar?.visibility = View.GONE
                                renderAvailableApps()
                                if (installedApps.isEmpty()) {
                                    emptyText?.text = "لم يتم الإبلاغ عن تطبيقات من جهاز الطفل بعد. افتح تطبيق الطفل واضغط تحديث التطبيقات."
                                    emptyText?.visibility = View.VISIBLE
                                } else {
                                    emptyText?.visibility = View.GONE
                                }
                                viewModel.clearInstalledAppsState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                emptyText?.text = "تعذر تحميل التطبيقات المثبتة. تأكد أن جهاز الطفل متصل ثم حاول مرة أخرى."
                                emptyText?.visibility = View.VISIBLE
                                renderAvailableApps()
                                viewModel.clearInstalledAppsState()
                            }
                            null -> Unit
                        }
                    }
                }

                // Observe DigitalRule fetch state
                launch {
                    viewModel.digitalRuleState.collect { state ->
                        when (state) {
                            is Result.Loading -> {
                                progressBar?.visibility = View.VISIBLE
                                emptyText?.visibility = View.GONE
                            }
                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                // Persist the rule ID so we can send updates later
                                ruleId = state.data.ruleId
                                // Blocked apps come from the DigitalRule API
                                blockedApps.clear()
                                blockedApps.addAll(state.data.blockedApp)
                                allowedPackages.clear()
                                allowedPackages.addAll(state.data.allowedApp)
                                appControlMode = if (state.data.appControlMode == "allowlist") "allowlist" else "blocklist"
                                modeGroup?.setOnCheckedChangeListener(null)
                                modeGroup?.check(if (appControlMode == "allowlist") R.id.allowlistMode else R.id.blocklistMode)
                                modeGroup?.setOnCheckedChangeListener { _, checkedId ->
                                    val selectedMode = if (checkedId == R.id.allowlistMode) "allowlist" else "blocklist"
                                    if (selectedMode != appControlMode) {
                                        appControlMode = selectedMode
                                        renderAvailableApps()
                                        updateModeSummary()
                                        saveChangesToServer()
                                    }
                                }
                                updateModeSummary()
                                // Per-app time limits are independent from the allowlist.
                                val childId = viewModel.getSelectedChildId()
                                if (state.data.appTimeLimits.isNotEmpty()) {
                                    allowedApps.clear()
                                    allowedApps.addAll(state.data.appTimeLimits.map { (pkg, minutes) -> AllowedAppItem(pkg, minutes) })
                                } else if (childId != null) {
                                    allowedApps.clear()
                                    allowedApps.addAll(getAllowedApps(childId))
                                }
                                renderLists()
                                viewModel.clearDigitalRuleState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                Toast.makeText(context, getString(R.string.error_loading_apps), Toast.LENGTH_SHORT).show()
                                // Fall back to cached allowed apps so the list isn't blank
                                val childId = viewModel.getSelectedChildId()
                                if (childId != null) {
                                    allowedApps.clear()
                                    allowedApps.addAll(getAllowedApps(childId))
                                }
                                renderLists()
                                viewModel.clearDigitalRuleState()
                            }
                            null -> Unit
                        }
                    }
                }

                // Observe DigitalRule update state (replaces updateInstalledAppsState)
                launch {
                    viewModel.updateDigitalRuleState.collect { state ->
                        when (state) {
                            is Result.Loading -> {
                                policyUpdateInFlight = true
                                progressBar?.visibility = View.VISIBLE
                                addBlockedAppBtn?.isEnabled = false
                                addAllowedAppBtn?.isEnabled = false
                            }
                            is Result.Success -> {
                                policyUpdateInFlight = false
                                progressBar?.visibility = View.GONE
                                addBlockedAppBtn?.isEnabled = true
                                addAllowedAppBtn?.isEnabled = true
                                Toast.makeText(context, "تم الحفظ بنجاح", Toast.LENGTH_SHORT).show()
                                // Re-fetch to stay in sync with server
                                val childId = viewModel.getSelectedChildId()
                                if (childId != null) viewModel.getDigitalRule(childId)
                                viewModel.clearUpdateDigitalRuleState()
                            }
                            is Result.Error -> {
                                policyUpdateInFlight = false
                                progressBar?.visibility = View.GONE
                                addBlockedAppBtn?.isEnabled = true
                                addAllowedAppBtn?.isEnabled = true
                                Toast.makeText(context, getString(R.string.error_saving_apps, ""), Toast.LENGTH_LONG).show()
                                // Re-fetch to restore consistent state
                                val childId = viewModel.getSelectedChildId()
                                if (childId != null) viewModel.getDigitalRule(childId)
                                viewModel.clearUpdateDigitalRuleState()
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }

        // Load initial data: show cache immediately, then fetch fresh from server
        val childId = viewModel.getSelectedChildId()
        if (childId != null) {
            allowedApps.addAll(getAllowedApps(childId))
            blockedApps.addAll(getBlockedApps(childId))
            renderLists()
            viewModel.getInstalledApps(childId)
            viewModel.getDigitalRule(childId)
        } else {
            emptyText?.text = getString(R.string.error_no_child)
            emptyText?.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        val childId = viewModel.getSelectedChildId() ?: return
        viewModel.getInstalledApps(childId)
        viewModel.getDigitalRule(childId)
    }

    private fun renderAvailableApps() {
        val ctx = requireContext()
        val list = availableAppsList ?: return
        list.removeAllViews()
        availableAppsCard?.visibility = if (installedApps.isEmpty()) View.GONE else View.VISIBLE

        installedApps.forEach { app ->
            val packageName = app.packageName
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 12, 0, 12)
            }

            val info = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            info.addView(TextView(ctx).apply {
                text = app.appName.ifBlank { packageName }
                textSize = 15f
                setTextColor(android.graphics.Color.parseColor("#15385F"))
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })
            info.addView(TextView(ctx).apply {
                text = packageName
                textSize = 11f
                setTextColor(android.graphics.Color.GRAY)
            })
            row.addView(info)

            row.addView(MaterialButton(ctx).apply {
                text = if (appControlMode == "allowlist") {
                    if (allowedPackages.contains(packageName)) "إلغاء السماح" else "السماح"
                } else {
                    if (blockedApps.contains(packageName)) "إلغاء الحظر" else "حظر"
                }
                isAllCaps = false
                setOnClickListener {
                    if (appControlMode == "allowlist") toggleAllowedPackage(app) else toggleBlockedApp(app)
                }
            })
            row.addView(MaterialButton(ctx).apply {
                text = "وقت"
                isAllCaps = false
                setOnClickListener { showTimeLimitDialog(app) }
            })
            list.addView(row)
        }
    }

    private fun toggleAllowedPackage(app: InstalledAppItem) {
        if (allowedPackages.contains(app.packageName)) {
            allowedPackages.remove(app.packageName)
        } else {
            allowedPackages.add(app.packageName)
        }
        saveChangesToServer()
    }

    private fun toggleBlockedApp(app: InstalledAppItem) {
        if (blockedApps.contains(app.packageName)) {
            blockedApps.remove(app.packageName)
        } else {
            blockedApps.add(app.packageName)
        }
        saveChangesToServer()
    }

    private fun showTimeLimitDialog(app: InstalledAppItem) {
        val input = EditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_NUMBER
            hint = "عدد الدقائق"
            setText(allowedApps.firstOrNull { it.name == app.packageName }?.timeLimitMinutes?.toString() ?: "60")
        }
        AlertDialog.Builder(requireContext())
            .setTitle("تحديد وقت: ${app.appName}")
            .setMessage(app.packageName)
            .setView(input)
            .setPositiveButton("حفظ") { _, _ ->
                val minutes = input.text.toString().toIntOrNull()
                if (minutes != null && minutes > 0) {
                    updateAllowedAppTime(app.packageName, minutes)
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun renderLists() {
        renderAvailableApps()
        val ctx = requireContext()
        blockedAppsCard?.visibility = if (appControlMode == "blocklist") View.VISIBLE else View.GONE
        allowedAppsCard?.visibility = View.VISIBLE
        blockedAppsList?.removeAllViews()
        allowedAppsList?.removeAllViews()

        if (blockedApps.isEmpty()) {
            blockedAppsList?.addView(TextView(ctx).apply {
                text = "لا يوجد"; textSize = 14f; setTextColor(android.graphics.Color.GRAY)
                setPadding(0, 16, 0, 16); gravity = android.view.Gravity.CENTER
            })
        } else {
            blockedApps.forEach { appName ->
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    setPadding(0, 16, 0, 16); gravity = android.view.Gravity.CENTER_VERTICAL
                }
                row.addView(TextView(ctx).apply {
                    text = appName; textSize = 14f; setTextColor(android.graphics.Color.BLACK)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(android.widget.ImageView(ctx).apply {
                    setImageResource(android.R.drawable.ic_menu_delete)
                    setColorFilter(android.graphics.Color.RED); setPadding(16, 16, 16, 16)
                    setOnClickListener { deleteBlockedApp(appName) }
                })
                blockedAppsList?.addView(row)
            }
        }

        if (allowedApps.isEmpty()) {
            allowedAppsList?.addView(TextView(ctx).apply {
                text = "لا يوجد"; textSize = 14f; setTextColor(android.graphics.Color.GRAY)
                setPadding(0, 16, 0, 16); gravity = android.view.Gravity.CENTER
            })
        } else {
            allowedApps.forEach { appItem ->
                val row = LinearLayout(ctx).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                    setPadding(0, 16, 0, 16); gravity = android.view.Gravity.CENTER_VERTICAL
                }
                val timeInput = EditText(ctx).apply {
                    inputType = android.text.InputType.TYPE_CLASS_NUMBER; hint = "دقيقة"
                    setText(appItem.timeLimitMinutes.toString()); textSize = 14f
                    layoutParams = LinearLayout.LayoutParams(200, LinearLayout.LayoutParams.WRAP_CONTENT)
                }
                row.addView(TextView(ctx).apply {
                    text = appItem.name; textSize = 14f; setTextColor(android.graphics.Color.BLACK)
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(timeInput)
                row.addView(android.widget.ImageView(ctx).apply {
                    setImageResource(android.R.drawable.ic_menu_save)
                    setColorFilter(android.graphics.Color.parseColor("#4CAF50")); setPadding(16, 16, 16, 16)
                    setOnClickListener { updateAllowedAppTime(appItem.name, timeInput.text.toString().toIntOrNull() ?: 0) }
                })
                row.addView(android.widget.ImageView(ctx).apply {
                    setImageResource(android.R.drawable.ic_menu_delete)
                    setColorFilter(android.graphics.Color.RED); setPadding(16, 16, 16, 16)
                    setOnClickListener { deleteAllowedApp(appItem.name) }
                })
                allowedAppsList?.addView(row)
            }
        }
    }

    private fun showAddAppDialog(isBlocked: Boolean) {
        val input = EditText(requireContext())
        input.hint = getString(R.string.app_package_hint)
        AlertDialog.Builder(requireContext())
            .setTitle(if (isBlocked) "أضف تطبيق محظور" else "أضف تطبيق مسموح")
            .setView(input)
            .setPositiveButton("إضافة") { _, _ ->
                val pkg = input.text.toString().trim()
                if (pkg.isNotEmpty()) addApp(pkg, isBlocked)
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteBlockedApp(appName: String) { blockedApps.remove(appName); saveChangesToServer() }
    private fun deleteAllowedApp(appName: String) { allowedApps.removeAll { it.name == appName }; saveChangesToServer() }
    private fun updateAllowedAppTime(appName: String, minutes: Int) {
        val index = allowedApps.indexOfFirst { it.name == appName }
        if (index != -1) { allowedApps[index].timeLimitMinutes = minutes; saveChangesToServer() }
    }

    private fun addApp(appPackage: String, isBlocked: Boolean) {
        if (isBlocked) {
            if (!blockedApps.contains(appPackage)) blockedApps.add(appPackage)
            allowedPackages.remove(appPackage)
            allowedApps.removeAll { it.name == appPackage }
        } else {
            if (allowedApps.none { it.name == appPackage }) allowedApps.add(AllowedAppItem(appPackage, 60))
            blockedApps.remove(appPackage)
        }
        saveChangesToServer()
    }

    private fun updateModeSummary() {
        modeSummary?.text = if (appControlMode == "allowlist") {
            "التطبيقات التي تختارها من قائمة جهاز الطفل فقط ستعمل. أي تطبيق جديد سيتم قفله تلقائيًا."
        } else {
            "سيتم قفل التطبيقات التي تختارها من قائمة جهاز الطفل فقط. التطبيقات الجديدة ستظل متاحة."
        }
        val blocklistVisible = appControlMode == "blocklist"
        blockedAppsCard?.visibility = if (blocklistVisible) View.VISIBLE else View.GONE
        addBlockedAppBtn?.visibility = if (blocklistVisible) View.VISIBLE else View.GONE
    }

    private fun saveChangesToServer(confirmEmptyAllowlist: Boolean = true) {
        if (policyUpdateInFlight) return
        val childId = viewModel.getSelectedChildId() ?: return
        renderLists()
        // Persist a local fallback and send the authoritative policy to the Backend.
        saveAllowedApps(childId, allowedApps)
        val id = ruleId ?: return
        if (confirmEmptyAllowlist && appControlMode == "allowlist" && allowedPackages.isEmpty()) {
            AlertDialog.Builder(requireContext())
                .setTitle("قائمة السماح فارغة")
                .setMessage("سيتم قفل كل تطبيقات الطرف الثالث، بما فيها التطبيقات التي سيتم تثبيتها لاحقًا. هل تريد المتابعة؟")
                .setNegativeButton(getString(R.string.cancel), null)
                .setPositiveButton("متابعة") { _, _ -> saveChangesToServer(confirmEmptyAllowlist = false) }
                .show()
            return
        }
        policyUpdateInFlight = true
        viewModel.updateDigitalRule(
            id,
            blockedApp = blockedApps,
            allowedApp = allowedPackages,
            appTimeLimits = allowedApps.associate { it.name to it.timeLimitMinutes },
            appControlMode = appControlMode
        )
    }

    // ─── Local SharedPreferences cache (UI-layer only, not business logic) ────

    private fun saveBlockedApps(childId: String, apps: List<String>) {
        requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
            .edit().putStringSet("blocked_apps_$childId", apps.toSet()).apply()
    }

    private fun getBlockedApps(childId: String): List<String> =
        requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
            .getStringSet("blocked_apps_$childId", emptySet())?.toList() ?: emptyList()

    private fun saveAllowedApps(childId: String, apps: List<AllowedAppItem>) {
        val json = com.google.gson.Gson().toJson(apps)
        requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
            .edit().putString("allowed_apps_$childId", json).apply()
    }

    private fun getAllowedApps(childId: String): List<AllowedAppItem> {
        val json = requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
            .getString("allowed_apps_$childId", null) ?: return emptyList()
        val type = object : com.google.gson.reflect.TypeToken<List<AllowedAppItem>>() {}.type
        return try { com.google.gson.Gson().fromJson(json, type) } catch (e: Exception) { emptyList() }
    }
}

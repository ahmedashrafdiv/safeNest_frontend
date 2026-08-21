package com.example.safenest.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.network.EffectiveContentBlurPolicyResponse
import com.example.safenest.network.InstalledAppItem
import com.example.safenest.policy.ContentBlurPolicyMutation
import com.example.safenest.policy.ParentContentBlurScopeCoordinator
import com.example.safenest.policy.ParentPolicyScopeStore
import com.example.safenest.repository.ChildDeviceRepository
import com.example.safenest.repository.DigitalControlRepository
import com.example.safenest.util.DailyLimitConfirmationValidator
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.MonitoringViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MonitoringFragment : Fragment() {

    companion object {
        private const val TAG = "MonitoringFragment"
    }

    private val viewModel: MonitoringViewModel by viewModels()

    // Repository used directly only for operations not yet in MonitoringViewModel
    // (deleteRule, clearDailyUsageLog — fire-and-forget actions that need no UI state flow)
    private val digitalControlRepo = DigitalControlRepository()
    private val childDeviceRepo = ChildDeviceRepository()
    private val contentBlurCoordinator = ParentContentBlurScopeCoordinator(childDeviceRepo)

    private lateinit var bottomNavBar: BottomNavigationView
    private lateinit var usedAppsCard: MaterialCardView
    private var progressBar: ProgressBar? = null
    private var screenTimeTv: TextView? = null
    private var contentBlurSwitch: MaterialSwitch? = null
    private var contentBlurTargetsButton: MaterialButton? = null
    private var contentBlurStatusText: TextView? = null
    private var contentBlurSourceText: TextView? = null

    private var currentRuleId: String? = null
    private var contentBlurPolicy: EffectiveContentBlurPolicyResponse? = null
    private var installedApps: List<InstalledAppItem> = emptyList()
    private var selectedContentBlurTargets: MutableSet<String> = mutableSetOf()
    private var contentBlurDeviceKey: String? = null
    private var applyingContentBlur = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_monitoring, container, false)

        bottomNavBar = view.findViewById(R.id.bottomNavBar)
        usedAppsCard = view.findViewById(R.id.usedAppsCard)
        progressBar = view.findViewById(R.id.progressBar)
        screenTimeTv = view.findViewById(R.id.screenTimeText)
        screenTimeTv?.setOnClickListener { showDailyLimitConfirmation() }
        contentBlurSwitch = view.findViewById(R.id.contentBlurSwitch)
        contentBlurTargetsButton = view.findViewById(R.id.contentBlurTargetsButton)
        contentBlurStatusText = view.findViewById(R.id.contentBlurStatusText)
        contentBlurSourceText = view.findViewById(R.id.contentBlurSourceText)
        contentBlurSwitch?.setOnCheckedChangeListener { _, checked ->
            if (!applyingContentBlur) {
                if (checked && selectedContentBlurTargets.isEmpty()) {
                    contentBlurSwitch?.isChecked = false
                    showTargetPicker()
                } else {
                    saveContentBlurPolicy(checked, selectedContentBlurTargets.toList())
                }
            }
        }
        contentBlurTargetsButton?.setOnClickListener { showTargetPicker() }

        // Populate header from cache (same pattern as HomeFragment)
        val prefs = requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
        val cachedName = prefs.getString("child_name", null)
        val cachedAvatarIndex = prefs.getInt("child_avatar_index", 3)
        val avatarDrawables = listOf(
            R.drawable.ch1, R.drawable.ch2, R.drawable.ch3,
            R.drawable.ch4, R.drawable.ch5, R.drawable.ch6
        )
        if (cachedName != null) {
            view.findViewById<TextView>(R.id.childName)?.text = cachedName
        }
        if (cachedAvatarIndex in avatarDrawables.indices) {
            view.findViewById<com.google.android.material.imageview.ShapeableImageView>(R.id.headerAvatar)
                ?.setImageResource(avatarDrawables[cachedAvatarIndex])
        }

        bottomNavBar.selectedItemId = R.id.nav_monitoring

        usedAppsCard.setOnClickListener {
            (activity as MainActivity).navigateToFragment(InstalledAppsFragment())
        }

        view.findViewById<MaterialButton?>(R.id.deleteRuleBtn)?.setOnClickListener {
            confirmDeleteRule()
        }

        view.findViewById<MaterialButton?>(R.id.clearUsageLogBtn)?.setOnClickListener {
            confirmClearUsageLog()
        }

        view.findViewById<MaterialCardView?>(R.id.websiteProtectionCard)?.setOnClickListener {
            (activity as MainActivity).navigateToFragment(WebsiteProtectionFragment())
        }

        view.findViewById<MaterialCardView?>(R.id.videoHistoryCard)?.setOnClickListener {
            (activity as MainActivity).navigateToFragment(VideoHistoryFragment())
        }

        view.findViewById<MaterialCardView?>(R.id.dailyUsageCard)?.setOnClickListener {
            (activity as MainActivity).navigateToFragment(DailyUsageFragment())
        }

        view.findViewById<MaterialCardView?>(R.id.pairChildCard)?.setOnClickListener {
            (activity as MainActivity).navigateToFragment(GeneratePinFragment())
        }

        bottomNavBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { (activity as MainActivity).navigateToFragment(HomeFragment()); true }
                R.id.nav_monitoring -> true
                R.id.nav_gps -> { (activity as MainActivity).navigateToFragment(GpsFragment()); true }
                R.id.nav_sensors -> { (activity as MainActivity).navigateToFragment(SensorsFragment()); true }
                R.id.nav_more -> { (activity as MainActivity).navigateToFragment(MoreFragment()); true }
                else -> false
            }
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    ParentPolicyScopeStore.state.collect { scope ->
                        renderContentBlurScope(scope)
                        val device = scope.selectedDevice
                        val childId = scope.childId ?: viewModel.getSelectedChildId()
                        val key = if (scope.canWriteDeviceOverride && childId != null && device != null) {
                            "$childId:${device.deviceId}"
                        } else null
                        if (key != contentBlurDeviceKey) {
                            contentBlurDeviceKey = key
                            if (childId != null && device != null && scope.canWriteDeviceOverride) {
                                loadContentBlurPolicy(childId, device.deviceId)
                            } else {
                                contentBlurPolicy = null
                                selectedContentBlurTargets.clear()
                                renderContentBlurPolicy(null)
                            }
                        }
                    }
                }

                launch {
                    viewModel.installedAppsState.collect { result ->
                        if (result is Result.Success) {
                            installedApps = result.data.apps
                            viewModel.clearInstalledAppsState()
                        }
                    }
                }

                // Load digital rule state
                launch {
                    viewModel.digitalRuleState.collect { state ->
                        when (state) {
                            is Result.Loading -> progressBar?.visibility = View.VISIBLE
                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                val rule = state.data
                                currentRuleId = rule.ruleId
                                updateUI(
                                    rule.dailyLimitMinutes,
                                    rule.usedTodayMinutes,
                                    rule.limitConfirmationRequired
                                )
                                viewModel.clearDigitalRuleState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                if (state.message.contains("404") || state.message.contains("not found", ignoreCase = true)) {
                                    // No rule yet — create a default one
                                    val childId = viewModel.getSelectedChildId()
                                    if (childId != null) createDefaultRule(childId)
                                }
                                viewModel.clearDigitalRuleState()
                            }
                            null -> Unit
                        }
                    }
                }

                // Update digital rule state
                launch {
                    viewModel.updateDigitalRuleState.collect { state ->
                        when (state) {
                            is Result.Success -> {
                                updateUI(
                                    state.data.dailyLimitMinutes,
                                    state.data.usedTodayMinutes,
                                    state.data.limitConfirmationRequired
                                )
                                Toast.makeText(context, getString(R.string.screen_time_set_success, state.data.dailyLimitMinutes ?: 0), Toast.LENGTH_SHORT).show()
                                viewModel.clearUpdateDigitalRuleState()
                            }
                            is Result.Error -> {
                                Toast.makeText(context, getString(R.string.error_update_screen_time), Toast.LENGTH_SHORT).show()
                                viewModel.clearUpdateDigitalRuleState()
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }

        val childId = viewModel.getSelectedChildId()
        if (childId != null) {
            viewModel.getDigitalRule(childId)
            viewModel.getInstalledApps(childId)
        }
    }

    private fun renderContentBlurScope(scope: com.example.safenest.policy.ParentPolicyScopeState) {
        val canWrite = scope.canWriteDeviceOverride
        contentBlurSwitch?.isEnabled = canWrite && !applyingContentBlur
        contentBlurTargetsButton?.isEnabled = canWrite && !applyingContentBlur
        contentBlurStatusText?.text = when {
            scope.scope != com.example.safenest.policy.ParentPolicyScope.SELECTED_DEVICE -> "اختر جهازاً محدداً للتحكم"
            scope.selectedDevice == null -> "اختر جهازاً محدداً للتحكم"
            !scope.selectedDevice.isEligible -> "الجهاز غير نشط ولا يمكن تطبيق السياسة"
            else -> "الجهاز المحدد: ${scope.selectedDevice.label}"
        }
    }

    private fun loadContentBlurPolicy(childId: String, deviceId: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            when (val result = childDeviceRepo.getEffectiveContentBlurPolicy(childId, deviceId)) {
                is Result.Success -> kotlinx.coroutines.withContext(Dispatchers.Main) {
                    contentBlurPolicy = result.data
                    selectedContentBlurTargets = result.data.values.targetPackages.toMutableSet()
                    renderContentBlurPolicy(result.data)
                }
                is Result.Error -> kotlinx.coroutines.withContext(Dispatchers.Main) {
                    contentBlurPolicy = null
                    renderContentBlurPolicy(null)
                    Toast.makeText(context, "تعذر تحميل سياسة طمس المحتوى لهذا الجهاز", Toast.LENGTH_LONG).show()
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun renderContentBlurPolicy(policy: EffectiveContentBlurPolicyResponse?) {
        applyingContentBlur = true
        contentBlurSwitch?.isChecked = policy?.values?.enabled == true
        applyingContentBlur = false
        contentBlurTargetsButton?.text = if (selectedContentBlurTargets.isEmpty()) {
            "اختيار التطبيقات المستهدفة"
        } else {
            "التطبيقات المستهدفة (${selectedContentBlurTargets.size})"
        }
        contentBlurSourceText?.text = when {
            policy == null -> "القيمة موروثة من إعداد الطفل أو غير متاحة"
            policy.inherited -> "موروثة من إعداد الطفل"
            else -> "مخصصة لهذا الجهاز — الإصدار ${policy.version}"
        }
    }

    private fun showTargetPicker() {
        if (installedApps.isEmpty()) {
            Toast.makeText(context, "لم تصل قائمة تطبيقات الطفل بعد", Toast.LENGTH_SHORT).show()
            viewModel.getSelectedChildId()?.let(viewModel::getInstalledApps)
            return
        }
        val labels = installedApps.map { it.appName }.toTypedArray()
        val packages = installedApps.map { it.packageName }
        val checked = packages.map { selectedContentBlurTargets.contains(it) }.toBooleanArray()
        AlertDialog.Builder(requireContext())
            .setTitle("اختيار تطبيقات الطمس")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                if (isChecked) selectedContentBlurTargets.add(packages[which])
                else selectedContentBlurTargets.remove(packages[which])
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton("حفظ") { _, _ ->
                val enabled = contentBlurSwitch?.isChecked == true
                if (enabled && selectedContentBlurTargets.isEmpty()) {
                    contentBlurSwitch?.isChecked = false
                    saveContentBlurPolicy(false, emptyList())
                } else if (enabled) {
                    saveContentBlurPolicy(true, selectedContentBlurTargets.toList())
                } else {
                    renderContentBlurPolicy(contentBlurPolicy)
                }
            }
            .show()
    }

    private fun saveContentBlurPolicy(enabled: Boolean, targets: List<String>) {
        val scope = ParentPolicyScopeStore.state.value
        val childId = scope.childId ?: viewModel.getSelectedChildId()
        val device = scope.selectedDevice
        val policy = contentBlurPolicy
        if (!scope.canWriteDeviceOverride || childId == null || device == null || policy == null) {
            applyingContentBlur = true
            contentBlurSwitch?.isChecked = contentBlurPolicy?.values?.enabled == true
            applyingContentBlur = false
            Toast.makeText(context, "اختر جهازاً نشطاً وانتظر تحميل سياسته أولاً", Toast.LENGTH_LONG).show()
            return
        }
        applyingContentBlur = true
        contentBlurSwitch?.isEnabled = false
        contentBlurTargetsButton?.isEnabled = false
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val mutation = contentBlurCoordinator.saveForDevice(
                childId = childId,
                deviceId = device.deviceId,
                enabled = enabled,
                mode = "CONSERVATIVE",
                targetPackages = targets,
                expectedVersion = policy.version,
            )
            kotlinx.coroutines.withContext(Dispatchers.Main) {
                applyingContentBlur = false
                when (mutation) {
                    is ContentBlurPolicyMutation.Applied -> {
                        Toast.makeText(context, "تم حفظ سياسة طمس المحتوى للجهاز المحدد", Toast.LENGTH_SHORT).show()
                        loadContentBlurPolicy(childId, device.deviceId)
                    }
                    is ContentBlurPolicyMutation.Conflict -> {
                        contentBlurPolicy = mutation.latest
                        selectedContentBlurTargets = mutation.latest.values.targetPackages.toMutableSet()
                        renderContentBlurPolicy(mutation.latest)
                        Toast.makeText(context, "تغيرت السياسة؛ تم تحديث الجهاز قبل إعادة التأكيد", Toast.LENGTH_LONG).show()
                    }
                    is ContentBlurPolicyMutation.Failed -> {
                        renderContentBlurPolicy(contentBlurPolicy)
                        Toast.makeText(context, mutation.message, Toast.LENGTH_LONG).show()
                    }
                }
                renderContentBlurScope(scope)
            }
        }
    }

    private fun createDefaultRule(childId: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val result = digitalControlRepo.createDigitalRule(childId, maxScreenTime = 120, blockedApp = emptyList())
            if (result is Result.Success) {
                currentRuleId = result.data.ruleId
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    updateUI(result.data.dailyLimitMinutes, result.data.usedTodayMinutes, result.data.limitConfirmationRequired)
                }
            }
        }
    }

    private fun updateUI(dailyLimitMinutes: Int?, usedTodayMinutes: Int?, limitConfirmationRequired: Boolean) {
        screenTimeTv?.text = if (limitConfirmationRequired)
            "اضغط لتأكيد الحد اليومي"
        else if (dailyLimitMinutes != null)
            getString(R.string.screen_time_limit_format, dailyLimitMinutes)
        else
            getString(R.string.screen_time_not_set)
        screenTimeTv?.contentDescription = if (limitConfirmationRequired) {
            "تأكيد الحد اليومي. اضغط لإدخال عدد الدقائق الذي حدده الأب."
        } else {
            "الحد اليومي. اضغط لتعديله."
        }

        // Update usage-time chip in header
        val usageTimeTv = view?.findViewById<TextView>(R.id.usageTime)
        if (usageTimeTv != null && dailyLimitMinutes != null && !limitConfirmationRequired) {
            val todayMinutes = usedTodayMinutes ?: 0
            val todayH = todayMinutes / 60; val todayM = todayMinutes % 60
            val maxH = dailyLimitMinutes / 60; val maxM = dailyLimitMinutes % 60
            val todayStr = if (todayH == 0) "${todayM}د" else if (todayM == 0) "${todayH}س" else "${todayH}س ${todayM}د"
            val maxStr = if (maxH == 0) "${maxM}د" else if (maxM == 0) "${maxH}س" else "${maxH}س ${maxM}د"
            usageTimeTv.text = "$todayStr من $maxStr"
        } else if (usageTimeTv != null) {
            usageTimeTv.text = "بانتظار تأكيد الحد"
        }
    }

    /**
     * Legacy rules never reuse their mutable remaining-time value as a daily limit.
     * The parent explicitly confirms the intended limit before new usage is accepted.
     */
    private fun showDailyLimitConfirmation() {
        val ruleId = currentRuleId ?: run {
            Toast.makeText(context, getString(R.string.error_no_active_rule), Toast.LENGTH_SHORT).show()
            return
        }
        val input = EditText(requireContext()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            hint = "مثال: 120"
            contentDescription = "الحد اليومي بالدقائق"
            setPadding(48, 16, 48, 16)
        }
        AlertDialog.Builder(requireContext())
            .setTitle("تأكيد الحد اليومي")
            .setMessage("أدخل عدد الدقائق الذي تريد السماح به يومياً. لن نستخدم الرقم القديم لأنه قد يكون وقتاً متبقياً وليس الحد الحقيقي.")
            .setView(input)
            .setNegativeButton(getString(R.string.cancel), null)
            .setPositiveButton("تأكيد") { _, _ ->
                val minutes = DailyLimitConfirmationValidator.minutesOrNull(input.text.toString())
                if (minutes == null) {
                    Toast.makeText(context, "أدخل عدداً صحيحاً من الدقائق", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                viewModel.updateDigitalRule(ruleId = ruleId, maxScreenTime = minutes)
            }
            .show()
    }

    private fun confirmDeleteRule() {
        val ruleId = currentRuleId ?: run {
            Toast.makeText(context, getString(R.string.error_no_active_rule), Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_rule_title))
            .setMessage(getString(R.string.delete_rule_message))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                progressBar?.visibility = View.VISIBLE
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val result = digitalControlRepo.deleteDigitalRule(ruleId)
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        progressBar?.visibility = View.GONE
                        if (result is Result.Success) {
                            currentRuleId = null
                            updateUI(null, null, false)
                            Toast.makeText(context, getString(R.string.rule_deleted_success), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, getString(R.string.error_delete_rule, (result as? Result.Error)?.message ?: ""), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun confirmClearUsageLog() {
        val childId = viewModel.getSelectedChildId() ?: run {
            Toast.makeText(context, getString(R.string.error_no_child), Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.clear_usage_log_title))
            .setMessage(getString(R.string.clear_usage_log_message))
            .setPositiveButton(getString(R.string.clear)) { _, _ ->
                progressBar?.visibility = View.VISIBLE
                viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                    val result = digitalControlRepo.clearDailyUsageLog(childId)
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        progressBar?.visibility = View.GONE
                        if (result is Result.Success) {
                            Toast.makeText(context, getString(R.string.usage_log_cleared_success), Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, getString(R.string.error_clear_usage_log, (result as? Result.Error)?.message ?: ""), Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}

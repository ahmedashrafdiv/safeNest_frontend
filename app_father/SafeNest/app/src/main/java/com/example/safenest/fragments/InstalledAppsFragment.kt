package com.example.safenest.fragments

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.PopupWindow
import android.widget.ProgressBar
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageButton
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.R
import com.example.safenest.network.InstalledAppItem
import com.example.safenest.policy.ParentAppBlockingScopeCoordinator
import com.example.safenest.policy.ParentPolicyScope
import com.example.safenest.policy.ParentPolicyScopeStore
import com.example.safenest.policy.ScopedPolicyMutation
import com.example.safenest.repository.ChildDeviceRepository
import com.example.safenest.util.AppControlRow
import com.example.safenest.util.AppControlState
import com.example.safenest.util.AppControlStatus
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.MonitoringViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import kotlinx.coroutines.launch

/** Layngo app control: policy selection, per-app status, and an inline weekly time editor. */
class InstalledAppsFragment : Fragment() {

    private enum class Tab { ALL, TIMED, BLOCKED }

    private val viewModel: MonitoringViewModel by viewModels()

    private var progressBar: ProgressBar? = null
    private var emptyText: TextView? = null
    private var appCountText: TextView? = null
    private var availableAppsCard: MaterialCardView? = null
    private var availableAppsList: LinearLayout? = null
    private var modeGroup: RadioGroup? = null
    private var modeSummary: TextView? = null
    private var tabAll: MaterialButton? = null
    private var tabTimed: MaterialButton? = null
    private var tabBlocked: MaterialButton? = null
    private var blockedSummarySection: LinearLayout? = null
    private var timedSummarySection: LinearLayout? = null
    private var blockedSummaryChips: ChipGroup? = null
    private var timedSummaryChips: ChipGroup? = null

    private var installedApps: List<InstalledAppItem> = emptyList()
    private var blockedApps: MutableList<String> = mutableListOf()
    private var allowedPackages: MutableList<String> = mutableListOf()
    private var appTimeLimits: MutableMap<String, Map<String, Int>> = mutableMapOf()
    private var appControlMode: String = "blocklist"

    private var activeTab: Tab = Tab.ALL
    private var expandedPackage: String? = null
    private var editorDraft: MutableMap<String, Int> = mutableMapOf()
    private var openDropdown: PopupWindow? = null

    private var ruleId: String? = null
    private var policyUpdateInFlight: Boolean = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val view = inflater.inflate(R.layout.fragment_installed_apps, container, false)

        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)
        appCountText = view.findViewById(R.id.appCountText)
        availableAppsCard = view.findViewById(R.id.availableAppsCard)
        availableAppsList = view.findViewById(R.id.availableAppsList)
        modeGroup = view.findViewById(R.id.appControlModeGroup)
        modeSummary = view.findViewById(R.id.appControlModeSummary)
        tabAll = view.findViewById(R.id.tabAll)
        tabTimed = view.findViewById(R.id.tabTimed)
        tabBlocked = view.findViewById(R.id.tabBlocked)
        blockedSummarySection = view.findViewById(R.id.blockedSummarySection)
        timedSummarySection = view.findViewById(R.id.timedSummarySection)
        blockedSummaryChips = view.findViewById(R.id.blockedSummaryChips)
        timedSummaryChips = view.findViewById(R.id.timedSummaryChips)

        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<MaterialButton>(R.id.saveChangesButton).setOnClickListener { saveChangesToServer() }

        tabAll?.setOnClickListener { activeTab = Tab.ALL; render() }
        tabTimed?.setOnClickListener { activeTab = Tab.TIMED; render() }
        tabBlocked?.setOnClickListener { activeTab = Tab.BLOCKED; render() }

        attachModeListener()
        modeGroup?.check(R.id.blocklistMode)

        val childName = requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
            .getString("child_name", null)?.trim().orEmpty().ifBlank { "طفلك" }
        view.findViewById<TextView>(R.id.screenTitle).text = "تطبيقات $childName"

        return view
    }

    private fun attachModeListener() {
        modeGroup?.setOnCheckedChangeListener { _, checkedId ->
            val selectedMode = if (checkedId == R.id.allowlistMode) "allowlist" else "blocklist"
            if (selectedMode != appControlMode) {
                appControlMode = selectedMode
                render()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.installedAppsState.collect { state ->
                        when (state) {
                            is Result.Loading -> showLoading()
                            is Result.Success -> {
                                installedApps = state.data.apps.sortedBy { it.appName.lowercase() }
                                progressBar?.visibility = View.GONE
                                render()
                                viewModel.clearInstalledAppsState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                emptyText?.text = "تعذر تحميل التطبيقات المثبتة. تأكد أن جهاز الطفل متصل ثم حاول مرة أخرى."
                                render()
                                viewModel.clearInstalledAppsState()
                            }
                            null -> Unit
                        }
                    }
                }

                launch {
                    viewModel.digitalRuleState.collect { state ->
                        when (state) {
                            is Result.Loading -> showLoading()
                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                ruleId = state.data.ruleId
                                blockedApps = state.data.blockedApp.toMutableList()
                                allowedPackages = state.data.allowedApp.toMutableList()
                                appTimeLimits = state.data.appTimeLimits.toMutableMap()
                                appControlMode = if (state.data.appControlMode == "allowlist") "allowlist" else "blocklist"
                                modeGroup?.setOnCheckedChangeListener(null)
                                modeGroup?.check(if (appControlMode == "allowlist") R.id.allowlistMode else R.id.blocklistMode)
                                attachModeListener()
                                render()
                                viewModel.clearDigitalRuleState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                Toast.makeText(context, getString(R.string.error_loading_apps), Toast.LENGTH_SHORT).show()
                                render()
                                viewModel.clearDigitalRuleState()
                            }
                            null -> Unit
                        }
                    }
                }

                launch {
                    viewModel.updateDigitalRuleState.collect { state ->
                        when (state) {
                            is Result.Loading -> {
                                policyUpdateInFlight = true
                                progressBar?.visibility = View.VISIBLE
                            }
                            is Result.Success -> {
                                policyUpdateInFlight = false
                                progressBar?.visibility = View.GONE
                                Toast.makeText(context, "تم الحفظ بنجاح", Toast.LENGTH_SHORT).show()
                                viewModel.getSelectedChildId()?.let(viewModel::getDigitalRule)
                                viewModel.clearUpdateDigitalRuleState()
                            }
                            is Result.Error -> {
                                policyUpdateInFlight = false
                                progressBar?.visibility = View.GONE
                                Toast.makeText(context, getString(R.string.error_saving_apps, ""), Toast.LENGTH_LONG).show()
                                viewModel.getSelectedChildId()?.let(viewModel::getDigitalRule)
                                viewModel.clearUpdateDigitalRuleState()
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }

        val childId = viewModel.getSelectedChildId()
        if (childId != null) {
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

    override fun onDestroyView() {
        openDropdown?.dismiss()
        openDropdown = null
        super.onDestroyView()
    }

    private fun showLoading() {
        progressBar?.visibility = View.VISIBLE
        emptyText?.visibility = View.GONE
    }

    // ── Rendering ──────────────────────────────────────────────

    private fun rows(): List<AppControlRow> = AppControlStatus.rowsFor(
        installedApps = installedApps.map { it.packageName to it.appName },
        blockedApps = blockedApps,
        allowedApps = allowedPackages,
        appTimeLimits = appTimeLimits,
        appControlMode = appControlMode
    )

    private fun render() {
        val allRows = rows()
        appCountText?.text = "${allRows.size} تطبيقًا على جهازها"
        updateModeSummary()

        val timedCount = allRows.count { it.state == AppControlState.TIMED }
        val blockedCount = allRows.count { it.state == AppControlState.BLOCKED }
        tabAll?.text = "كل التطبيقات ${allRows.size}"
        tabTimed?.text = "بوقت $timedCount"
        tabBlocked?.text = "محظور $blockedCount"
        styleTab(tabAll, activeTab == Tab.ALL)
        styleTab(tabTimed, activeTab == Tab.TIMED)
        styleTab(tabBlocked, activeTab == Tab.BLOCKED)

        val visibleRows = when (activeTab) {
            Tab.ALL -> allRows
            Tab.TIMED -> allRows.filter { it.state == AppControlState.TIMED }
            Tab.BLOCKED -> allRows.filter { it.state == AppControlState.BLOCKED }
        }

        emptyText?.visibility = if (allRows.isEmpty()) View.VISIBLE else View.GONE
        availableAppsCard?.visibility = if (visibleRows.isEmpty()) View.GONE else View.VISIBLE

        val container = availableAppsList ?: return
        container.removeAllViews()
        visibleRows.forEachIndexed { index, row ->
            container.addView(appRow(row))
            if (row.packageName == expandedPackage) container.addView(weeklyEditor(row))
            if (index < visibleRows.lastIndex) container.addView(divider())
        }

        renderSummaries(allRows)
    }

    private fun styleTab(button: MaterialButton?, selected: Boolean) {
        val ctx = context ?: return
        button?.setTextColor(ContextCompat.getColor(ctx, if (selected) R.color.white else R.color.navy_brand))
        button?.backgroundTintList = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(ctx, if (selected) R.color.teal_brand else R.color.mint_surface)
        )
    }

    private fun renderSummaries(allRows: List<AppControlRow>) {
        val blocked = allRows.filter { it.state == AppControlState.BLOCKED }
        val timed = allRows.filter { it.state == AppControlState.TIMED }

        blockedSummarySection?.visibility = if (blocked.isEmpty()) View.GONE else View.VISIBLE
        timedSummarySection?.visibility = if (timed.isEmpty()) View.GONE else View.VISIBLE

        blockedSummaryChips?.removeAllViews()
        blocked.forEach { blockedSummaryChips?.addView(summaryChip(it.displayName, R.color.daily_usage_coral)) }
        timedSummaryChips?.removeAllViews()
        timed.forEach {
            summaryChip("${it.displayName} · ${AppControlStatus.formatDurationArabic(it.todayLimitMinutes ?: 0)}", R.color.teal_brand)
                .also { chip -> timedSummaryChips?.addView(chip) }
        }
    }

    private fun summaryChip(label: String, colorRes: Int): Chip = Chip(requireContext()).apply {
        text = label
        isClickable = false
        isCheckable = false
        setTextColor(ContextCompat.getColor(requireContext(), colorRes))
        chipBackgroundColor = android.content.res.ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.mint_surface)
        )
    }

    private fun appRow(row: AppControlRow): View {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(0, dp(10), 0, dp(10))

            addView(TextView(ctx).apply {
                text = row.iconLabel
                gravity = Gravity.CENTER
                textSize = 13f
                setTextColor(ContextCompat.getColor(ctx, R.color.navy_brand))
                background = ContextCompat.getDrawable(ctx, R.drawable.bg_daily_app_icon)
                layoutParams = LinearLayout.LayoutParams(dp(36), dp(36))
            })

            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    .apply { marginStart = dp(12); marginEnd = dp(12) }
                addView(TextView(ctx).apply {
                    text = row.displayName
                    textSize = 15f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(ctx, R.color.navy_brand))
                })
                addView(TextView(ctx).apply {
                    text = AppControlStatus.statusText(row)
                    textSize = 12f
                    setTextColor(
                        ContextCompat.getColor(
                            ctx,
                            when (row.state) {
                                AppControlState.BLOCKED -> R.color.daily_usage_coral
                                AppControlState.TIMED -> R.color.teal_brand
                                AppControlState.ALLOWED -> R.color.gray_medium
                            }
                        )
                    )
                })
            })

            addView(ImageButton(ctx).apply {
                setImageResource(R.drawable.ic_app_more_vertical)
                background = ContextCompat.getDrawable(ctx, android.R.drawable.btn_default)
                contentDescription = "خيارات ${row.displayName}"
                layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
                setOnClickListener { showActionMenu(this, row) }
            })
        }
    }

    // ── State 02: three-dot action menu ────────────────────────

    private fun showActionMenu(anchor: View, row: AppControlRow) {
        val ctx = requireContext()
        val content = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = ContextCompat.getDrawable(ctx, R.drawable.bg_app_action_popup)
            setPadding(dp(16), dp(12), dp(16), dp(12))
        }

        content.addView(TextView(ctx).apply {
            text = row.displayName
            textSize = 16f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setTextColor(ContextCompat.getColor(ctx, R.color.navy_brand))
            setPadding(0, 0, 0, dp(8))
        })

        val popup = PopupWindow(content, dp(280), LinearLayout.LayoutParams.WRAP_CONTENT, true).apply {
            elevation = 12f
        }

        fun action(title: String, subtitle: String, colorRes: Int, iconRes: Int, onClick: () -> Unit) {
            content.addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                minimumHeight = dp(48)
                setPadding(0, dp(10), 0, dp(10))
                isClickable = true
                addView(ImageView(ctx).apply {
                    setImageResource(iconRes)
                    contentDescription = null
                    layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(12) }
                })
                addView(LinearLayout(ctx).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    addView(TextView(ctx).apply {
                        text = title
                        textSize = 15f
                        typeface = android.graphics.Typeface.DEFAULT_BOLD
                        setTextColor(ContextCompat.getColor(ctx, colorRes))
                    })
                    addView(TextView(ctx).apply {
                        text = subtitle
                        textSize = 12f
                        setTextColor(ContextCompat.getColor(ctx, R.color.gray_medium))
                    })
                })
                setOnClickListener { popup.dismiss(); onClick() }
            })
        }

        action("سماح", "يمكن لطفلك استخدام التطبيق ضمن وقت الشاشة.", R.color.teal_brand, R.drawable.ic_app_action_allow) { allowApp(row.packageName) }
        action("تحديد وقت", "اختر حدًا يوميًا لهذا التطبيق.", R.color.navy_brand, R.drawable.ic_app_action_time) { expandWeeklyEditor(row.packageName) }
        action("حظر", "لن يتمكن جهاز طفلك من فتح التطبيق.", R.color.daily_usage_coral, R.drawable.ic_app_action_block) { blockApp(row.packageName) }

        popup.showAsDropDown(anchor, 0, 0)
    }

    private fun allowApp(packageName: String) {
        blockedApps.remove(packageName)
        appTimeLimits.remove(packageName)
        if (appControlMode == "allowlist" && !allowedPackages.contains(packageName)) {
            allowedPackages.add(packageName)
        }
        if (expandedPackage == packageName) collapseEditor()
        render()
        saveChangesToServer()
    }

    private fun blockApp(packageName: String) {
        appTimeLimits.remove(packageName)
        allowedPackages.remove(packageName)
        if (!blockedApps.contains(packageName)) blockedApps.add(packageName)
        if (expandedPackage == packageName) collapseEditor()
        render()
        saveChangesToServer()
    }

    // ── State 03: inline weekly editor ─────────────────────────

    private fun expandWeeklyEditor(packageName: String) {
        expandedPackage = packageName
        editorDraft = AppControlStatus.fullWeek(appTimeLimits[packageName]).toMutableMap()
        render()
    }

    private fun collapseEditor() {
        openDropdown?.dismiss()
        openDropdown = null
        expandedPackage = null
        editorDraft = mutableMapOf()
    }

    private fun weeklyEditor(row: AppControlRow): View {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.mint_surface))
            setPadding(dp(14), dp(14), dp(14), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = dp(4); bottomMargin = dp(8) }

            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(ImageView(ctx).apply {
                    setImageResource(R.drawable.ic_app_weekly_time)
                    contentDescription = null
                    layoutParams = LinearLayout.LayoutParams(dp(24), dp(24)).apply { marginEnd = dp(8) }
                })
                addView(TextView(ctx).apply {
                    text = "وقت استخدام ${row.displayName}"
                    textSize = 15f
                    typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(ctx, R.color.navy_brand))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                addView(TextView(ctx).apply {
                    text = "طي الإعدادات"
                    textSize = 12f
                    minHeight = dp(48)
                    gravity = Gravity.CENTER_VERTICAL
                    setTextColor(ContextCompat.getColor(ctx, R.color.teal_brand))
                    setOnClickListener { collapseEditor(); render() }
                })
            })

            addView(TextView(ctx).apply {
                text = "حدد الحد اليومي لكل يوم. يمكن أن يختلف الوقت من يوم لآخر."
                textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, R.color.gray_medium))
                setPadding(0, dp(2), 0, dp(10))
            })

            addView(TextView(ctx).apply {
                text = "نسخ وقت السبت إلى باقي الأيام"
                textSize = 12f
                minHeight = dp(48)
                gravity = Gravity.CENTER_VERTICAL
                setTextColor(ContextCompat.getColor(ctx, R.color.teal_brand))
                setOnClickListener {
                    editorDraft = AppControlStatus.copySaturdayToOtherDays(editorDraft).toMutableMap()
                    render()
                }
            })

            AppControlStatus.WEEKDAY_CODES.forEach { dayCode -> addView(dayRow(dayCode)) }

            addView(TextView(ctx).apply {
                text = "00:00 يعني غير متاح اليوم، و24:00 يعني متاح طوال اليوم."
                textSize = 11f
                setTextColor(ContextCompat.getColor(ctx, R.color.gray_medium))
                setPadding(0, dp(8), 0, dp(10))
            })

            addView(MaterialButton(ctx).apply {
                text = "حفظ وقت ${row.displayName}"
                textSize = 14f
                isAllCaps = false
                cornerRadius = dp(14)
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    ContextCompat.getColor(ctx, R.color.teal_brand)
                )
                layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(48))
                setOnClickListener { saveWeeklyLimit(row.packageName) }
            })

            addView(TextView(ctx).apply {
                text = "تصل القاعدة إلى جهاز طفلك عند اتصاله بالإنترنت."
                textSize = 11f
                gravity = Gravity.CENTER
                setTextColor(ContextCompat.getColor(ctx, R.color.gray_medium))
                setPadding(0, dp(6), 0, 0)
            })
        }
    }

    private fun dayRow(dayCode: String): View {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(6), 0, dp(6))

            addView(TextView(ctx).apply {
                text = AppControlStatus.WEEKDAY_ARABIC_NAMES[dayCode]
                textSize = 14f
                setTextColor(ContextCompat.getColor(ctx, R.color.navy_brand))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            addView(TextView(ctx).apply {
                text = AppControlStatus.formatHhMm(editorDraft[dayCode] ?: 0)
                textSize = 15f
                gravity = Gravity.CENTER
                minWidth = dp(96)
                minHeight = dp(48)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                setTextColor(ContextCompat.getColor(ctx, R.color.navy_brand))
                setBackgroundColor(ContextCompat.getColor(ctx, R.color.white))
                contentDescription = "وقت ${AppControlStatus.WEEKDAY_ARABIC_NAMES[dayCode]}"
                setOnClickListener { showTimeDropdown(this, dayCode) }
            })

            addView(TextView(ctx).apply {
                text = "ساعات في اليوم"
                textSize = 11f
                setTextColor(ContextCompat.getColor(ctx, R.color.gray_medium))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(8) }
            })
        }
    }

    /** In-page anchored dropdown of all 97 quarter-hour values — never a bottom sheet or new screen. */
    private fun showTimeDropdown(anchor: View, dayCode: String) {
        val ctx = requireContext()
        val options = AppControlStatus.TIME_OPTIONS_MINUTES
        val labels = options.map { AppControlStatus.formatHhMm(it) }

        val listView = ListView(ctx).apply {
            adapter = android.widget.ArrayAdapter(ctx, android.R.layout.simple_list_item_1, labels)
            setBackgroundColor(ContextCompat.getColor(ctx, R.color.white))
            setSelection(options.indexOf(editorDraft[dayCode] ?: 0).coerceAtLeast(0))
        }

        val popup = PopupWindow(listView, dp(140), dp(260), true).apply { elevation = 12f }
        listView.setOnItemClickListener { _, _, position, _ ->
            editorDraft[dayCode] = options[position]
            popup.dismiss()
            render()
        }
        openDropdown?.dismiss()
        openDropdown = popup
        popup.showAsDropDown(anchor, 0, 0)
    }

    private fun saveWeeklyLimit(packageName: String) {
        appTimeLimits[packageName] = editorDraft.toMap()
        blockedApps.remove(packageName)
        if (appControlMode == "allowlist" && !allowedPackages.contains(packageName)) {
            allowedPackages.add(packageName)
        }
        collapseEditor()
        render()
        saveChangesToServer()
    }

    private fun divider(): View = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        setBackgroundColor(Color.parseColor("#E5F4F1"))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun updateModeSummary() {
        val base = if (appControlMode == "allowlist") {
            "التطبيقات التي تختارها من قائمة جهاز الطفل فقط ستعمل. أي تطبيق جديد سيتم قفله تلقائيًا."
        } else {
            "سيتم قفل التطبيقات التي تختارها من قائمة جهاز الطفل فقط. التطبيقات الجديدة ستظل متاحة."
        }
        val scopeState = ParentPolicyScopeStore.state.value
        val provenance = if (scopeState.scope == ParentPolicyScope.SELECTED_DEVICE) {
            "تعديل خاص بهذا الجهاز: ${scopeState.selectedDevice?.label ?: "الجهاز المحدد"}"
        } else {
            "موروثة من إعدادات الطفل العامة"
        }
        modeSummary?.text = "$base${System.lineSeparator()}$provenance"
    }

    // ── Persistence (unchanged device-scope override behavior) ──

    private fun saveChangesToServer(confirmEmptyAllowlist: Boolean = true) {
        if (policyUpdateInFlight) return
        viewModel.getSelectedChildId() ?: return
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
        if (ParentPolicyScopeStore.state.value.scope == ParentPolicyScope.SELECTED_DEVICE) {
            viewLifecycleOwner.lifecycleScope.launch {
                val scope = ParentPolicyScopeStore.state.value
                val child = scope.childId
                val device = scope.selectedDevice
                if (child.isNullOrBlank() || device == null || !scope.canWriteDeviceOverride) {
                    Toast.makeText(context, scope.blockedReason ?: "اختر جهازًا نشطًا قبل حفظ قواعد التطبيقات.", Toast.LENGTH_LONG).show()
                } else {
                    when (val effective = ChildDeviceRepository().getEffectiveAppBlockingPolicy(child, device.deviceId)) {
                        is Result.Success -> {
                            val patch = mapOf<String, Any?>(
                                "blocked_app" to blockedApps.toList(),
                                "allowed_app" to allowedPackages.toList(),
                                "app_time_limits" to appTimeLimits.toMap(),
                                "app_control_mode" to appControlMode,
                            )
                            when (val mutation = ParentAppBlockingScopeCoordinator(ChildDeviceRepository()).saveSelectedDeviceOverride(patch, effective.data.version)) {
                                is ScopedPolicyMutation.Applied -> Toast.makeText(context, "تم حفظ القواعد لهذا الجهاز", Toast.LENGTH_SHORT).show()
                                is ScopedPolicyMutation.Blocked -> Toast.makeText(context, mutation.message, Toast.LENGTH_LONG).show()
                                is ScopedPolicyMutation.Failed -> Toast.makeText(context, mutation.message, Toast.LENGTH_LONG).show()
                                is ScopedPolicyMutation.Conflict -> {
                                    policyUpdateInFlight = false
                                    val source = if (mutation.latest.inherited) "موروثة من إعدادات الطفل العامة" else "تعديل خاص بهذا الجهاز"
                                    AlertDialog.Builder(requireContext())
                                        .setTitle("تعارض في إصدار القاعدة")
                                        .setMessage("تغيّرت قاعدة هذا الجهاز في مكان آخر. المصدر الحالي: $source. الإصدار الحالي: ${mutation.latest.version}. راجع القاعدة أو أعد تطبيق تغييراتك صراحةً.")
                                        .setNegativeButton("مراجعة القاعدة", null)
                                        .setPositiveButton("تطبيق التغييرات") { _, _ -> saveChangesToServer(confirmEmptyAllowlist = false) }
                                        .show()
                                }
                            }
                        }
                        is Result.Error -> Toast.makeText(context, effective.message ?: "Unable to load the current device policy. No changes were saved.", Toast.LENGTH_LONG).show()
                        Result.Loading -> Unit
                    }
                }
                policyUpdateInFlight = false
            }
        } else {
            viewModel.updateDigitalRule(
                id,
                blockedApp = blockedApps,
                allowedApp = allowedPackages,
                appTimeLimits = appTimeLimits.toMap(),
                appControlMode = appControlMode
            )
        }
    }
}

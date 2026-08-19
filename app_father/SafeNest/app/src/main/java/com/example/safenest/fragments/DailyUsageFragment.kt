package com.example.safenest.fragments

import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.R
import com.example.safenest.util.DailyUsageApp
import com.example.safenest.util.DailyUsageState
import com.example.safenest.util.DailyUsageSummary
import com.example.safenest.util.DailyUsageSummaryMapper
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.MonitoringViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.progressindicator.CircularProgressIndicator
import kotlinx.coroutines.launch

/** Layngo daily usage: Arabic RTL, calm visual hierarchy, verified daily data only. */
class DailyUsageFragment : Fragment() {
    private val viewModel: MonitoringViewModel by viewModels()

    private var loader: ProgressBar? = null
    private var stateText: TextView? = null
    private var summaryCard: MaterialCardView? = null
    private var appsCard: MaterialCardView? = null
    private var usageRing: CircularProgressIndicator? = null
    private var totalText: TextView? = null
    private var limitText: TextView? = null
    private var remainingText: TextView? = null
    private var updatedText: TextView? = null
    private var appsList: LinearLayout? = null
    private var showMore: MaterialButton? = null
    private var apps: List<DailyUsageApp> = emptyList()
    private var expanded = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        return inflater.inflate(R.layout.fragment_daily_usage, container, false).also { view ->
            loader = view.findViewById(R.id.progressBar)
            stateText = view.findViewById(R.id.stateText)
            summaryCard = view.findViewById(R.id.summaryCard)
            appsCard = view.findViewById(R.id.appsListCard)
            usageRing = view.findViewById(R.id.usageRing)
            totalText = view.findViewById(R.id.totalUsageText)
            limitText = view.findViewById(R.id.limitText)
            remainingText = view.findViewById(R.id.remainingText)
            updatedText = view.findViewById(R.id.lastUpdatedText)
            appsList = view.findViewById(R.id.usageAppsList)
            showMore = view.findViewById(R.id.showMoreButton)

            view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener { parentFragmentManager.popBackStack() }
            view.findViewById<MaterialButton>(R.id.manageScreenTimeButton).setOnClickListener { parentFragmentManager.popBackStack() }
            showMore?.setOnClickListener { expanded = !expanded; renderApps() }

            val childName = requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
                .getString("child_name", null)?.trim().orEmpty().ifBlank { "طفلك" }
            view.findViewById<TextView>(R.id.childNameText).text = childName
            view.findViewById<TextView>(R.id.screenTitle).text = "استخدام $childName اليوم"
        }
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        super.onViewCreated(view, state)
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.digitalRuleState.collect { result ->
                        when (result) {
                            is Result.Loading -> showLoading()
                            is Result.Success -> { render(DailyUsageSummaryMapper.map(result.data)); viewModel.clearDigitalRuleState() }
                            is Result.Error -> { showMessage("تعذر تحديث استخدام اليوم. أعد المحاولة لاحقاً."); viewModel.clearDigitalRuleState() }
                            null -> Unit
                        }
                    }
                }
            }
        }
        viewModel.getSelectedChildId()?.let(viewModel::getDigitalRule)
            ?: showMessage("اختر طفلاً لعرض استخدامه اليومي.")
    }

    override fun onResume() {
        super.onResume()
        viewModel.getSelectedChildId()?.let(viewModel::getDigitalRule)
    }

    private fun showLoading() {
        loader?.visibility = View.VISIBLE
        stateText?.visibility = View.GONE
    }

    private fun showMessage(message: String) {
        loader?.visibility = View.GONE
        summaryCard?.visibility = View.GONE
        appsCard?.visibility = View.GONE
        stateText?.apply { text = message; visibility = View.VISIBLE }
    }

    private fun render(summary: DailyUsageSummary) {
        loader?.visibility = View.GONE
        when (summary.state) {
            DailyUsageState.LIMIT_CONFIRMATION_REQUIRED -> return showMessage("حد الاستخدام يحتاج إلى تأكيد قبل عرض ملخص دقيق.")
            DailyUsageState.STALE -> return showMessage("بيانات الاستخدام ليست لليوم الحالي بعد.")
            else -> Unit
        }
        summaryCard?.visibility = View.VISIBLE
        stateText?.visibility = if (summary.state == DailyUsageState.EMPTY) View.VISIBLE else View.GONE
        if (summary.state == DailyUsageState.EMPTY) stateText?.text = "لا يوجد استخدام مسجل اليوم حتى الآن."
        appsCard?.visibility = if (summary.apps.isEmpty()) View.GONE else View.VISIBLE

        val overLimit = summary.state == DailyUsageState.OVER_LIMIT
        usageRing?.progress = summary.progressPercent
        usageRing?.setIndicatorColor(ContextCompat.getColor(requireContext(), if (overLimit) R.color.daily_usage_coral else R.color.teal_brand))
        totalText?.text = DailyUsageSummaryMapper.formatDuration(summary.totalMinutes)
        limitText?.text = "الحد اليومي ${DailyUsageSummaryMapper.formatDuration(summary.dailyLimitMinutes)}"
        remainingText?.text = if (overLimit) "تم تجاوز الحد اليومي" else "متبقّي ${DailyUsageSummaryMapper.formatDuration(summary.remainingMinutes)} اليوم"
        updatedText?.text = summary.relativeUpdatedText
        apps = summary.apps
        expanded = false
        renderApps()
    }

    private fun renderApps() {
        val container = appsList ?: return
        container.removeAllViews()
        val visible = if (expanded) apps else apps.take(4)
        visible.forEachIndexed { index, app ->
            container.addView(appRow(app))
            if (index < visible.lastIndex) container.addView(divider())
        }
        showMore?.visibility = if (apps.size > 4) View.VISIBLE else View.GONE
        showMore?.text = if (expanded) "عرض أقل" else "عرض المزيد"
    }

    private fun appRow(app: DailyUsageApp): View {
        val ctx = requireContext()
        return LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(4), dp(12), dp(4), dp(12))
            contentDescription = "${app.displayName}: ${DailyUsageSummaryMapper.formatDuration(app.usageMinutes)}"
            addView(TextView(ctx).apply {
                text = app.iconLabel; gravity = Gravity.CENTER; textSize = 13f
                setTextColor(ContextCompat.getColor(ctx, R.color.navy_brand))
                background = ContextCompat.getDrawable(ctx, R.drawable.bg_daily_app_icon)
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(34))
            })
            addView(LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(12); marginEnd = dp(12) }
                addView(TextView(ctx).apply {
                    text = app.displayName; textSize = 14f; typeface = android.graphics.Typeface.DEFAULT_BOLD
                    setTextColor(ContextCompat.getColor(ctx, R.color.navy_brand))
                })
                addView(ProgressBar(ctx, null, android.R.attr.progressBarStyleHorizontal).apply {
                    max = 100
                    val longestUsage = apps.maxOfOrNull { it.usageMinutes }?.coerceAtLeast(1) ?: 1
                    progress = app.usageMinutes * 100 / longestUsage
                    progressDrawable = ContextCompat.getDrawable(ctx, R.drawable.progress_daily_usage)
                    layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(5)).apply { topMargin = dp(7) }
                    contentDescription = "نسبة استخدام ${app.displayName}"
                })
            })
            addView(TextView(ctx).apply {
                text = DailyUsageSummaryMapper.formatDuration(app.usageMinutes); textSize = 12f
                setTextColor(ContextCompat.getColor(ctx, R.color.gray_medium))
            })
        }
    }

    private fun divider(): View = View(requireContext()).apply {
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
        setBackgroundColor(Color.parseColor("#E5F4F1"))
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}

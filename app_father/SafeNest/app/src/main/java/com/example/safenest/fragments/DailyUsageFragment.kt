package com.example.safenest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.MonitoringViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class DailyUsageFragment : Fragment() {

    companion object {
        private const val TAG = "DailyUsageFragment"
    }

    private val viewModel: MonitoringViewModel by viewModels()

    private var progressBar: ProgressBar? = null
    private var emptyText: TextView? = null
    private var summaryCard: MaterialCardView? = null
    private var appsListCard: MaterialCardView? = null
    private var totalUsageText: TextView? = null
    private var maxScreenTimeText: TextView? = null
    private var usageAppsList: LinearLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_daily_usage, container, false)

        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)
        summaryCard = view.findViewById(R.id.summaryCard)
        appsListCard = view.findViewById(R.id.appsListCard)
        totalUsageText = view.findViewById(R.id.totalUsageText)
        maxScreenTimeText = view.findViewById(R.id.maxScreenTimeText)
        usageAppsList = view.findViewById(R.id.usageAppsList)

        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

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
                                val usageLog = state.data.dailyUsageLog
                                val maxScreenTime = state.data.maxScreenTime ?: 0

                                if (usageLog.isEmpty()) {
                                    emptyText?.text = "لا يوجد بيانات استخدام"
                                    emptyText?.visibility = View.VISIBLE
                                    summaryCard?.visibility = View.GONE
                                    appsListCard?.visibility = View.GONE
                                } else {
                                    emptyText?.visibility = View.GONE
                                    summaryCard?.visibility = View.VISIBLE
                                    appsListCard?.visibility = View.VISIBLE
                                    renderSummary(usageLog, maxScreenTime)
                                    renderAppsList(usageLog)
                                }
                                viewModel.clearDigitalRuleState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                Toast.makeText(
                                    context,
                                    getString(R.string.error_loading_apps),
                                    Toast.LENGTH_SHORT
                                ).show()
                                viewModel.clearDigitalRuleState()
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }

        val childId = viewModel.getSelectedChildId()
        if (childId != null) {
            viewModel.getDigitalRule(childId)
        } else {
            emptyText?.text = getString(R.string.error_no_child)
            emptyText?.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        val childId = viewModel.getSelectedChildId() ?: return
        viewModel.getDigitalRule(childId)
    }

    // ─── Render helpers ──────────────────────────────────────────────────────

    private fun renderSummary(usageLog: Map<String, Int>, maxScreenTime: Int) {
        val totalMinutes = usageLog.values.sum()
        totalUsageText?.text = formatMinutes(totalMinutes, "إجمالي الاستخدام: ")
        maxScreenTimeText?.text = formatMinutes(maxScreenTime, "الحد اليومي: ")
    }

    private fun renderAppsList(usageLog: Map<String, Int>) {
        val ctx = requireContext()
        usageAppsList?.removeAllViews()

        usageLog.entries.sortedByDescending { it.value }.forEach { (pkg, mins) ->
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setPadding(0, 16, 0, 16)
                gravity = android.view.Gravity.CENTER_VERTICAL
            }

            // Package name (takes remaining space)
            row.addView(TextView(ctx).apply {
                text = pkg
                textSize = 14f
                setTextColor(android.graphics.Color.BLACK)
                typeface = android.graphics.Typeface.DEFAULT_BOLD
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })

            // Usage time (right-aligned in RTL)
            row.addView(TextView(ctx).apply {
                text = formatMinutes(mins, "")
                textSize = 14f
                setTextColor(android.graphics.Color.parseColor("#692AC8")) // purple_dark approx
                typeface = android.graphics.Typeface.DEFAULT_BOLD
            })

            usageAppsList?.addView(row)

            // Thin divider between rows
            usageAppsList?.addView(View(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"))
            })
        }
    }

    // ─── Format helper ────────────────────────────────────────────────────────

    private fun formatMinutes(minutes: Int, prefix: String): String {
        val h = minutes / 60
        val m = minutes % 60
        return when {
            h == 0 -> "${prefix}${m} دقيقة"
            m == 0 -> "${prefix}${h} ساعة"
            else   -> "${prefix}${h} ساعة ${m} دقيقة"
        }
    }
}

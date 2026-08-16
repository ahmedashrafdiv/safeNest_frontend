package com.example.safenest.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.example.safenest.repository.DigitalControlRepository
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.MonitoringViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
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

    private lateinit var bottomNavBar: BottomNavigationView
    private lateinit var usedAppsCard: MaterialCardView
    private var progressBar: ProgressBar? = null
    private var screenTimeTv: TextView? = null

    private var currentRuleId: String? = null

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
                // Load digital rule state
                launch {
                    viewModel.digitalRuleState.collect { state ->
                        when (state) {
                            is Result.Loading -> progressBar?.visibility = View.VISIBLE
                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                val rule = state.data
                                currentRuleId = rule.ruleId
                                updateUI(rule.maxScreenTime, rule.dailyUsageLog)
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
                                updateUI(state.data.maxScreenTime, state.data.dailyUsageLog)
                                Toast.makeText(context, getString(R.string.screen_time_set_success, state.data.maxScreenTime ?: 0), Toast.LENGTH_SHORT).show()
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
        }
    }

    private fun createDefaultRule(childId: String) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val result = digitalControlRepo.createDigitalRule(childId, maxScreenTime = 120, blockedApp = emptyList())
            if (result is Result.Success) {
                currentRuleId = result.data.ruleId
                kotlinx.coroutines.withContext(Dispatchers.Main) {
                    updateUI(result.data.maxScreenTime, result.data.dailyUsageLog)
                }
            }
        }
    }

    private fun updateUI(maxScreenTime: Int?, usageLog: Map<String, Int>? = null) {
        screenTimeTv?.text = if (maxScreenTime != null)
            getString(R.string.screen_time_limit_format, maxScreenTime)
        else
            getString(R.string.screen_time_not_set)

        // Update usage-time chip in header
        val usageTimeTv = view?.findViewById<TextView>(R.id.usageTime)
        if (usageTimeTv != null && maxScreenTime != null) {
            val todayMinutes = usageLog?.values?.sum() ?: 0
            val todayH = todayMinutes / 60; val todayM = todayMinutes % 60
            val maxH = maxScreenTime / 60; val maxM = maxScreenTime % 60
            val todayStr = if (todayH == 0) "${todayM}د" else if (todayM == 0) "${todayH}س" else "${todayH}س ${todayM}د"
            val maxStr = if (maxH == 0) "${maxM}د" else if (maxM == 0) "${maxH}س" else "${maxH}س ${maxM}د"
            usageTimeTv.text = "$todayStr من $maxStr"
        }
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
                            updateUI(null)
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

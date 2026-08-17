package com.example.safenest.fragments

import android.content.Context
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
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.R
import com.example.safenest.network.ApiClient
import com.example.safenest.policy.ParentPolicyScope
import com.example.safenest.policy.ParentPolicyScopeStore
import com.example.safenest.policy.ParentScreenTimeScopeCoordinator
import com.example.safenest.policy.ScopedScreenTimeMutation
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.MonitoringViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.Slider
import kotlinx.coroutines.launch

class ScreenTimeFragment : Fragment() {

    companion object {
        private const val TAG = "ScreenTimeFragment"
    }

    private val viewModel: MonitoringViewModel by viewModels()

    private var progressBar: ProgressBar? = null
    private var screenTimeValueText: TextView? = null
    private var screenTimeSlider: Slider? = null
    private var saveButton: MaterialButton? = null

    // Holds the DigitalRule rule ID returned from the server, needed for updates
    private var ruleId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_screen_time, container, false)

        progressBar = view.findViewById(R.id.progressBar)
        screenTimeValueText = view.findViewById(R.id.screenTimeValueText)
        screenTimeSlider = view.findViewById(R.id.screenTimeSlider)
        saveButton = view.findViewById(R.id.saveButton)

        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        // Update display label as slider moves
        screenTimeSlider?.addOnChangeListener { _, value, _ ->
            updateTimeDisplay(value.toInt())
        }

        // Save button — requires ruleId from first successful fetch
        saveButton?.setOnClickListener {
            val minutes = screenTimeSlider?.value?.toInt() ?: return@setOnClickListener
            if (ParentPolicyScopeStore.state.value.scope == ParentPolicyScope.SELECTED_DEVICE) {
                viewLifecycleOwner.lifecycleScope.launch {
                    when (val mutation = ParentScreenTimeScopeCoordinator(ApiClient.apiService).saveSelectedDeviceLimit(minutes)) {
                        ScopedScreenTimeMutation.Applied -> Toast.makeText(context, "Screen Time override saved for the selected device", Toast.LENGTH_SHORT).show()
                        is ScopedScreenTimeMutation.Blocked -> Toast.makeText(context, mutation.message, Toast.LENGTH_LONG).show()
                        is ScopedScreenTimeMutation.Failed -> Toast.makeText(context, mutation.message, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                val id = ruleId ?: return@setOnClickListener
                viewModel.updateDigitalRule(id, maxScreenTime = minutes)
            }
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
                            }
                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                ruleId = state.data.ruleId
                                val minutes = state.data.maxScreenTime ?: 120
                                screenTimeSlider?.value = minutes.toFloat().coerceIn(0f, 480f)
                                updateTimeDisplay(minutes)
                                val childId = viewModel.getSelectedChildId()
                                if (childId != null) saveToCache(childId, minutes)
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

                // Observe DigitalRule update state
                launch {
                    viewModel.updateDigitalRuleState.collect { state ->
                        when (state) {
                            is Result.Loading -> {
                                progressBar?.visibility = View.VISIBLE
                                saveButton?.isEnabled = false
                            }
                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                saveButton?.isEnabled = true
                                Toast.makeText(context, "تم الحفظ بنجاح", Toast.LENGTH_SHORT).show()
                                val childId = viewModel.getSelectedChildId()
                                if (childId != null) viewModel.getDigitalRule(childId)
                                viewModel.clearUpdateDigitalRuleState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                saveButton?.isEnabled = true
                                Toast.makeText(
                                    context,
                                    getString(R.string.error_saving_apps, ""),
                                    Toast.LENGTH_LONG
                                ).show()
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
            val cached = getFromCache(childId)
            screenTimeSlider?.value = cached.toFloat().coerceIn(0f, 480f)
            updateTimeDisplay(cached)
            viewModel.getDigitalRule(childId)
        }
    }

    override fun onResume() {
        super.onResume()
        val childId = viewModel.getSelectedChildId() ?: return
        viewModel.getDigitalRule(childId)
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun updateTimeDisplay(minutes: Int) {
        val h = minutes / 60
        val m = minutes % 60
        val text = when {
            h == 0 -> "$m دقيقة"
            m == 0 -> "$h ساعة"
            else   -> "$h ساعة $m دقيقة"
        }
        screenTimeValueText?.text = text
    }

    // ─── SharedPreferences cache ──────────────────────────────────────────────

    private fun saveToCache(childId: String, minutes: Int) {
        requireContext().getSharedPreferences("SafeNestPrefs", Context.MODE_PRIVATE)
            .edit().putInt("screen_time_$childId", minutes).apply()
    }

    private fun getFromCache(childId: String): Int =
        requireContext().getSharedPreferences("SafeNestPrefs", Context.MODE_PRIVATE)
            .getInt("screen_time_$childId", 120) // default 2 hours
}

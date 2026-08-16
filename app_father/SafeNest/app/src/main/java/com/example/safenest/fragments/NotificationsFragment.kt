package com.example.safenest.fragments

import android.app.AlertDialog
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
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.network.AlertOut
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.NotificationsViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class NotificationsFragment : Fragment() {

    companion object {
        private const val TAG = "NotificationsFragment"
    }

    private val viewModel: NotificationsViewModel by viewModels()

    private lateinit var bottomNavBar: BottomNavigationView
    private var progressBar: ProgressBar? = null
    private var emptyText: TextView? = null
    private var alertsContainer: LinearLayout? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_notifications, container, false)

        bottomNavBar = view.findViewById(R.id.bottomNavBar)
        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)
        alertsContainer = view.findViewById(R.id.alertsContainer)

        view.findViewById<MaterialButton?>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        bottomNavBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { (activity as MainActivity).navigateToFragment(HomeFragment()); true }
                R.id.nav_monitoring -> { (activity as MainActivity).navigateToFragment(MonitoringFragment()); true }
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
                viewModel.alertsState.collect { state ->
                    when (state) {
                        is Result.Loading -> {
                            progressBar?.visibility = View.VISIBLE
                            emptyText?.visibility = View.GONE
                            alertsContainer?.removeAllViews()
                        }
                        is Result.Success -> {
                            progressBar?.visibility = View.GONE
                            val alerts = state.data
                            if (alerts.isEmpty()) {
                                emptyText?.text = getString(R.string.no_alerts)
                                emptyText?.visibility = View.VISIBLE
                            } else {
                                emptyText?.visibility = View.GONE
                                alerts.forEach { alert -> addAlertCard(alert) }
                            }
                            viewModel.clearAlertsState()
                        }
                        is Result.Error -> {
                            progressBar?.visibility = View.GONE
                            emptyText?.text = getString(R.string.error_loading_alerts)
                            emptyText?.visibility = View.VISIBLE
                            viewModel.clearAlertsState()
                        }
                        null -> Unit
                    }
                }
            }
        }

        viewModel.listAlerts()
    }

    private fun addAlertCard(alert: AlertOut) {
        val ctx = requireContext()

        val card = MaterialCardView(ctx).apply {
            radius = 48f
            cardElevation = 8f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
            setCardBackgroundColor(android.graphics.Color.WHITE)
        }

        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        val typeTv = TextView(ctx).apply {
            text = alert.alertType
            textSize = 12f
            setPadding(16, 4, 16, 4)
            setTextColor(android.graphics.Color.WHITE)
            background = android.graphics.drawable.GradientDrawable().apply {
                shape = android.graphics.drawable.GradientDrawable.RECTANGLE
                cornerRadius = 32f
                setColor(
                    when {
                        alert.alertType.contains("SOS", ignoreCase = true) -> android.graphics.Color.parseColor("#E15151")
                        alert.alertType.contains("zone", ignoreCase = true) -> android.graphics.Color.parseColor("#FF9800")
                        else -> android.graphics.Color.parseColor("#692AC8")
                    }
                )
            }
        }

        val messageTv = TextView(ctx).apply {
            text = alert.message
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#333333"))
            setPadding(0, 8, 0, 0)
        }

        val timeTv = TextView(ctx).apply {
            text = alert.timestamp ?: ""
            textSize = 11f
            setTextColor(android.graphics.Color.GRAY)
            setPadding(0, 4, 0, 8)
        }

        val statusTv = TextView(ctx).apply {
            text = if (alert.isResolved) getString(R.string.alert_resolved) else getString(R.string.alert_pending)
            textSize = 12f
            setTextColor(
                if (alert.isResolved) android.graphics.Color.parseColor("#16A22B")
                else android.graphics.Color.parseColor("#E15151")
            )
        }

        val actionRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12 }
        }

        if (!alert.isResolved) {
            val resolveBtn = MaterialButton(ctx).apply {
                text = getString(R.string.mark_resolved)
                textSize = 12f
                setTextColor(android.graphics.Color.WHITE)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = 8 }
                backgroundTintList = android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#692AC8")
                )
                cornerRadius = 32
                setOnClickListener {
                    viewModel.updateAlert(alert.alertId, true)
                    statusTv.text = getString(R.string.alert_resolved)
                    statusTv.setTextColor(android.graphics.Color.parseColor("#16A22B"))
                    this.visibility = View.GONE
                    Toast.makeText(context, getString(R.string.alert_resolved_success), Toast.LENGTH_SHORT).show()
                }
            }
            actionRow.addView(resolveBtn)
        }

        val deleteBtn = MaterialButton(ctx).apply {
            text = getString(R.string.delete)
            textSize = 12f
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#E15151")
            )
            cornerRadius = 32
            setOnClickListener {
                alertsContainer?.removeView(card)
                if (alertsContainer?.childCount == 0) {
                    emptyText?.text = getString(R.string.no_alerts)
                    emptyText?.visibility = View.VISIBLE
                }
                viewModel.deleteAlert(alert.alertId)
            }
        }
        actionRow.addView(deleteBtn)

        inner.addView(typeTv)
        inner.addView(messageTv)
        inner.addView(timeTv)
        inner.addView(statusTv)
        inner.addView(actionRow)
        card.addView(inner)
        alertsContainer?.addView(card)
    }
}

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
import com.example.safenest.network.DeviceOut
import com.example.safenest.network.DeviceStatusResponse
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.DevicesViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class MyDevicesFragment : Fragment() {

    companion object {
        private const val TAG = "MyDevicesFragment"
    }

    private val viewModel: DevicesViewModel by viewModels()

    private lateinit var bottomNavBar: BottomNavigationView
    private var progressBar: ProgressBar? = null
    private var emptyText: TextView? = null
    private var devicesContainer: LinearLayout? = null

    // Hold statuses in memory for card rendering once both lists arrive
    private var cachedStatuses: Map<String, DeviceStatusResponse> = emptyMap()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_my_devices, container, false)

        bottomNavBar = view.findViewById(R.id.bottomNavBar)
        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)
        devicesContainer = view.findViewById(R.id.devicesContainer)

        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
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
                // Devices list
                launch {
                    viewModel.devicesState.collect { state ->
                        when (state) {
                            is Result.Loading -> {
                                progressBar?.visibility = View.VISIBLE
                                emptyText?.visibility = View.GONE
                                devicesContainer?.removeAllViews()
                            }
                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                val devices = state.data
                                if (devices.isEmpty()) {
                                    emptyText?.visibility = View.VISIBLE
                                } else {
                                    emptyText?.visibility = View.GONE
                                    devices.forEach { device ->
                                        addDeviceCard(device, cachedStatuses[device.deviceId])
                                    }
                                }
                                viewModel.clearDevicesState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                emptyText?.text = getString(R.string.error_loading_devices)
                                emptyText?.visibility = View.VISIBLE
                                viewModel.clearDevicesState()
                            }
                            null -> Unit
                        }
                    }
                }

                // Statuses (fire and forget — cache when received)
                launch {
                    viewModel.devicesStatusState.collect { state ->
                        when (state) {
                            is Result.Success -> {
                                cachedStatuses = state.data.associateBy { it.deviceId }
                                viewModel.clearDevicesStatusState()
                            }
                            is Result.Error -> viewModel.clearDevicesStatusState()
                            else -> Unit
                        }
                    }
                }
            }
        }

        viewModel.listDevicesStatus()
        viewModel.listDevices()
    }

    private fun addDeviceCard(device: DeviceOut, status: DeviceStatusResponse?) {
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

        val headerRow = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val nameTv = TextView(ctx).apply {
            text = device.deviceName
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#692AC8"))
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val typeTv = TextView(ctx).apply {
            text = device.deviceType
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
        }

        headerRow.addView(nameTv)
        headerRow.addView(typeTv)

        val isOnline = isDeviceOnline(device.lastActive)
        val statusTv = TextView(ctx).apply {
            text = if (isOnline) getString(R.string.device_status_online) else getString(R.string.device_status_offline)
            textSize = 13f
            setPadding(0, 8, 0, 0)
            setTextColor(
                if (isOnline) android.graphics.Color.parseColor("#16A22B")
                else android.graphics.Color.parseColor("#E15151")
            )
        }

        if (device.lastActive != null) {
            val lastActiveTv = TextView(ctx).apply {
                text = getString(R.string.last_active_format, device.lastActive)
                textSize = 11f
                setTextColor(android.graphics.Color.GRAY)
                setPadding(0, 4, 0, 0)
            }
            inner.addView(lastActiveTv)
        }

        if (status != null) {
            val batteryTv = TextView(ctx).apply {
                text = getString(R.string.battery_level_format, status.batteryLevel)
                textSize = 12f
                setTextColor(android.graphics.Color.parseColor("#692AC8"))
                setPadding(0, 4, 0, 0)
            }
            inner.addView(batteryTv)
        }

        val deleteBtn = MaterialButton(ctx).apply {
            text = getString(R.string.delete_device)
            textSize = 13f
            setTextColor(android.graphics.Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = 12 }
            backgroundTintList = android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor("#E15151")
            )
            cornerRadius = 32
            setOnClickListener { confirmDeleteDevice(device, card) }
        }

        inner.addView(headerRow)
        inner.addView(statusTv)
        inner.addView(deleteBtn)
        card.addView(inner)
        devicesContainer?.addView(card)
    }

    private fun isDeviceOnline(lastActive: String?): Boolean {
        if (lastActive == null) return false
        return try {
            val fmt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            val last = fmt.parse(lastActive) ?: return false
            (System.currentTimeMillis() - last.time) / 60000 < 10
        } catch (e: Exception) {
            false
        }
    }

    private fun confirmDeleteDevice(device: DeviceOut, card: MaterialCardView) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_device_confirm_title))
            .setMessage(getString(R.string.delete_device_confirm_message, device.deviceName))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                devicesContainer?.removeView(card)
                if (devicesContainer?.childCount == 0) {
                    emptyText?.text = getString(R.string.no_devices_paired)
                    emptyText?.visibility = View.VISIBLE
                }
                viewModel.deleteDevice(device.deviceId)
                Toast.makeText(context, getString(R.string.device_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}

package com.example.safenest.fragments

import android.app.AlertDialog
import android.graphics.Color
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
import androidx.lifecycle.lifecycleScope
import com.example.safenest.R
import com.example.safenest.network.ChildDeviceSummary
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.ChildDevicesViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/**
 * Layngo device field guide: every card names one physical device and every
 * destructive action explicitly targets that card, never all sibling devices.
 */
class MyDevicesFragment : Fragment() {
    private val viewModel: ChildDevicesViewModel by viewModels()
    private var progressBar: ProgressBar? = null
    private var emptyText: TextView? = null
    private var devicesContainer: LinearLayout? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_my_devices, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)
        devicesContainer = view.findViewById(R.id.devicesContainer)
        view.findViewById<View>(R.id.backButton).setOnClickListener { parentFragmentManager.popBackStack() }
        addPairingAction(view)
        observeState()
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.scopeChildDefault).setOnClickListener {
            Toast.makeText(requireContext(), "Future policies will use the child default", Toast.LENGTH_SHORT).show()
        }
        view.findViewById<com.google.android.material.button.MaterialButton>(R.id.scopeSelectedDevice).setOnClickListener {
            Toast.makeText(requireContext(), "Select a device card before applying an override", Toast.LENGTH_SHORT).show()
        }

        viewModel.loadDevices()
    }

    private fun addPairingAction(root: View) {
        val button = MaterialButton(requireContext()).apply {
            text = "Add another device"
            setTextColor(Color.WHITE)
            backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2CA39D"))
            setOnClickListener { viewModel.createPairing() }
        }
        (root.findViewById<View>(R.id.devicesContainer).parent as? LinearLayout)?.addView(button, 0)
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.devicesState.collect { state ->
                when (state) {
                    is Result.Loading -> progressBar?.visibility = View.VISIBLE
                    is Result.Success -> {
                        progressBar?.visibility = View.GONE
                        renderDevices(state.data)
                    }
                    is Result.Error -> {
                        progressBar?.visibility = View.GONE
                        showEmpty(state.message)
                    }
                    null -> Unit
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.pairingState.collect { state ->
                when (state) {
                    is Result.Success -> {
                        Toast.makeText(requireContext(), "Pairing code: ${state.data.pairingCode}", Toast.LENGTH_LONG).show()
        requireView().findViewById<com.google.android.material.button.MaterialButton>(R.id.scopeChildDefault).setOnClickListener {
            Toast.makeText(requireContext(), "Future policies will use the child default", Toast.LENGTH_SHORT).show()
        }
        requireView().findViewById<com.google.android.material.button.MaterialButton>(R.id.scopeSelectedDevice).setOnClickListener {
            Toast.makeText(requireContext(), "Select a device card before applying an override", Toast.LENGTH_SHORT).show()
        }

        viewModel.loadDevices()
                    }
                    is Result.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    else -> Unit
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.revokeState.collect { state ->
                when (state) {
                    is Result.Success -> viewModel.loadDevices()
                    is Result.Error -> Toast.makeText(requireContext(), state.message, Toast.LENGTH_LONG).show()
                    else -> Unit
                }
            }
        }
    }

    private fun renderDevices(devices: List<ChildDeviceSummary>) {
        devicesContainer?.removeAllViews()
        if (devices.isEmpty()) { showEmpty("No devices are paired to this child yet"); return }
        emptyText?.visibility = View.GONE
        val activeCount = devices.count { it.status.equals("active", ignoreCase = true) }
        devicesContainer?.addView(label("$activeCount of ${devices.size} devices active", 14f, "#15385F"))
        devices.forEach { devicesContainer?.addView(deviceCard(it)) }
    }

    private fun deviceCard(device: ChildDeviceSummary): View = MaterialCardView(requireContext()).apply {
        radius = 28f
        setCardBackgroundColor(Color.WHITE)
        strokeWidth = 1
        strokeColor = Color.parseColor("#D6E0EB")
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 }
        addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            addView(label("${device.model} Â· ${device.platform}", 18f, "#15385F"))
            addView(label("${device.status.replaceFirstChar { it.uppercase() }} Â· ${device.trustState.replaceFirstChar { it.uppercase() }}", 14f, "#2CA39D"))
            addView(label("Last seen: ${device.lastSeenAt ?: "Not yet reported"}", 13f, "#6B7280"))
            addView(MaterialButton(requireContext()).apply {
                text = "Revoke this device"
                setTextColor(Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#B94040"))
                setOnClickListener { confirmRevoke(device) }
            })
        })
    }

    private fun label(textValue: String, size: Float, color: String) = TextView(requireContext()).apply {
        text = textValue
        textSize = size
        setTextColor(Color.parseColor(color))
        setPadding(0, 0, 0, 10)
    }

    private fun confirmRevoke(device: ChildDeviceSummary) {
        AlertDialog.Builder(requireContext())
            .setTitle("Revoke ${device.model}?")
            .setMessage("Only this device will lose access. Other devices paired to this child remain unchanged.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Revoke") { _, _ -> viewModel.revokeDevice(device.deviceId) }
            .show()
    }

    private fun showEmpty(message: String) {
        devicesContainer?.removeAllViews()
        emptyText?.text = message
        emptyText?.visibility = View.VISIBLE
    }
}


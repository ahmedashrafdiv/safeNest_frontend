package com.example.safenest.fragments

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.safenest.R
import com.example.safenest.network.ChildDeviceSummary
import com.example.safenest.network.EffectiveProtectionPolicyResponse
import com.example.safenest.policy.ParentProtectedHomeScopeCoordinator
import com.example.safenest.policy.ParentPolicyScope
import com.example.safenest.policy.ParentPolicyScopeStore
import com.example.safenest.policy.ProtectedHomePolicyMutation
import com.example.safenest.policy.SelectedPolicyDevice
import com.example.safenest.protection.DeviceProtectionStatusFormatter
import com.example.safenest.protection.ProtectedHomePolicyStatusFormatter
import com.example.safenest.repository.ChildDeviceRepository
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.ChildDevicesViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.switchmaterial.SwitchMaterial
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
    private var scopeContextText: TextView? = null
    private val protectedHomeCoordinator by lazy { ParentProtectedHomeScopeCoordinator(ChildDeviceRepository()) }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_my_devices, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)
        devicesContainer = view.findViewById(R.id.devicesContainer)
        scopeContextText = view.findViewById(R.id.scopeContextText)
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
        setOnClickListener { selectDeviceForPolicyScope(device) }
        layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 16 }
        addView(LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
            addView(label("${device.model} Â· ${device.platform}", 18f, "#15385F"))
            addView(label("${device.status.replaceFirstChar { it.uppercase() }} Â· ${device.trustState.replaceFirstChar { it.uppercase() }}", 14f, "#2CA39D"))
            addView(label("Last seen: ${device.lastSeenAt ?: "Not yet reported"}", 13f, "#6B7280"))
            val protectionStatus = DeviceProtectionStatusFormatter.format(
                health = device.protectionHealth,
                mode = device.protectionMode,
                uninstallProtectionConfirmed = device.uninstallProtectionConfirmed,
                permissionStates = device.permissionStates,
            )
            addView(label(protectionStatus.text, 13f, protectionStatus.colorHex))
            addView(protectedHomePanel(device))
            addView(MaterialButton(requireContext()).apply {
                text = "Revoke this device"
                setTextColor(Color.WHITE)
                backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#B94040"))
                setOnClickListener { confirmRevoke(device) }
            })
        })
    }

    private fun protectedHomePanel(device: ChildDeviceSummary): View {
        val card = MaterialCardView(requireContext()).apply {
            radius = 20f
            strokeWidth = 1
            strokeColor = Color.parseColor("#B9DDD9")
            setCardBackgroundColor(Color.parseColor("#F5FBFA"))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
            ).apply { bottomMargin = 14 }
        }
        val content = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 18)
        }
        val title = label("Home Screen Protection", 16f, "#15385F")
        val detail = label("Loading this device’s protection preference…", 13f, "#6B7280")
        val toggle = SwitchMaterial(requireContext()).apply {
            text = "Use Layngo Protected Home"
            textSize = 14f
            setTextColor(Color.parseColor("#15385F"))
            minimumHeight = 48
            isEnabled = false
        }
        content.addView(title)
        content.addView(detail)
        content.addView(toggle)
        card.addView(content)

        var latest: EffectiveProtectionPolicyResponse? = null
        lateinit var toggleListener: CompoundButton.OnCheckedChangeListener

        fun render(policy: EffectiveProtectionPolicyResponse) {
            latest = policy
            val status = ProtectedHomePolicyStatusFormatter.format(
                requested = policy.values.protectedHomeRequested,
                permissionState = device.permissionStates["protected_home"],
            )
            title.text = status.title
            detail.text = status.detail
            detail.setTextColor(Color.parseColor(status.colorHex))
            toggle.setOnCheckedChangeListener(null)
            toggle.isChecked = policy.values.protectedHomeRequested
            toggle.isEnabled = device.status.equals("active", ignoreCase = true)
            toggle.setOnCheckedChangeListener(toggleListener)
        }

        fun refresh() {
            val childId = viewModel.selectedChildId() ?: run {
                detail.text = "Select a child before changing device protection."
                return
            }
            viewLifecycleOwner.lifecycleScope.launch {
                when (val result = ChildDeviceRepository().getEffectiveProtectionPolicy(childId, device.deviceId)) {
                    is Result.Success -> render(result.data)
                    is Result.Error -> {
                        detail.text = result.message
                        detail.setTextColor(Color.parseColor("#B94040"))
                        toggle.isEnabled = false
                    }
                    Result.Loading -> Unit
                }
            }
        }

        fun persist(requested: Boolean, expectedVersion: Int) {
            val childId = viewModel.selectedChildId() ?: run {
                detail.text = "Select a child before changing device protection."
                return
            }
            toggle.isEnabled = false
            detail.text = "Saving the protection preference…"
            viewLifecycleOwner.lifecycleScope.launch {
                when (val mutation = protectedHomeCoordinator.saveForDevice(
                    childId = childId,
                    deviceId = device.deviceId,
                    requested = requested,
                    expectedVersion = expectedVersion,
                )) {
                    is ProtectedHomePolicyMutation.Applied -> refresh()
                    is ProtectedHomePolicyMutation.Failed -> {
                        detail.text = mutation.message
                        detail.setTextColor(Color.parseColor("#B94040"))
                        latest?.let(::render)
                    }
                    is ProtectedHomePolicyMutation.Conflict -> {
                        render(mutation.latest)
                        AlertDialog.Builder(requireContext())
                            .setTitle("Protection policy changed")
                            .setMessage("The latest device setting was loaded. Apply your choice again using the current version?")
                            .setNegativeButton("Keep latest", null)
                            .setPositiveButton("Apply my choice") { _, _ ->
                                persist(requested, mutation.latest.version)
                            }
                            .show()
                    }
                }
            }
        }

        toggleListener = CompoundButton.OnCheckedChangeListener { _, requested ->
            val current = latest ?: return@OnCheckedChangeListener
            persist(requested, current.version)
        }
        refresh()
        return card
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

    private fun selectDeviceForPolicyScope(device: ChildDeviceSummary) {
        ParentPolicyScopeStore.selectDevice(
            childId = viewModel.selectedChildId(),
            device = SelectedPolicyDevice(
                deviceId = device.deviceId,
                label = device.model,
                status = device.status,
            ),
        )
        requireView().findViewById<com.google.android.material.button.MaterialButton>(R.id.scopeSelectedDevice).isChecked = true
        renderScopeContext()
        Toast.makeText(requireContext(), "Selected ${device.model} for device-specific policies", Toast.LENGTH_SHORT).show()
    }

    private fun renderScopeContext() {
        val state = ParentPolicyScopeStore.state.value
        scopeContextText?.text = when (state.scope) {
            ParentPolicyScope.CHILD_DEFAULT -> "Child default Â· applies to every active device unless overridden"
            ParentPolicyScope.SELECTED_DEVICE -> state.selectedDevice?.let { device ->
                if (device.isEligible) "Device override Â· ${device.label}"
                else "Device override unavailable Â· ${device.label} is not active"
            } ?: "Device override Â· select an active device"
        }
    }}


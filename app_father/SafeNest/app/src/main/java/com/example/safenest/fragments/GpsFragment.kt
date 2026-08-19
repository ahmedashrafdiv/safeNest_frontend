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
import com.example.safenest.network.ApiClient
import com.example.safenest.network.ChildDeviceSummary
import com.example.safenest.policy.ParentPhoneLocationScopeCoordinator
import com.example.safenest.policy.ParentPolicyScopeStore
import com.example.safenest.policy.GpsDeviceSelectionResolver
import com.example.safenest.policy.SelectedPolicyDevice
import com.example.safenest.policy.ScopedLocationMutation
import com.example.safenest.repository.ChildDeviceRepository
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.GpsViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

class GpsFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val TAG = "GpsFragment"
    }

    private val viewModel: GpsViewModel by viewModels()

    private lateinit var bottomNavBar: BottomNavigationView
    private lateinit var alertCard: MaterialCardView
    private lateinit var mapCard: MaterialCardView
    private lateinit var phoneTrackingSwitch: MaterialSwitch
    private lateinit var gpsDeviceButton: MaterialButton
    private var phoneTrackingRequestInFlight = false
    private var progressBar: ProgressBar? = null
    private var locationTv: TextView? = null
    private var lastUpdateTv: TextView? = null
    private var batteryTv: TextView? = null
    private var statusTv: TextView? = null
    private var googleMap: GoogleMap? = null
    private var lastKnownLat: Double? = null
    private var lastKnownLng: Double? = null
    private val childDeviceRepository = ChildDeviceRepository()
    private var availableGpsDevices: List<SelectedPolicyDevice> = emptyList()
    private var selectedGpsDevice: SelectedPolicyDevice? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gps, container, false)

        bottomNavBar = view.findViewById(R.id.bottomNavBar)
        alertCard = view.findViewById(R.id.alertCard)
        mapCard = view.findViewById(R.id.mapCard)
        phoneTrackingSwitch = view.findViewById(R.id.switch_phone_tracking)
        gpsDeviceButton = view.findViewById(R.id.button_select_gps_device)
        gpsDeviceButton.setOnClickListener { showGpsDevicePicker() }
        phoneTrackingSwitch.setOnCheckedChangeListener { _, enabled ->
            if (phoneTrackingRequestInFlight) return@setOnCheckedChangeListener
            phoneTrackingRequestInFlight = true
            phoneTrackingSwitch.isEnabled = false
            val childId = viewModel.getSelectedChildId()
            val targetDevice = selectedGpsDevice
            if (childId.isNullOrBlank() || targetDevice == null) {
                phoneTrackingSwitch.isChecked = !enabled
                phoneTrackingRequestInFlight = false
                phoneTrackingSwitch.isEnabled = true
                Toast.makeText(context, "اختر جهاز الطفل أولاً", Toast.LENGTH_LONG).show()
                return@setOnCheckedChangeListener
            }
            viewLifecycleOwner.lifecycleScope.launch {
                when (val mutation = ParentPhoneLocationScopeCoordinator(ApiClient.apiService)
                    .setDeviceTracking(childId, targetDevice, enabled)) {
                    ScopedLocationMutation.Applied -> {
                        Toast.makeText(context, "تم تحديث تتبع الهاتف للجهاز المحدد", Toast.LENGTH_SHORT).show()
                        refreshLocationForCurrentScope()
                    }
                    is ScopedLocationMutation.Blocked -> {
                        phoneTrackingSwitch.isChecked = !enabled
                        Toast.makeText(context, mutation.message, Toast.LENGTH_LONG).show()
                    }
                    is ScopedLocationMutation.Failed -> {
                        phoneTrackingSwitch.isChecked = !enabled
                        Toast.makeText(context, mutation.message, Toast.LENGTH_LONG).show()
                    }
                }
                phoneTrackingRequestInFlight = false
                phoneTrackingSwitch.isEnabled = true
            }
        }
        progressBar = view.findViewById(R.id.progressBar)
        locationTv = view.findViewById(R.id.locationText)
        lastUpdateTv = view.findViewById(R.id.lastUpdateText)
        batteryTv = view.findViewById(R.id.batteryText)
        statusTv = view.findViewById(R.id.statusText)
        renderGpsDeviceContext()

        bottomNavBar.selectedItemId = R.id.nav_gps

        alertCard.setOnClickListener {
            (activity as MainActivity).navigateToFragment(NotificationsFragment())
        }

        // Initialize Google Map
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)

        bottomNavBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { (activity as MainActivity).navigateToFragment(HomeFragment()); true }
                R.id.nav_monitoring -> { (activity as MainActivity).navigateToFragment(MonitoringFragment()); true }
                R.id.nav_gps -> true
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
                // Location state
                launch {
                    viewModel.locationState.collect { state ->
                        when (state) {
                            is Result.Loading -> {
                                progressBar?.visibility = View.VISIBLE
                                statusTv?.text = "جاري تحميل البيانات..."
                            }
                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                val locationData = state.data
                                locationData.trackingActive?.let { enabled ->
                                    phoneTrackingRequestInFlight = true
                                    phoneTrackingSwitch.isChecked = enabled
                                    phoneTrackingRequestInFlight = false
                                }
                                val coordinate = locationData.effectiveCoordinate()
                                val sourceLabel = when (locationData.effectiveSource()) {
                                    "phone" -> "هاتف الطفل"
                                    "external_gps" -> "GPS خارجي"
                                    else -> "مصدر غير معروف"
                                }
                                val freshnessLabel = when {
                                    locationData.availabilityStatus == "available" && !locationData.isStale -> "متصل ✓"
                                    locationData.availabilityStatus == "stale" || locationData.isStale -> "آخر نقطة قديمة ✗"
                                    locationData.availabilityStatus == "disabled" -> "تتبع الهاتف متوقف"
                                    else -> "الموقع غير متاح ✗"
                                }

                                if (coordinate != null && locationData.availabilityStatus != "unavailable") {
                                    lastKnownLat = coordinate.latitude
                                    lastKnownLng = coordinate.longitude
                                    locationTv?.text = "الموقع: %.6f, %.6f\nالمصدر: $sourceLabel".format(coordinate.latitude, coordinate.longitude)
                                    statusTv?.text = freshnessLabel
                                    updateMapLocation(coordinate.latitude, coordinate.longitude, sourceLabel, locationData.isStale)
                                } else {
                                    locationTv?.text = "لا يوجد موقع محدد\nالحالة: $freshnessLabel"
                                    statusTv?.text = freshnessLabel
                                }

                                val lastUpdate = locationData.capturedAt
                                    ?: locationData.receivedAt
                                    ?: locationData.legacyLastUpdate
                                if (lastUpdate != null) {
                                    val age = locationData.ageSeconds?.let { " (العمر: ${it}s)" } ?: ""
                                    val accuracy = locationData.accuracyMeters?.let { " | الدقة: %.0fm".format(it) } ?: ""
                                    lastUpdateTv?.text = "آخر تحديث: $lastUpdate$age$accuracy"
                                }
                                viewModel.clearLocationState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                locationTv?.text = "لا يوجد موقع متاح"
                                statusTv?.text = "الجهاز غير متصل"
                                viewModel.clearLocationState()
                            }
                            null -> Unit
                        }
                    }
                }

                launch {
                    viewModel.phoneTrackingPolicyState.collect { policyState ->
                        when (policyState) {
                            is Result.Loading -> Unit
                            is Result.Success -> {
                                phoneTrackingRequestInFlight = true
                                phoneTrackingSwitch.isChecked = policyState.data.enabled
                                phoneTrackingRequestInFlight = false
                                phoneTrackingSwitch.isEnabled = true
                                Toast.makeText(requireContext(), if (policyState.data.enabled) "تم تفعيل تتبع الهاتف" else "تم إيقاف تتبع الهاتف", Toast.LENGTH_SHORT).show()
                                viewModel.clearPhoneTrackingPolicyState()
                                refreshLocationForCurrentScope()
                            }
                            is Result.Error -> {
                                phoneTrackingRequestInFlight = false
                                phoneTrackingSwitch.isEnabled = true
                                Toast.makeText(requireContext(), "تعذر تحديث سياسة تتبع الهاتف", Toast.LENGTH_SHORT).show()
                                viewModel.clearPhoneTrackingPolicyState()
                            }
                            null -> Unit
                        }
                    }
                }

                // Child info state (for device ID / battery)
                launch {
                    viewModel.childState.collect { state ->
                        when (state) {
                            is Result.Success -> {
                                val deviceId = state.data.deviceId
                                if (deviceId != null) batteryTv?.text = "معرف الجهاز: $deviceId"
                                viewModel.clearChildState()
                            }
                            is Result.Error -> viewModel.clearChildState()
                            else -> Unit
                        }
                    }
                }
            }
        }

        val childId = viewModel.getSelectedChildId()
        if (childId != null) {
            loadGpsDevices(childId)
            viewModel.getChild(childId)
        } else {
            statusTv?.text = "لا يوجد طفل مرتبط"
        }
    }

    private fun refreshLocationForCurrentScope() {
        val childId = viewModel.getSelectedChildId() ?: return
        val device = selectedGpsDevice
        if (device == null) {
            locationTv?.text = "اختر جهاز الطفل لعرض موقعه"
            statusTv?.text = "لا يوجد جهاز محدد"
            return
        }
        viewModel.getChildLocation(childId, device.deviceId)
    }

    private fun loadGpsDevices(childId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = childDeviceRepository.listDevices(childId)) {
                is Result.Success -> {
                    availableGpsDevices = result.data.map { it.toGpsPolicyDevice() }
                    selectedGpsDevice = GpsDeviceSelectionResolver.resolve(
                        ParentPolicyScopeStore.state.value,
                        availableGpsDevices,
                    )
                    renderGpsDeviceContext()
                    refreshLocationForCurrentScope()
                }
                is Result.Error -> {
                    availableGpsDevices = emptyList()
                    selectedGpsDevice = null
                    renderGpsDeviceContext()
                    statusTv?.text = result.message
                }
                Result.Loading -> Unit
            }
        }
    }

    private fun showGpsDevicePicker() {
        val activeDevices = availableGpsDevices.filter { it.isEligible }
        if (activeDevices.isEmpty()) {
            Toast.makeText(context, "لا يوجد جهاز طفل نشط", Toast.LENGTH_LONG).show()
            return
        }
        AlertDialog.Builder(requireContext())
            .setTitle("اختر جهاز الطفل للموقع")
            .setItems(activeDevices.map { it.label }.toTypedArray()) { _, index ->
                selectedGpsDevice = activeDevices[index]
                ParentPolicyScopeStore.selectDevice(viewModel.getSelectedChildId(), activeDevices[index])
                renderGpsDeviceContext()
                refreshLocationForCurrentScope()
            }
            .show()
    }

    private fun renderGpsDeviceContext() {
        val device = selectedGpsDevice
        val activeCount = availableGpsDevices.count { it.isEligible }
        gpsDeviceButton.text = when {
            device != null -> "موقع الهاتف: ${device.label}"
            activeCount > 1 -> "اختر جهاز الطفل للموقع ($activeCount أجهزة نشطة)"
            activeCount == 1 -> "جاري تحديد جهاز الطفل"
            else -> "اختر جهاز الطفل للموقع"
        }
        phoneTrackingSwitch.text = device?.let { "تتبع موقع الهاتف: ${it.label}" }
            ?: "تتبع موقع هاتف الطفل"
        phoneTrackingSwitch.isEnabled = device != null && !phoneTrackingRequestInFlight
    }

    private fun ChildDeviceSummary.toGpsPolicyDevice() = SelectedPolicyDevice(
        deviceId = deviceId,
        label = "${model.ifBlank { platform }} ID ${deviceId.takeLast(6)}",
        status = status,
        lastSeenAt = lastSeenAt,
    )

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isMyLocationButtonEnabled = false
        // If we already have location data, show it
        val lat = lastKnownLat
        val lng = lastKnownLng
        if (lat != null && lng != null) {
            updateMapLocation(lat, lng)
        }
    }

    private fun updateMapLocation(lat: Double, lng: Double, source: String = "موقع الطفل", isStale: Boolean = false) {
        val position = LatLng(lat, lng)
        googleMap?.clear()
        googleMap?.addMarker(
            MarkerOptions()
                .position(position)
                .title(if (isStale) "$source — قديم" else source)
                .icon(BitmapDescriptorFactory.defaultMarker(if (isStale) BitmapDescriptorFactory.HUE_ORANGE else BitmapDescriptorFactory.HUE_VIOLET))
        )
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(position, 15f)
        )
    }
}

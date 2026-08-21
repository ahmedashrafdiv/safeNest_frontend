package com.example.safenest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.network.ChildDeviceSummary
import com.example.safenest.repository.ChildDeviceRepository
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.GpsViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/** Calm location overview: only the latest available point and its freshness are presented. */
class GpsFragment : Fragment(), OnMapReadyCallback {
    private val viewModel: GpsViewModel by viewModels()
    private val deviceRepository = ChildDeviceRepository()
    private var selectedDeviceId: String? = null
    private var map: GoogleMap? = null
    private var pendingPoint: LatLng? = null

    private lateinit var progress: ProgressBar
    private lateinit var freshness: TextView
    private lateinit var location: TextView
    private lateinit var lastUpdate: TextView
    private lateinit var status: TextView
    private lateinit var refreshButton: MaterialButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_gps, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progress = view.findViewById(R.id.progressBar)
        freshness = view.findViewById(R.id.location_freshness_text)
        location = view.findViewById(R.id.locationText)
        lastUpdate = view.findViewById(R.id.lastUpdateText)
        status = view.findViewById(R.id.statusText)
        refreshButton = view.findViewById(R.id.btn_refresh_location)
        view.findViewById<MaterialButton>(R.id.btn_location_back).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<MaterialButton>(R.id.btn_manage_places).setOnClickListener { (activity as MainActivity).navigateToFragment(SafeZonesFragment()) }
        refreshButton.setOnClickListener { loadLocation() }
        setupBottomNav(view.findViewById(R.id.bottomNavBar))
        (childFragmentManager.findFragmentById(R.id.mapFragment) as? SupportMapFragment)?.getMapAsync(this)
        observeLocation()
        resolveDeviceAndLoad()
    }

    private fun observeLocation() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.locationState.collect { state ->
                when (state) {
                    is Result.Loading -> setLoading(true)
                    is Result.Success -> {
                        setLoading(false)
                        val data = state.data
                        val coordinate = data.effectiveCoordinate()
                        val updated = data.capturedAt ?: data.receivedAt ?: data.legacyLastUpdate
                        when {
                            coordinate != null && data.availabilityStatus == "available" && !data.isStale -> {
                                freshness.text = data.ageSeconds?.let { "آخر تحديث منذ ${it / 60} دقيقة" } ?: "آخر تحديث متاح"
                                location.text = "آخر موقع معروف: بالقرب من مكان مألوف"
                                lastUpdate.text = updated?.let { "تم التحديث: $it" } ?: "تم التحديث مؤخرًا"
                                status.visibility = View.GONE
                                showPoint(LatLng(coordinate.latitude, coordinate.longitude))
                            }
                            data.availabilityStatus == "stale" || data.isStale -> showUnavailable("آخر موقع وصل منذ مدة، وقد لا يعكس المكان الحالي.", updated)
                            data.availabilityStatus == "disabled" -> showUnavailable("تتبع موقع الهاتف متوقف حاليًا.", null)
                            else -> showUnavailable("لا يتوفر موقع حديث الآن.", null)
                        }
                        viewModel.clearLocationState()
                    }
                    is Result.Error -> {
                        setLoading(false)
                        showUnavailable("تعذر تحديث الموقع الآن. حاول مرة أخرى.", null)
                        viewModel.clearLocationState()
                    }
                    null -> Unit
                }
            }
        }
    }

    private fun resolveDeviceAndLoad() {
        val childId = viewModel.getSelectedChildId()
        if (childId.isNullOrBlank()) {
            showUnavailable("اختر ملف الطفل أولاً لعرض موقعه.", null)
            return
        }
        viewLifecycleOwner.lifecycleScope.launch {
            when (val result = deviceRepository.listDevices(childId)) {
                is Result.Success -> {
                    selectedDeviceId = result.data.firstOrNull()?.deviceId
                    loadLocation()
                }
                is Result.Error -> showUnavailable("لا يوجد جهاز طفل نشط لعرض الموقع.", null)
                Result.Loading -> Unit
            }
        }
    }

    private fun loadLocation() {
        val childId = viewModel.getSelectedChildId()
        val deviceId = selectedDeviceId
        if (childId.isNullOrBlank() || deviceId.isNullOrBlank()) return
        viewModel.getChildLocation(childId, deviceId)
    }

    private fun setLoading(loading: Boolean) {
        progress.visibility = if (loading) View.VISIBLE else View.GONE
        refreshButton.isEnabled = !loading
        if (loading) freshness.text = "جاري تحديث الموقع"
    }

    private fun showUnavailable(message: String, timestamp: String?) {
        freshness.text = "الموقع غير متاح الآن"
        location.text = "آخر موقع معروف: غير متاح"
        lastUpdate.text = timestamp?.let { "آخر تحديث متاح: $it" } ?: "سيظهر التحديث عند اتصال جهاز ليان."
        status.text = message
        status.visibility = View.VISIBLE
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        runCatching { googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.location_map_calm)) }
        googleMap.uiSettings.isMapToolbarEnabled = false
        googleMap.uiSettings.isZoomControlsEnabled = false
        pendingPoint?.let(::showPoint)
    }

    private fun showPoint(point: LatLng) {
        pendingPoint = point
        map?.let { googleMap ->
            googleMap.clear()
            googleMap.addMarker(MarkerOptions().position(point).title("آخر موقع متاح").icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)))
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 14.5f))
        }
    }

    private fun setupBottomNav(nav: BottomNavigationView) {
        nav.selectedItemId = R.id.nav_gps
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { (activity as MainActivity).navigateToFragment(HomeFragment()); true }
                R.id.nav_monitoring -> { (activity as MainActivity).navigateToFragment(MonitoringFragment()); true }
                R.id.nav_gps -> true
                R.id.nav_sensors -> { (activity as MainActivity).navigateToFragment(SensorsFragment()); true }
                R.id.nav_more -> { (activity as MainActivity).navigateToFragment(MoreFragment()); true }
                else -> false
            }
        }
    }
}

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
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class GpsFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val TAG = "GpsFragment"
    }

    private val viewModel: GpsViewModel by viewModels()

    private lateinit var bottomNavBar: BottomNavigationView
    private lateinit var alertCard: MaterialCardView
    private lateinit var mapCard: MaterialCardView
    private var progressBar: ProgressBar? = null
    private var locationTv: TextView? = null
    private var lastUpdateTv: TextView? = null
    private var batteryTv: TextView? = null
    private var statusTv: TextView? = null
    private var googleMap: GoogleMap? = null
    private var lastKnownLat: Double? = null
    private var lastKnownLng: Double? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_gps, container, false)

        bottomNavBar = view.findViewById(R.id.bottomNavBar)
        alertCard = view.findViewById(R.id.alertCard)
        mapCard = view.findViewById(R.id.mapCard)
        progressBar = view.findViewById(R.id.progressBar)
        locationTv = view.findViewById(R.id.locationText)
        lastUpdateTv = view.findViewById(R.id.lastUpdateText)
        batteryTv = view.findViewById(R.id.batteryText)
        statusTv = view.findViewById(R.id.statusText)

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

                                val lat = (locationData["latitude"] as? Double)
                                    ?: (locationData["lat"] as? Double)
                                    ?: (locationData["latitude"] as? Number)?.toDouble()
                                    ?: (locationData["lat"] as? Number)?.toDouble()

                                val lng = (locationData["longitude"] as? Double)
                                    ?: (locationData["lng"] as? Double)
                                    ?: (locationData["longitude"] as? Number)?.toDouble()
                                    ?: (locationData["lng"] as? Number)?.toDouble()

                                val lastUpdate = locationData["last_updated"]?.toString()
                                    ?: locationData["timestamp"]?.toString()

                                if (lat != null && lng != null) {
                                    lastKnownLat = lat
                                    lastKnownLng = lng
                                    locationTv?.text = "الموقع: %.6f, %.6f".format(lat, lng)
                                    statusTv?.text = "متصل ✓"
                                    updateMapLocation(lat, lng)
                                } else {
                                    locationTv?.text = "لا يوجد موقع محدد"
                                    statusTv?.text = "انتظار الجهاز..."
                                }

                                if (lastUpdate != null) {
                                    lastUpdateTv?.text = "آخر تحديث: $lastUpdate"
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
            viewModel.getChildLocation(childId)
            viewModel.getChild(childId)
        } else {
            statusTv?.text = "لا يوجد طفل مرتبط"
        }
    }

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

    private fun updateMapLocation(lat: Double, lng: Double) {
        val position = LatLng(lat, lng)
        googleMap?.clear()
        googleMap?.addMarker(
            MarkerOptions()
                .position(position)
                .title("موقع الطفل")
                .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_VIOLET))
        )
        googleMap?.animateCamera(
            CameraUpdateFactory.newLatLngZoom(position, 15f)
        )
    }
}

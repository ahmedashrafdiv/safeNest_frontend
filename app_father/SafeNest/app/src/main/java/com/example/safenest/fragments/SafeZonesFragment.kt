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
import com.example.safenest.network.ZoneResponse
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.SafeZonesViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class SafeZonesFragment : Fragment() {

    companion object {
        private const val TAG = "SafeZonesFragment"
    }

    private val viewModel: SafeZonesViewModel by viewModels()

    private lateinit var bottomNavBar: BottomNavigationView
    private var progressBar: ProgressBar? = null
    private var emptyText: TextView? = null
    private var zonesContainer: LinearLayout? = null
    private var childId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_safe_zones, container, false)

        bottomNavBar = view.findViewById(R.id.bottomNavBar)
        progressBar = view.findViewById(R.id.progressBar)
        emptyText = view.findViewById(R.id.emptyText)
        zonesContainer = view.findViewById(R.id.zonesContainer)

        val prefs = requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
        childId = prefs.getString("child_id", null)

        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<MaterialButton>(R.id.addZoneBtn).setOnClickListener {
            (activity as MainActivity).navigateToFragment(AddZoneFragment())
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
                viewModel.zonesState.collect { state ->
                    when (state) {
                        is Result.Loading -> {
                            progressBar?.visibility = View.VISIBLE
                            emptyText?.visibility = View.GONE
                            zonesContainer?.removeAllViews()
                        }
                        is Result.Success -> {
                            progressBar?.visibility = View.GONE
                            val zones = state.data
                            if (zones.isEmpty()) {
                                emptyText?.text = getString(R.string.no_zones)
                                emptyText?.visibility = View.VISIBLE
                            } else {
                                emptyText?.visibility = View.GONE
                                zones.forEach { zone -> addZoneCard(zone) }
                            }
                            viewModel.clearZonesState()
                        }
                        is Result.Error -> {
                            progressBar?.visibility = View.GONE
                            emptyText?.text = getString(R.string.error_loading_zones)
                            emptyText?.visibility = View.VISIBLE
                            viewModel.clearZonesState()
                        }
                        null -> Unit
                    }
                }
            }
        }

        val cid = childId
        if (cid != null) {
            viewModel.getChildZones(cid)
        } else {
            emptyText?.text = getString(R.string.error_no_child)
            emptyText?.visibility = View.VISIBLE
        }
    }

    private fun addZoneCard(zone: ZoneResponse) {
        val ctx = requireContext()

        val card = MaterialCardView(ctx).apply {
            radius = 48f
            cardElevation = 8f
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(0, 0, 0, 16) }
            setCardBackgroundColor(android.graphics.Color.WHITE)
            strokeColor = if (zone.zoneType == "Safe")
                android.graphics.Color.parseColor("#16A22B")
            else
                android.graphics.Color.parseColor("#E15151")
            strokeWidth = 4
        }

        val inner = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 32, 48, 32)
        }

        val zoneTypeColor = if (zone.zoneType == "Safe")
            android.graphics.Color.parseColor("#16A22B")
        else
            android.graphics.Color.parseColor("#E15151")

        val nameTv = TextView(ctx).apply {
            text = zone.name
            textSize = 16f
            setTextColor(android.graphics.Color.parseColor("#692AC8"))
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val typeTv = TextView(ctx).apply {
            text = getString(R.string.zone_type_format, zone.zoneType)
            textSize = 13f
            setTextColor(zoneTypeColor)
            setPadding(0, 4, 0, 0)
        }

        val radiusTv = TextView(ctx).apply {
            text = getString(R.string.zone_radius_format, zone.radiusMeters)
            textSize = 13f
            setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            setPadding(0, 4, 0, 0)
        }

        val coordsTv = TextView(ctx).apply {
            text = getString(R.string.zone_coords_format, zone.latitude, zone.longitude)
            textSize = 11f
            setTextColor(android.graphics.Color.GRAY)
            setPadding(0, 4, 0, 0)
        }

        val deleteBtn = MaterialButton(ctx).apply {
            text = getString(R.string.delete_zone)
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
            setOnClickListener { confirmDeleteZone(zone, card) }
        }

        inner.addView(nameTv)
        inner.addView(typeTv)
        inner.addView(radiusTv)
        inner.addView(coordsTv)
        inner.addView(deleteBtn)
        card.addView(inner)
        zonesContainer?.addView(card)
    }

    private fun confirmDeleteZone(zone: ZoneResponse, card: MaterialCardView) {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_zone_confirm_title))
            .setMessage(getString(R.string.delete_zone_confirm_message, zone.name))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                zonesContainer?.removeView(card)
                if (zonesContainer?.childCount == 0) {
                    emptyText?.text = getString(R.string.no_zones)
                    emptyText?.visibility = View.VISIBLE
                }
                viewModel.deleteZone(zone.zoneId)
                Toast.makeText(context, getString(R.string.zone_deleted), Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }
}

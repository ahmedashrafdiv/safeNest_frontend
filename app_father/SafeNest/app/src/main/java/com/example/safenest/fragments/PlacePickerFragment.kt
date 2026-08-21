package com.example.safenest.fragments

import android.location.Geocoder
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

/** Map picker intentionally keeps the selected coordinate internal and never renders it as text. */
class PlacePickerFragment : Fragment(), OnMapReadyCallback {
    private val placeType get() = requireArguments().getString(ARG_TYPE) ?: "safe"
    private var point = LatLng(30.0444, 31.2357)
    private var map: GoogleMap? = null
    private lateinit var nameInput: EditText
    private lateinit var searchInput: EditText
    private lateinit var notice: TextView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        val root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(24.dp, 20.dp, 24.dp, 24.dp); setBackgroundColor(resources.getColor(R.color.ivory_surface, null)) }
        scroll.addView(root)
        root.addView(MaterialButton(requireContext()).apply { text = "→"; textSize = 22f; setTextColor(resources.getColor(R.color.navy_brand, null)); setOnClickListener { parentFragmentManager.popBackStack() } })
        root.addView(TextView(requireContext()).apply { text = "حدد المكان"; textSize = 20f; typeface = resources.getFont(R.font.lemonada_bold); setTextColor(resources.getColor(R.color.navy_brand, null)) })
        searchInput = EditText(requireContext()).apply { hint = "ابحث عن مكان أو عنوان"; setSingleLine(true); setBackgroundColor(resources.getColor(R.color.white, null)); setPadding(14.dp, 12.dp, 14.dp, 12.dp) }
        root.addView(searchInput, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp).apply { topMargin = 12.dp })
        root.addView(MaterialButton(requireContext()).apply { text = "بحث"; setTextColor(resources.getColor(R.color.navy_brand, null)); setOnClickListener { search() } })
        val mapContainer = FrameLayout(requireContext()).apply { id = View.generateViewId(); setBackgroundColor(resources.getColor(R.color.mint_surface, null)) }
        root.addView(mapContainer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 260.dp).apply { topMargin = 8.dp })
        nameInput = EditText(requireContext()).apply { hint = "اسم المكان"; setText("المنزل"); setSingleLine(true); setBackgroundColor(resources.getColor(R.color.white, null)); setPadding(14.dp, 12.dp, 14.dp, 12.dp) }
        root.addView(nameInput, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp).apply { topMargin = 16.dp })
        notice = TextView(requireContext()).apply { text = "يمكنك البحث أو تحريك المؤشر لتحديد المكان."; textSize = 12f; setTextColor(resources.getColor(R.color.navy_brand, null)); gravity = Gravity.CENTER }
        root.addView(notice)
        root.addView(MaterialButton(requireContext()).apply { text = "تأكيد هذا المكان"; setTextColor(resources.getColor(R.color.white, null)); setBackgroundColor(resources.getColor(R.color.teal_brand, null)); setOnClickListener { (activity as MainActivity).navigateToFragment(PlaceSettingsFragment.newCreate(placeType, nameInput.text.toString().trim().ifBlank { "مكان محفوظ" }, point.latitude, point.longitude)) } }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp).apply { topMargin = 16.dp })
        childFragmentManager.beginTransaction().replace(mapContainer.id, SupportMapFragment.newInstance()).commit()
        childFragmentManager.executePendingTransactions()
        (childFragmentManager.findFragmentById(mapContainer.id) as? SupportMapFragment)?.getMapAsync(this)
        return scroll
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        runCatching { googleMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(requireContext(), R.raw.location_map_calm)) }
        googleMap.uiSettings.isMapToolbarEnabled = false
        googleMap.setOnMapClickListener { newPoint -> point = newPoint; renderMarker() }
        renderMarker()
    }

    private fun renderMarker() { map?.let { it.clear(); it.addMarker(MarkerOptions().position(point).draggable(true).title("المكان المختار")); it.moveCamera(CameraUpdateFactory.newLatLngZoom(point, 14.5f)) } }
    private fun search() {
        val query = searchInput.text.toString().trim(); if (query.isBlank()) return
        notice.text = "جاري البحث عن المكان…"
        viewLifecycleOwner.lifecycleScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { Geocoder(requireContext(), Locale("ar")).getFromLocationName(query, 1)?.firstOrNull() }.getOrNull() }
            if (result == null) notice.text = "لم نجد نتيجة واضحة. يمكنك تحريك المؤشر يدويًا."
            else { point = LatLng(result.latitude, result.longitude); renderMarker(); notice.text = "تم اختيار مكان قريب من نتيجة البحث." }
        }
    }
    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
    companion object { private const val ARG_TYPE = "place_type"; fun newInstance(type: String) = PlacePickerFragment().apply { arguments = Bundle().apply { putString(ARG_TYPE, type) } } }
}

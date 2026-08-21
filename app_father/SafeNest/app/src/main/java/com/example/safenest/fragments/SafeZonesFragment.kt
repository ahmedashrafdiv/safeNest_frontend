package com.example.safenest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.network.ChildPlaceResponse
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.PlaceViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/** Grouped parent place management, deliberately free of coordinates and route history. */
class SafeZonesFragment : Fragment() {
    private val viewModel: PlaceViewModel by viewModels()
    private var childId: String? = null
    private lateinit var countText: TextView
    private lateinit var emptyText: TextView
    private lateinit var container: LinearLayout
    private lateinit var progress: ProgressBar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_safe_zones, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        countText = view.findViewById(R.id.places_count_text)
        emptyText = view.findViewById(R.id.emptyText)
        container = view.findViewById(R.id.placesContainer)
        progress = view.findViewById(R.id.progressBar)
        childId = requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE).getString("child_id", null)
        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<MaterialButton>(R.id.addZoneBtn).setOnClickListener { (activity as MainActivity).navigateToFragment(AddZoneFragment()) }
        setupBottomNav(view.findViewById(R.id.bottomNavBar))
        observe()
        childId?.let(viewModel::load) ?: renderEmpty("اختر ملف الطفل أولاً لإدارة الأماكن.")
    }

    override fun onResume() {
        super.onResume()
        childId?.let(viewModel::load)
    }

    private fun observe() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.placesState.collect { state ->
                when (state) {
                    is Result.Loading -> { progress.visibility = View.VISIBLE; emptyText.visibility = View.GONE }
                    is Result.Success -> { progress.visibility = View.GONE; renderPlaces(state.data); viewModel.clearPlaces() }
                    is Result.Error -> { progress.visibility = View.GONE; renderEmpty(state.message); viewModel.clearPlaces() }
                    null -> Unit
                }
            }
        }
    }

    private fun renderPlaces(places: List<ChildPlaceResponse>) {
        container.removeAllViews()
        countText.text = "${places.size} أماكن محفوظة"
        if (places.isEmpty()) {
            renderEmpty("لم تضف أماكن بعد. أضف مكانًا للاطمئنان.")
            return
        }
        emptyText.visibility = View.GONE
        section("الأماكن الآمنة", places.filter { it.placeType == "safe" })
        section("أماكن تحتاج انتباهًا", places.filter { it.placeType == "attention" })
        section("مناطق الخطر", places.filter { it.placeType == "risk" })
    }

    private fun section(title: String, places: List<ChildPlaceResponse>) {
        if (places.isEmpty()) return
        container.addView(TextView(requireContext()).apply {
            text = title
            textSize = 14f
            setTextColor(resources.getColor(R.color.navy_brand, null))
            typeface = resources.getFont(R.font.lemonada_bold)
            setPadding(0, 12, 0, 8)
        })
        places.forEach { container.addView(placeCard(it)) }
    }

    private fun placeCard(place: ChildPlaceResponse): MaterialCardView {
        val isRisk = place.placeType == "risk"
        val label = when (place.placeType) { "safe" -> "مكان آمن"; "attention" -> "يحتاج انتباهًا"; else -> "منطقة خطر" }
        val icon = when (place.placeType) { "safe" -> "⌂"; "attention" -> "◉"; else -> "△" }
        val detail = when {
            place.notifyOnExit -> "الوصول والمغادرة"
            place.notifyOnEnter -> "تنبيه عند الدخول"
            else -> "التنبيهات متوقفة"
        }
        return MaterialCardView(requireContext()).apply {
            radius = 16f
            cardElevation = 0f
            setCardBackgroundColor(resources.getColor(R.color.white, null))
            strokeColor = resources.getColor(if (isRisk) R.color.coral_action else R.color.mint_surface, null)
            strokeWidth = 1
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { bottomMargin = 8 }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                setPadding(14, 14, 14, 14)
                addView(TextView(context).apply { text = icon; textSize = 22f; setTextColor(resources.getColor(if (isRisk) R.color.coral_action else R.color.teal_brand, null)) })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 12 }
                    addView(TextView(context).apply { text = place.name; textSize = 15f; setTextColor(resources.getColor(R.color.navy_brand, null)); typeface = resources.getFont(R.font.lemonada_bold) })
                    addView(TextView(context).apply { text = "$label • نطاق ${place.radiusMeters} م • $detail"; textSize = 11f; setTextColor(resources.getColor(R.color.navy_brand, null)) })
                })
                addView(MaterialButton(context).apply {
                    text = "✎"
                    textSize = 18f
                    minWidth = 48
                    minHeight = 48
                    setTextColor(resources.getColor(R.color.navy_brand, null))
                    setOnClickListener { (activity as MainActivity).navigateToFragment(AddZoneFragment.newEdit(place)) }
                })
            })
        }
    }

    private fun renderEmpty(message: String) {
        container.removeAllViews()
        emptyText.text = message
        emptyText.visibility = View.VISIBLE
        countText.text = "0 أماكن محفوظة"
    }

    private fun setupBottomNav(nav: BottomNavigationView) {
        nav.selectedItemId = R.id.nav_gps
        nav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { (activity as MainActivity).navigateToFragment(HomeFragment()); true }
                R.id.nav_monitoring -> { (activity as MainActivity).navigateToFragment(MonitoringFragment()); true }
                R.id.nav_gps -> { (activity as MainActivity).navigateToFragment(GpsFragment()); true }
                R.id.nav_sensors -> { (activity as MainActivity).navigateToFragment(SensorsFragment()); true }
                R.id.nav_more -> { (activity as MainActivity).navigateToFragment(MoreFragment()); true }
                else -> false
            }
        }
    }
}

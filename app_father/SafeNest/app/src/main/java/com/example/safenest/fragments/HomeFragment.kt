package com.example.safenest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.HomeViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    companion object {
        private const val TAG = "HomeFragment"
    }

    private val viewModel: HomeViewModel by viewModels()

    private lateinit var bottomNavBar: BottomNavigationView
    private val avatarDrawables = listOf(
        R.drawable.ch1, R.drawable.ch2, R.drawable.ch3,
        R.drawable.ch4, R.drawable.ch5, R.drawable.ch6
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_home, container, false)

        val childNameText = view.findViewById<TextView>(R.id.childName)
        val headerAvatarImage = view.findViewById<ShapeableImageView>(R.id.headerAvatar)

        // Show cached child name immediately for instant UI
        val prefs = requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
        val cachedName = prefs.getString("child_name", null)
        val cachedAvatarIndex = prefs.getInt("child_avatar_index", 3)
        if (cachedName != null) {
            childNameText.text = cachedName
            if (cachedAvatarIndex in avatarDrawables.indices) {
                headerAvatarImage.setImageResource(avatarDrawables[cachedAvatarIndex])
            }
        }

        bottomNavBar = view.findViewById(R.id.bottomNavBar)
        bottomNavBar.selectedItemId = R.id.nav_home

        bottomNavBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> true
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

        val childNameText = view.findViewById<TextView>(R.id.childName)
        val headerAvatarImage = view.findViewById<ShapeableImageView>(R.id.headerAvatar)
        val prefs = requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Children list state
                launch {
                    viewModel.childrenState.collect { state ->
                        when (state) {
                            is Result.Success -> {
                                val children = state.data
                                if (children.isEmpty()) {
                                    (activity as MainActivity).navigateToFragment(AddChildFragment())
                                } else {
                                    val firstChild = children[0]
                                    childNameText.text = firstChild.name

                                    // Persist selected child via ViewModel (SessionManager)
                                    viewModel.saveSelectedChildId(firstChild.childId)

                                    // Also cache name/gender in prefs for instant display
                                    prefs.edit().apply {
                                        putString("child_id", firstChild.childId)
                                        putString("child_name", firstChild.name)
                                        putString("child_gender", firstChild.gender)
                                        apply()
                                    }

                                    val avatarIndex = prefs.getInt("child_avatar_index", 3)
                                    if (avatarIndex in avatarDrawables.indices) {
                                        headerAvatarImage.setImageResource(avatarDrawables[avatarIndex])
                                    }

                                    // Load digital rule for charts
                                    viewModel.getDigitalRule(firstChild.childId)
                                }
                                viewModel.clearChildrenState()
                            }
                            is Result.Error -> {
                                // Keep showing cached data on failure
                                viewModel.clearChildrenState()
                            }
                            else -> Unit
                        }
                    }
                }

                // Digital rule state (for charts)
                launch {
                    viewModel.digitalRuleState.collect { state ->
                        when (state) {
                            is Result.Success -> {
                                val rule = state.data
                                val usageLog = rule.dailyUsageLog ?: emptyMap()
                                if (usageLog.isEmpty()) {
                                    fallbackCharts(view)
                                } else {
                                    updateCharts(rule, usageLog, view, prefs)
                                }
                                viewModel.clearDigitalRuleState()
                            }
                            is Result.Error -> {
                                fallbackCharts(view)
                                viewModel.clearDigitalRuleState()
                            }
                            else -> Unit
                        }
                    }
                }
            }
        }

        // Pre-load charts from cached child while API loads
        val cachedChildId = viewModel.getSelectedChildId()
        if (cachedChildId != null) viewModel.getDigitalRule(cachedChildId)

        viewModel.getChildren()
    }

    private fun fallbackCharts(view: View) {
        val bars = listOf(
            R.id.barMon, R.id.barTue, R.id.barWed, R.id.barThu, R.id.barFri, R.id.barSat, R.id.barSun,
            R.id.catGames, R.id.catSocial, R.id.catYoutube, R.id.catAllowed, R.id.catOther
        )
        bars.forEach { id ->
            val v = view.findViewById<View>(id)
            if (v != null) { val p = v.layoutParams; p.height = 0; v.layoutParams = p }
        }
    }

    private fun updateCharts(
        rule: com.example.safenest.network.DigitalRuleResponse,
        usageLog: Map<String, Int>,
        view: View,
        prefs: android.content.SharedPreferences
    ) {
        val maxMinutes = rule.maxScreenTime ?: 0
        val todayMinutes = usageLog.values.sum()
        val ratio = if (maxMinutes > 0) todayMinutes.toFloat() / maxMinutes else 0f

        // Update usage-time chip: "X ساعة Y دقيقة من Z ساعة"
        val todayH = todayMinutes / 60; val todayM = todayMinutes % 60
        val maxH = maxMinutes / 60; val maxM = maxMinutes % 60
        val todayStr = if (todayH == 0) "${todayM}د" else if (todayM == 0) "${todayH}س" else "${todayH}س ${todayM}د"
        val maxStr = if (maxH == 0) "${maxM}د" else if (maxM == 0) "${maxH}س" else "${maxH}س ${maxM}د"
        view.findViewById<android.widget.TextView>(R.id.usageTime)?.text = "$todayStr من $maxStr"

        val scale = view.resources.displayMetrics.density
        val maxHeightPx = (150f * scale + 0.5f).toInt()
        val heightPx = (ratio * maxHeightPx).toInt().coerceAtLeast(0)

        val today = (java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) + 5) % 7
        val bars = listOf(R.id.barMon, R.id.barTue, R.id.barWed, R.id.barThu, R.id.barFri, R.id.barSat, R.id.barSun)
        bars.forEachIndexed { index, id ->
            val v = view.findViewById<View>(id) ?: return@forEachIndexed
            val p = v.layoutParams
            if (index == today) { p.height = heightPx; v.setBackgroundResource(R.drawable.bar_chart_active) }
            else { p.height = 0; v.setBackgroundResource(R.drawable.bar_chart_rounded) }
            v.layoutParams = p
        }
        val hoursLabel = if (todayH == 0) "${todayM} دقيقة" else if (todayM == 0) "${todayH} ساعة" else "${todayH}س ${todayM}د"
        view.findViewById<android.widget.TextView>(R.id.chartHoursLabel)?.text = hoursLabel


        var gamesMin = 0; var socialMin = 0; var youtubeMin = 0; var allowedMin = 0; var otherMin = 0
        val json = prefs.getString("allowed_apps_${rule.childId}", null)
        val allowedAppsList = mutableListOf<String>()
        if (json != null) {
            try {
                val type = object : com.google.gson.reflect.TypeToken<List<com.example.safenest.network.AllowedAppItem>>() {}.type
                val list: List<com.example.safenest.network.AllowedAppItem> = com.google.gson.Gson().fromJson(json, type)
                allowedAppsList.addAll(list.map { it.name.lowercase() })
            } catch (e: Exception) {}
        }
        usageLog.forEach { (pkg, mins) ->
            val p = pkg.lowercase()
            when {
                p.contains("game") || p.contains("pubg") || p.contains("fortnite") || p.contains("clash") || p.contains("minecraft") -> gamesMin += mins
                p.contains("facebook") || p.contains("instagram") || p.contains("twitter") || p.contains("tiktok") || p.contains("snapchat") || p.contains("whatsapp") -> socialMin += mins
                p.contains("youtube") || p.contains("yt") -> youtubeMin += mins
                allowedAppsList.any { p.contains(it) } -> allowedMin += mins
                else -> otherMin += mins
            }
        }
        val catBars = listOf(
            Pair(R.id.catGames, gamesMin), Pair(R.id.catSocial, socialMin),
            Pair(R.id.catYoutube, youtubeMin), Pair(R.id.catAllowed, allowedMin), Pair(R.id.catOther, otherMin)
        )
        val maxCat = catBars.maxOfOrNull { it.second } ?: 1
        val catScale = if (maxCat > 0) maxHeightPx.toFloat() / maxCat else 0f
        catBars.forEach { (id, mins) ->
            val v = view.findViewById<View>(id) ?: return@forEach
            val p = v.layoutParams; p.height = (mins * catScale).toInt(); v.layoutParams = p
        }
    }
}

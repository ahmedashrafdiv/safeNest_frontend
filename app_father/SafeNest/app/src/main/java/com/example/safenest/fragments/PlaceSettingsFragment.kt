package com.example.safenest.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.network.ChildPlaceResponse
import com.example.safenest.network.PlaceCreateRequest
import com.example.safenest.network.PlaceUpdateRequest
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.PlaceViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.materialswitch.MaterialSwitch
import kotlinx.coroutines.launch

/** Type-constrained place settings. Attention and risk intentionally expose entry notification only. */
class PlaceSettingsFragment : Fragment() {
    private val viewModel: PlaceViewModel by viewModels()
    private val type get() = requireArguments().getString(ARG_TYPE) ?: "safe"
    private val isEdit get() = requireArguments().getString(ARG_ID) != null
    private var radius = 200
    private lateinit var nameInput: EditText
    private lateinit var enterSwitch: MaterialSwitch
    private var exitSwitch: MaterialSwitch? = null
    private lateinit var saveButton: MaterialButton
    private lateinit var errorText: TextView
    private lateinit var progress: ProgressBar
    private val radiusButtons = mutableMapOf<Int, MaterialButton>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        radius = requireArguments().getInt(ARG_RADIUS, defaultRadius(type))
        val scroll = ScrollView(requireContext())
        val root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; setPadding(24.dp, 20.dp, 24.dp, 24.dp); setBackgroundColor(resources.getColor(R.color.ivory_surface, null)) }
        scroll.addView(root)
        root.addView(MaterialButton(requireContext()).apply { text = "→"; textSize = 22f; setTextColor(resources.getColor(R.color.navy_brand, null)); setOnClickListener { parentFragmentManager.popBackStack() } })
        root.addView(TextView(requireContext()).apply { text = "إعدادات ${typeLabel(type)}"; textSize = 20f; typeface = resources.getFont(R.font.lemonada_bold); setTextColor(resources.getColor(R.color.navy_brand, null)) })
        root.addView(TextView(requireContext()).apply { text = if (type == "safe") "مكان متوقع ومألوف." else "سيصلك تنبيه هادئ عند دخول هذا المكان."; textSize = 12f; setTextColor(resources.getColor(R.color.navy_brand, null)); setPadding(0, 4.dp, 0, 12.dp) })
        nameInput = EditText(requireContext()).apply { hint = "اسم المكان"; setSingleLine(true); setText(requireArguments().getString(ARG_NAME, "")); setBackgroundColor(resources.getColor(R.color.white, null)); setPadding(14.dp, 12.dp, 14.dp, 12.dp) }
        root.addView(nameInput, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp))
        root.addView(TextView(requireContext()).apply { text = "نطاق التنبيه"; textSize = 15f; typeface = resources.getFont(R.font.lemonada_bold); setTextColor(resources.getColor(R.color.navy_brand, null)); setPadding(0, 20.dp, 0, 8.dp) })
        val ranges = LinearLayout(requireContext()).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        listOf(100, 200, 300).forEach { value ->
            val button = MaterialButton(requireContext()).apply {
                text = "$value م"; minHeight = 44.dp; minWidth = 0; layoutParams = LinearLayout.LayoutParams(0, 44.dp, 1f).apply { marginEnd = if (value == 300) 0 else 8.dp }; setOnClickListener { selectRadius(value) }
            }
            radiusButtons[value] = button; ranges.addView(button)
        }
        root.addView(ranges)
        root.addView(TextView(requireContext()).apply { text = "التنبيهات"; textSize = 15f; typeface = resources.getFont(R.font.lemonada_bold); setTextColor(resources.getColor(R.color.navy_brand, null)); setPadding(0, 20.dp, 0, 4.dp) })
        enterSwitch = MaterialSwitch(requireContext()).apply { text = if (type == "safe") "عند الوصول" else "عند الدخول"; isChecked = requireArguments().getBoolean(ARG_ENTER, true); setTextColor(resources.getColor(R.color.navy_brand, null)); minHeight = 52.dp }
        root.addView(enterSwitch)
        if (type == "safe") {
            exitSwitch = MaterialSwitch(requireContext()).apply { text = "عند المغادرة"; isChecked = requireArguments().getBoolean(ARG_EXIT, false); setTextColor(resources.getColor(R.color.navy_brand, null)); minHeight = 52.dp }
            root.addView(exitSwitch)
        }
        errorText = TextView(requireContext()).apply { setTextColor(resources.getColor(R.color.coral_action, null)); textSize = 12f; gravity = Gravity.CENTER; visibility = View.GONE; setPadding(0, 8.dp, 0, 0) }
        root.addView(errorText)
        progress = ProgressBar(requireContext()).apply { visibility = View.GONE }
        root.addView(progress, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { gravity = Gravity.CENTER })
        saveButton = MaterialButton(requireContext()).apply { text = "حفظ المكان"; setTextColor(resources.getColor(R.color.white, null)); setBackgroundColor(resources.getColor(R.color.teal_brand, null)); setOnClickListener { save() } }
        root.addView(saveButton, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp).apply { topMargin = 16.dp })
        selectRadius(radius)
        observe()
        return scroll
    }

    private fun observe() = viewLifecycleOwner.lifecycleScope.launch {
        viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.mutationState.collect { state ->
                when (state) {
                    is Result.Loading -> setSaving(true)
                    is Result.Success -> { setSaving(false); viewModel.clearMutation(); (activity as MainActivity).navigateToFragment(PlaceSavedFragment.newInstance(state.data.name, state.data.placeType)) }
                    is Result.Error -> { setSaving(false); errorText.text = state.message; errorText.visibility = View.VISIBLE; viewModel.clearMutation() }
                    null -> Unit
                }
            }
        }
    }

    private fun save() {
        val childId = requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE).getString("child_id", null)
        val name = nameInput.text.toString().trim()
        if (childId.isNullOrBlank()) { errorText.text = "اختر ملف الطفل أولاً."; errorText.visibility = View.VISIBLE; return }
        if (name.isBlank()) { errorText.text = "اكتب اسمًا للمكان."; errorText.visibility = View.VISIBLE; return }
        errorText.visibility = View.GONE
        val enter = enterSwitch.isChecked
        val exit = exitSwitch?.isChecked ?: false
        if (isEdit) {
            viewModel.update(childId, requireArguments().getString(ARG_ID)!!, PlaceUpdateRequest(name = name, placeType = type, latitude = requireArguments().getDouble(ARG_LAT), longitude = requireArguments().getDouble(ARG_LON), radiusMeters = radius, notifyOnEnter = enter, notifyOnExit = exit))
        } else {
            viewModel.create(childId, PlaceCreateRequest(name, type, childId, requireArguments().getDouble(ARG_LAT), requireArguments().getDouble(ARG_LON), radius, enter, exit))
        }
    }

    private fun selectRadius(value: Int) {
        radius = value
        radiusButtons.forEach { (candidate, button) ->
            button.setTextColor(resources.getColor(if (candidate == value) R.color.white else R.color.navy_brand, null))
            button.setBackgroundColor(resources.getColor(if (candidate == value) R.color.teal_brand else R.color.mint_surface, null))
        }
    }
    private fun setSaving(saving: Boolean) { progress.visibility = if (saving) View.VISIBLE else View.GONE; saveButton.isEnabled = !saving }
    private fun typeLabel(value: String) = when (value) { "safe" -> "المكان الآمن"; "attention" -> "المكان الذي يحتاج انتباهًا"; else -> "منطقة الخطر" }
    private fun defaultRadius(value: String) = if (value == "risk") 300 else 200
    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
    companion object {
        private const val ARG_TYPE = "type"; private const val ARG_NAME = "name"; private const val ARG_LAT = "lat"; private const val ARG_LON = "lon"; private const val ARG_RADIUS = "radius"; private const val ARG_ENTER = "enter"; private const val ARG_EXIT = "exit"; private const val ARG_ID = "place_id"
        fun newCreate(type: String, name: String, lat: Double, lon: Double) = PlaceSettingsFragment().apply { arguments = Bundle().apply { putString(ARG_TYPE, type); putString(ARG_NAME, name); putDouble(ARG_LAT, lat); putDouble(ARG_LON, lon); putInt(ARG_RADIUS, if (type == "risk") 300 else 200); putBoolean(ARG_ENTER, true); putBoolean(ARG_EXIT, false) } }
        fun newEdit(place: ChildPlaceResponse) = PlaceSettingsFragment().apply { arguments = Bundle().apply { putString(ARG_ID, place.placeId); putString(ARG_TYPE, place.placeType); putString(ARG_NAME, place.name); putDouble(ARG_LAT, place.latitude); putDouble(ARG_LON, place.longitude); putInt(ARG_RADIUS, place.radiusMeters); putBoolean(ARG_ENTER, place.notifyOnEnter); putBoolean(ARG_EXIT, place.notifyOnExit) } }
    }
}

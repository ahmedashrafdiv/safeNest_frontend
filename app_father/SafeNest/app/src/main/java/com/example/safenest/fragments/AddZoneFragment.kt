package com.example.safenest.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.network.ChildPlaceResponse
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView

/** First place-creation decision: choose the meaning before the map or settings. */
class AddZoneFragment : Fragment() {
    private var selectedType: String? = null
    private val options = mutableMapOf<String, MaterialCardView>()
    private lateinit var continueButton: MaterialButton

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val scroll = ScrollView(requireContext())
        val root = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24.dp, 20.dp, 24.dp, 24.dp)
            setBackgroundColor(resources.getColor(R.color.ivory_surface, null))
        }
        scroll.addView(root)
        root.addView(backButton())
        root.addView(title("ما نوع هذا المكان؟", 20f))
        root.addView(subtitle("اختر المعنى المناسب لنرسل التنبيه الصحيح."))
        root.addView(typeCard("safe", "⌂", "مكان آمن", "مكان متوقع مثل المنزل أو المدرسة."))
        root.addView(typeCard("attention", "◉", "مكان يحتاج انتباهًا", "يخبرك بهدوء عند دخول ليان إليه."))
        root.addView(typeCard("risk", "△", "منطقة خطر", "ينبهك بوضوح عند دخول ليان إليها.", true))
        root.addView(subtitle("يمكنك تعديل النوع والتنبيهات لاحقًا.").apply { setPadding(0, 12.dp, 0, 8.dp) })
        continueButton = MaterialButton(requireContext()).apply {
            text = "متابعة"
            isEnabled = false
            setTextColor(resources.getColor(R.color.white, null))
            setBackgroundColor(resources.getColor(R.color.teal_brand, null))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp).apply { topMargin = 8.dp }
            setOnClickListener { selectedType?.let { (activity as MainActivity).navigateToFragment(PlacePickerFragment.newInstance(it)) } }
        }
        root.addView(continueButton)
        return scroll
    }

    private fun typeCard(type: String, icon: String, label: String, copy: String, risk: Boolean = false): MaterialCardView {
        return MaterialCardView(requireContext()).apply {
            radius = 16f
            cardElevation = 0f
            strokeWidth = 1
            strokeColor = resources.getColor(R.color.mint_surface, null)
            setCardBackgroundColor(resources.getColor(R.color.white, null))
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply { topMargin = 12.dp }
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(16.dp, 16.dp, 16.dp, 16.dp)
                addView(TextView(context).apply { text = icon; textSize = 24f; setTextColor(resources.getColor(if (risk) R.color.coral_action else R.color.teal_brand, null)) })
                addView(LinearLayout(context).apply {
                    orientation = LinearLayout.VERTICAL
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = 12.dp }
                    addView(title(label, 15f))
                    addView(subtitle(copy))
                })
            })
            setOnClickListener { select(type) }
            options[type] = this
        }
    }

    private fun select(type: String) {
        selectedType = type
        options.forEach { (key, card) ->
            card.strokeWidth = if (key == type) 3 else 1
            card.strokeColor = resources.getColor(if (key == type) R.color.teal_brand else R.color.mint_surface, null)
        }
        continueButton.isEnabled = true
    }

    private fun backButton() = MaterialButton(requireContext()).apply {
        text = "→"
        textSize = 22f
        setTextColor(resources.getColor(R.color.navy_brand, null))
        setOnClickListener { parentFragmentManager.popBackStack() }
    }

    private fun title(value: String, size: Float) = TextView(requireContext()).apply {
        text = value; textSize = size; setTextColor(resources.getColor(R.color.navy_brand, null)); typeface = resources.getFont(R.font.lemonada_bold)
    }

    private fun subtitle(value: String) = TextView(requireContext()).apply { text = value; textSize = 12f; setTextColor(resources.getColor(R.color.navy_brand, null)) }
    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()

    companion object {
        fun newEdit(place: ChildPlaceResponse): Fragment = PlaceSettingsFragment.newEdit(place)
    }
}

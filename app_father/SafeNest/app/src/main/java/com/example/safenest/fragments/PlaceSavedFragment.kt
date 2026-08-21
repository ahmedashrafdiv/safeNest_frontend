package com.example.safenest.fragments

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.google.android.material.button.MaterialButton

class PlaceSavedFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        val root = LinearLayout(requireContext()).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(30.dp, 30.dp, 30.dp, 30.dp); setBackgroundColor(resources.getColor(R.color.ivory_surface, null)) }
        root.addView(TextView(requireContext()).apply { text = "✓"; textSize = 54f; gravity = Gravity.CENTER; setTextColor(resources.getColor(R.color.teal_brand, null)) })
        root.addView(TextView(requireContext()).apply { text = "تم حفظ ${requireArguments().getString(ARG_NAME)}"; textSize = 20f; gravity = Gravity.CENTER; typeface = resources.getFont(R.font.lemonada_bold); setTextColor(resources.getColor(R.color.navy_brand, null)) })
        root.addView(TextView(requireContext()).apply { text = "ستصلك التنبيهات المفعلة لهذا ${typeLabel(requireArguments().getString(ARG_TYPE) ?: "safe")}."; textSize = 13f; gravity = Gravity.CENTER; setTextColor(resources.getColor(R.color.navy_brand, null)); setPadding(0, 8.dp, 0, 18.dp) })
        root.addView(MaterialButton(requireContext()).apply { text = "العودة إلى الأماكن"; setTextColor(resources.getColor(R.color.white, null)); setBackgroundColor(resources.getColor(R.color.teal_brand, null)); setOnClickListener { (activity as MainActivity).navigateToFragment(SafeZonesFragment()) } }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 52.dp))
        return root
    }
    private fun typeLabel(value: String) = when (value) { "safe" -> "مكان الآمن"; "attention" -> "المكان"; else -> "منطقة الخطر" }
    private val Int.dp get() = (this * resources.displayMetrics.density).toInt()
    companion object { private const val ARG_NAME = "name"; private const val ARG_TYPE = "type"; fun newInstance(name: String, type: String) = PlaceSavedFragment().apply { arguments = Bundle().apply { putString(ARG_NAME, name); putString(ARG_TYPE, type) } } }
}

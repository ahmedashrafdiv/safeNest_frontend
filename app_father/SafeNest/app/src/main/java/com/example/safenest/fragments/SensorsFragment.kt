package com.example.safenest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment

/**
 * Placeholder screen for the disabled Sensors destination.
 *
 * The IoT/sensor implementation is intentionally not part of the current
 * parental-safety runtime. This screen exists only to keep existing navigation
 * references buildable without touching IoT source files.
 */
class SensorsFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return TextView(requireContext()).apply {
            text = "Sensors are currently disabled."
            textSize = 18f
            setPadding(48, 48, 48, 48)
        }
    }
}

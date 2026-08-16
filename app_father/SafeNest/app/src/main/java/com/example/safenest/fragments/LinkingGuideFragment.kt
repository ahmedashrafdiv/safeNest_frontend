package com.example.safenest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.google.android.material.button.MaterialButton

class LinkingGuideFragment : Fragment() {
    
    private lateinit var backButton: MaterialButton
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_linking_guide, container, false)
        
        // Initialize views
        backButton = view.findViewById(R.id.backButton)
        
        // Set click listeners
        backButton.setOnClickListener {
            // Go back to sign in page
            (activity as MainActivity).navigateToFragment(LoginSignInFragment())
        }
        
        return view
    }
}

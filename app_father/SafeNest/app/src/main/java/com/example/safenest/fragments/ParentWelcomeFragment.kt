package com.example.safenest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.safenest.MainActivity
import com.example.safenest.R

/** First-launch Parent entry surface for registration or an existing account. */
class ParentWelcomeFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_parent_welcome, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<View>(R.id.createAccountButton).setOnClickListener {
            (activity as MainActivity).openRegistrationFromWelcome()
        }
        view.findViewById<View>(R.id.signInButton).setOnClickListener {
            (activity as MainActivity).openSignInFromWelcome()
        }
    }
}

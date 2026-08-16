package com.example.safenest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.google.android.material.card.MaterialCardView

class AddChildIntroFragment : Fragment() {

    private lateinit var addChildCard: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_child_intro, container, false)
        addChildCard = view.findViewById(R.id.addChildCard)

        addChildCard.setOnClickListener {
            (activity as MainActivity).navigateToFragment(AddChildFragment())
        }

        return view
    }
}

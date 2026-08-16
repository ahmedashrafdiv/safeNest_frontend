package com.example.safenest.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.example.safenest.network.ApiClient
import kotlinx.coroutines.launch

class MoreFragment : Fragment() {

    companion object {
        private const val TAG = "MoreFragment"
    }

    private lateinit var bottomNavBar: BottomNavigationView
    private lateinit var settingsCard: MaterialCardView
    private lateinit var notificationsCard: MaterialCardView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_more, container, false)

        // Initialize existing views
        bottomNavBar = view.findViewById(R.id.bottomNavBar)
        settingsCard = view.findViewById(R.id.settingsCard)
        notificationsCard = view.findViewById(R.id.notificationsCard)

        // Set More as selected in bottom nav
        bottomNavBar.selectedItemId = R.id.nav_more

        // Existing click listeners
        settingsCard.setOnClickListener {
            (activity as MainActivity).navigateToFragment(SettingsFragment())
        }

        notificationsCard.setOnClickListener {
            (activity as MainActivity).navigateToFragment(NotificationsFragment())
        }

        // Feature 1: Device Pairing
        view.findViewById<MaterialCardView?>(R.id.devicePairingCard)?.setOnClickListener {
            (activity as MainActivity).navigateToFragment(PairingFragment())
        }

        // Feature 1: My Devices
        view.findViewById<MaterialCardView?>(R.id.myDevicesCard)?.setOnClickListener {
            (activity as MainActivity).navigateToFragment(MyDevicesFragment())
        }

        // Feature 2: Child GPS Config
        view.findViewById<MaterialCardView?>(R.id.childGpsCard)?.setOnClickListener {
            (activity as MainActivity).navigateToFragment(ChildGpsFragment())
        }

        // Feature 3: Safe Zones
        view.findViewById<MaterialCardView?>(R.id.safeZonesCard)?.setOnClickListener {
            (activity as MainActivity).navigateToFragment(SafeZonesFragment())
        }

        // Feature 6: Edit Child Profile
        view.findViewById<MaterialCardView?>(R.id.editChildCard)?.setOnClickListener {
            (activity as MainActivity).navigateToFragment(EditChildFragment())
        }

        // Feature 6: Delete Child
        view.findViewById<MaterialCardView?>(R.id.deleteChildCard)?.setOnClickListener {
            confirmDeleteChild()
        }

        // Set navigation item click listener
        bottomNavBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    (activity as MainActivity).navigateToFragment(HomeFragment())
                    true
                }
                R.id.nav_monitoring -> {
                    (activity as MainActivity).navigateToFragment(MonitoringFragment())
                    true
                }
                R.id.nav_gps -> {
                    (activity as MainActivity).navigateToFragment(GpsFragment())
                    true
                }
                R.id.nav_sensors -> {
                    (activity as MainActivity).navigateToFragment(SensorsFragment())
                    true
                }
                R.id.nav_more -> {
                    // Already on More
                    true
                }
                else -> false
            }
        }

        return view
    }

    private fun confirmDeleteChild() {
        val prefs = requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
        val childName = prefs.getString("child_name", null) ?: getString(R.string.unknown_child)
        val childId = prefs.getString("child_id", null) ?: run {
            Toast.makeText(context, getString(R.string.error_no_child), Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.delete_child_title))
            .setMessage(getString(R.string.delete_child_message, childName))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                // Second confirmation
                AlertDialog.Builder(requireContext())
                    .setTitle(getString(R.string.delete_child_confirm_final_title))
                    .setMessage(getString(R.string.delete_child_confirm_final_message))
                    .setPositiveButton(getString(R.string.delete_permanently)) { _, _ ->
                        deleteChild(childId)
                    }
                    .setNegativeButton(getString(R.string.cancel), null)
                    .show()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun deleteChild(childId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Deleting child: $childId")
                val response = ApiClient.apiService.deleteChild(childId)

                if (response.isSuccessful) {
                    Log.d(TAG, "Child deleted successfully")
                    // Clear child-related data from SharedPreferences
                    requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
                        .edit().apply {
                            remove("child_id")
                            remove("child_name")
                            remove("child_gender")
                            remove("child_avatar_index")
                            remove("gps_id")
                            apply()
                        }
                    Toast.makeText(context, getString(R.string.child_deleted_success), Toast.LENGTH_SHORT).show()
                    // Navigate to add child screen since no child exists anymore
                    (activity as MainActivity).goToHome()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, getString(R.string.error_delete_child, ApiClient.parseError(errorBody)), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting child: ${e.message}", e)
                Toast.makeText(context, getString(R.string.error_network), Toast.LENGTH_SHORT).show()
            }
        }
    }
}

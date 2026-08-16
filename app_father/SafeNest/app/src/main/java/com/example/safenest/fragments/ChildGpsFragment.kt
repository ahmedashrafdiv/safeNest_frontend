package com.example.safenest.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.network.ApiClient
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

class ChildGpsFragment : Fragment() {

    companion object {
        private const val TAG = "ChildGpsFragment"
        private const val PREFS_NAME = "SafeNestPrefs"
    }

    private lateinit var bottomNavBar: BottomNavigationView
    private var progressBar: ProgressBar? = null
    private var noGpsGroup: LinearLayout? = null
    private var hasGpsGroup: LinearLayout? = null
    private var gpsStatusTv: TextView? = null

    private var childId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_child_gps, container, false)

        bottomNavBar = view.findViewById(R.id.bottomNavBar)
        progressBar = view.findViewById(R.id.progressBar)
        noGpsGroup = view.findViewById(R.id.noGpsGroup)
        hasGpsGroup = view.findViewById(R.id.hasGpsGroup)
        gpsStatusTv = view.findViewById(R.id.gpsStatusTv)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        childId = prefs.getString("child_id", null)

        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        view.findViewById<MaterialButton>(R.id.addGpsBtn).setOnClickListener {
            pairGps()
        }

        view.findViewById<MaterialButton>(R.id.updateGpsBtn).setOnClickListener {
            updateGpsLocation()
        }

        view.findViewById<MaterialButton>(R.id.removeGpsBtn).setOnClickListener {
            confirmRemoveGps()
        }

        bottomNavBar.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { (activity as MainActivity).navigateToFragment(HomeFragment()); true }
                R.id.nav_monitoring -> { (activity as MainActivity).navigateToFragment(MonitoringFragment()); true }
                R.id.nav_gps -> { (activity as MainActivity).navigateToFragment(GpsFragment()); true }
                R.id.nav_sensors -> { (activity as MainActivity).navigateToFragment(SensorsFragment()); true }
                R.id.nav_more -> { (activity as MainActivity).navigateToFragment(MoreFragment()); true }
                else -> false
            }
        }

        loadGpsState()
        return view
    }

    private fun loadGpsState() {
        val cid = childId ?: run {
            showNoGpsState()
            return
        }
        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        val isPaired = prefs.getBoolean("gps_paired_$cid", false)

        if (isPaired) {
            showHasGpsState()
        } else {
            showNoGpsState()
        }
    }

    private fun showNoGpsState() {
        noGpsGroup?.visibility = View.VISIBLE
        hasGpsGroup?.visibility = View.GONE
    }

    private fun showHasGpsState() {
        noGpsGroup?.visibility = View.GONE
        hasGpsGroup?.visibility = View.VISIBLE
        gpsStatusTv?.text = "جهاز GPS مرتبط ✓"
    }

    private fun pairGps() {
        val cid = childId ?: run {
            Toast.makeText(context, getString(R.string.error_no_child), Toast.LENGTH_SHORT).show()
            return
        }

        progressBar?.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Pairing GPS for child: $cid")
                val response = ApiClient.apiService.pairGps(cid)

                if (response.isSuccessful) {
                    Log.d(TAG, "GPS paired successfully")
                    requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                        .edit().putBoolean("gps_paired_$cid", true).apply()
                    showHasGpsState()
                    Toast.makeText(context, "تم ربط جهاز GPS بنجاح", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, ApiClient.parseError(errorBody), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error pairing GPS: ${e.message}", e)
                Toast.makeText(context, getString(R.string.error_network), Toast.LENGTH_SHORT).show()
            } finally {
                progressBar?.visibility = View.GONE
            }
        }
    }

    private fun updateGpsLocation() {
        val cid = childId ?: run {
            Toast.makeText(context, getString(R.string.error_no_child), Toast.LENGTH_SHORT).show()
            return
        }

        progressBar?.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Updating GPS from ThingSpeak for child: $cid")
                val response = ApiClient.apiService.updateGpsFromThingspeak(cid)

                if (response.isSuccessful) {
                    Toast.makeText(context, "تم تحديث الموقع", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, ApiClient.parseError(errorBody), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating GPS: ${e.message}", e)
                Toast.makeText(context, getString(R.string.error_network), Toast.LENGTH_SHORT).show()
            } finally {
                progressBar?.visibility = View.GONE
            }
        }
    }

    private fun confirmRemoveGps() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.remove_gps_title))
            .setMessage(getString(R.string.remove_gps_message))
            .setPositiveButton(getString(R.string.remove)) { _, _ ->
                removeGps()
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    private fun removeGps() {
        val cid = childId ?: return
        progressBar?.visibility = View.VISIBLE

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.deleteGps(cid)

                if (response.isSuccessful) {
                    Log.d(TAG, "GPS removed for child: $cid")
                    requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                        .edit().remove("gps_paired_$cid").apply()
                    showNoGpsState()
                    Toast.makeText(context, "تم إزالة الجهاز", Toast.LENGTH_SHORT).show()
                } else {
                    val errorBody = response.errorBody()?.string()
                    Toast.makeText(context, ApiClient.parseError(errorBody), Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error removing GPS: ${e.message}", e)
                Toast.makeText(context, getString(R.string.error_network), Toast.LENGTH_SHORT).show()
            } finally {
                progressBar?.visibility = View.GONE
            }
        }
    }
}

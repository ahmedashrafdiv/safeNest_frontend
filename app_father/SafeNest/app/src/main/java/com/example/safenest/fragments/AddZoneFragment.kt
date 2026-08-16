package com.example.safenest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.R
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.SafeZonesViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class AddZoneFragment : Fragment() {

    companion object {
        private const val TAG = "AddZoneFragment"
    }

    private val viewModel: SafeZonesViewModel by viewModels()

    private var zoneNameInput: TextInputEditText? = null
    private var zoneTypeInput: TextInputEditText? = null
    private var latitudeInput: TextInputEditText? = null
    private var longitudeInput: TextInputEditText? = null
    private var radiusInput: TextInputEditText? = null
    private var saveZoneBtn: MaterialButton? = null
    private var progressBar: ProgressBar? = null
    private var errorText: TextView? = null

    private var childId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_zone, container, false)

        zoneNameInput = view.findViewById(R.id.zoneNameInput)
        zoneTypeInput = view.findViewById(R.id.zoneTypeInput)
        latitudeInput = view.findViewById(R.id.latitudeInput)
        longitudeInput = view.findViewById(R.id.longitudeInput)
        radiusInput = view.findViewById(R.id.radiusInput)
        saveZoneBtn = view.findViewById(R.id.saveZoneBtn)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)

        val prefs = requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
        childId = prefs.getString("child_id", null)

        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        saveZoneBtn?.setOnClickListener {
            saveZoneBtn?.isEnabled = false
            saveZone()
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.createZoneState.collect { state ->
                    when (state) {
                        is Result.Loading -> setLoading(true)
                        is Result.Success -> {
                            setLoading(false)
                            Toast.makeText(context, getString(R.string.zone_created), Toast.LENGTH_SHORT).show()
                            parentFragmentManager.popBackStack()
                            viewModel.clearCreateZoneState()
                        }
                        is Result.Error -> {
                            setLoading(false)
                            showError(state.message)
                            viewModel.clearCreateZoneState()
                        }
                        null -> Unit
                    }
                }
            }
        }
    }

    private fun saveZone() {
        val name = zoneNameInput?.text.toString().trim()
        val zoneType = zoneTypeInput?.text.toString().trim()
        val latStr = latitudeInput?.text.toString().trim()
        val lngStr = longitudeInput?.text.toString().trim()
        val radiusStr = radiusInput?.text.toString().trim()
        val cid = this.childId

        if (name.isEmpty()) { showError(getString(R.string.error_zone_name_required)); return }
        if (zoneType.isEmpty() || (zoneType != "Safe" && zoneType != "Danger")) { showError(getString(R.string.error_zone_type_invalid)); return }
        val latitude = latStr.toDoubleOrNull() ?: run { showError(getString(R.string.error_latitude_invalid)); return }
        val longitude = lngStr.toDoubleOrNull() ?: run { showError(getString(R.string.error_longitude_invalid)); return }
        val radius = radiusStr.toIntOrNull()?.takeIf { it > 0 } ?: run { showError(getString(R.string.error_radius_invalid)); return }
        if (cid == null) { showError(getString(R.string.error_no_child)); return }

        hideError()
        viewModel.createZone(name, zoneType, cid, latitude, longitude, radius)
    }

    private fun showError(message: String) {
        saveZoneBtn?.isEnabled = true
        errorText?.text = message
        errorText?.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorText?.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        saveZoneBtn?.isEnabled = !loading
        saveZoneBtn?.text = if (loading) getString(R.string.saving) else getString(R.string.save_zone)
        progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
    }
}

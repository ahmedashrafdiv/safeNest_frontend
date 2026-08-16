package com.example.safenest.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.safenest.R
import com.example.safenest.network.ApiClient
import com.example.safenest.network.ChildUpdateRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class EditChildFragment : Fragment() {

    companion object {
        private const val TAG = "EditChildFragment"
        private const val PREFS_NAME = "SafeNestPrefs"
    }

    private var childNameInput: TextInputEditText? = null
    private var childGenderInput: TextInputEditText? = null
    private var childDobInput: TextInputEditText? = null
    private var saveChildBtn: MaterialButton? = null
    private var progressBar: ProgressBar? = null
    private var errorText: TextView? = null

    private var childId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_edit_child, container, false)

        childNameInput = view.findViewById(R.id.childNameInput)
        childGenderInput = view.findViewById(R.id.childGenderInput)
        childDobInput = view.findViewById(R.id.childDobInput)
        saveChildBtn = view.findViewById(R.id.saveChildBtn)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)

        val prefs = requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
        childId = prefs.getString("child_id", null)

        // Pre-fill current values from cache
        childNameInput?.setText(prefs.getString("child_name", ""))
        childGenderInput?.setText(prefs.getString("child_gender", ""))

        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        saveChildBtn?.setOnClickListener {
            saveChild()
        }

        // Load fresh data from API to pre-fill date of birth
        loadChildData()

        return view
    }

    private fun loadChildData() {
        val childId = this.childId ?: return

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getChild(childId)
                if (response.isSuccessful) {
                    val child = response.body()
                    if (child != null) {
                        childNameInput?.setText(child.name)
                        childGenderInput?.setText(child.gender)
                        childDobInput?.setText(child.dateOfBirth)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading child data: ${e.message}", e)
            }
        }
    }

    private fun saveChild() {
        val childId = this.childId ?: run {
            showError(getString(R.string.error_no_child))
            return
        }

        val name = childNameInput?.text.toString().trim()
        val gender = childGenderInput?.text.toString().trim()
        val dob = childDobInput?.text.toString().trim()

        if (name.isEmpty()) {
            showError(getString(R.string.error_child_name_required))
            return
        }

        val genderForApi = when {
            gender.contains("ذكر", ignoreCase = true) || gender.equals("male", ignoreCase = true) -> "Male"
            gender.contains("أنثى", ignoreCase = true) || gender.equals("female", ignoreCase = true) -> "Female"
            gender.isNotEmpty() -> gender
            else -> null
        }

        val dobForApi = if (dob.isNotEmpty()) dob else null

        hideError()
        setLoading(true)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d(TAG, "Updating child $childId: name=$name, gender=$genderForApi, dob=$dobForApi")
                val request = ChildUpdateRequest(
                    name = name,
                    gender = genderForApi,
                    dateOfBirth = dobForApi
                )
                val response = ApiClient.apiService.updateChild(childId, request)

                if (response.isSuccessful) {
                    val child = response.body()
                    Log.d(TAG, "Child updated: ${child?.name}")

                    // Update SharedPreferences cache
                    requireContext().getSharedPreferences(PREFS_NAME, android.content.Context.MODE_PRIVATE)
                        .edit().apply {
                            putString("child_name", child?.name ?: name)
                            putString("child_gender", child?.gender ?: genderForApi)
                            apply()
                        }

                    Toast.makeText(context, getString(R.string.child_updated_success), Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                } else {
                    val errorBody = response.errorBody()?.string()
                    val errorMsg = when (response.code()) {
                        401 -> getString(R.string.error_unauthorized)
                        422 -> ApiClient.parseError(errorBody)
                        else -> ApiClient.parseError(errorBody)
                    }
                    showError(errorMsg)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Network error updating child: ${e.message}", e)
                showError(getString(R.string.error_network))
            } finally {
                setLoading(false)
            }
        }
    }

    private fun showError(message: String) {
        errorText?.text = message
        errorText?.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorText?.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        saveChildBtn?.isEnabled = !loading
        saveChildBtn?.text = if (loading) getString(R.string.saving) else getString(R.string.save_changes)
        progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
    }
}

package com.example.safenest.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.network.ApiClient
import com.example.safenest.network.ChildCreateRequest
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class AddChildFragment : Fragment() {

    companion object {
        private const val TAG = "AddChildFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_add_child, container, false)

        val childNameInput = view.findViewById<TextInputEditText>(R.id.childName)
        val childAgeInput = view.findViewById<TextInputEditText>(R.id.childAge)
        val childGenderInput = view.findViewById<TextInputEditText>(R.id.childGender)
        val nameInputLayout = view.findViewById<TextInputLayout>(R.id.nameInputLayout)
        val progressBar = view.findViewById<ProgressBar?>(R.id.progressBar)

        var selectedAvatarIndex = 0

        val avatarIds = listOf(
            R.id.avatar1, R.id.avatar2, R.id.avatar3,
            R.id.avatar4, R.id.avatar5, R.id.avatar6
        )

        val avatars = avatarIds.map { id -> view.findViewById<ImageView>(id) }

        // Initial state
        avatars[0].alpha = 0.6f
        avatars[0].isSelected = true

        avatars.forEachIndexed { index, imageView ->
            imageView.setOnClickListener {
                avatars.forEach { it.alpha = 1.0f; it.isSelected = false }
                imageView.alpha = 0.6f
                imageView.isSelected = true
                selectedAvatarIndex = index
            }
        }

        val confirmButton = view.findViewById<MaterialButton>(R.id.button)
        confirmButton.setOnClickListener {
            val name = childNameInput.text.toString().trim()
            val dobOrAge = childAgeInput.text.toString().trim()
            val gender = childGenderInput.text.toString().trim()

            // Validate name
            if (name.isEmpty()) {
                nameInputLayout.error = "الرجاء إدخال اسم الطفل"
                return@setOnClickListener
            }
            nameInputLayout.error = null

            // Determine gender string for API (Male/Female)
            val genderForApi = when {
                gender.contains("ذكر", ignoreCase = true) || gender.equals("male", ignoreCase = true) -> "Male"
                gender.contains("أنثى", ignoreCase = true) || gender.equals("female", ignoreCase = true) -> "Female"
                gender.isNotEmpty() -> gender
                else -> "Male" // default
            }

            // Determine dateOfBirth from age input (YYYY-MM-DD required by API)
            // If user typed a proper date use it, otherwise estimate from age
            val dateOfBirth = if (dobOrAge.matches(Regex("\\d{4}-\\d{2}-\\d{2}"))) {
                dobOrAge
            } else {
                val age = dobOrAge.toIntOrNull() ?: 8
                val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) - age
                "$year-01-01"
            }

            setLoading(true, confirmButton, progressBar)

            viewLifecycleOwner.lifecycleScope.launch {
                try {
                    val request = ChildCreateRequest(
                        name = name,
                        gender = genderForApi,
                        dateOfBirth = dateOfBirth
                    )

                    Log.d(TAG, "Creating child: $name, gender: $genderForApi, dob: $dateOfBirth")

                    val response = ApiClient.apiService.createChild(request)

                    if (response.isSuccessful) {
                        val child = response.body()
                        Log.d(TAG, "Child created: ${child?.childId}")

                        // Save child ID and avatar preference locally for quick access
                        val prefs = requireContext().getSharedPreferences("SafeNestPrefs", android.content.Context.MODE_PRIVATE)
                        prefs.edit().apply {
                            putString("child_id", child?.childId)
                            putString("child_name", child?.name)
                            putString("child_gender", child?.gender)
                            putInt("child_avatar_index", selectedAvatarIndex)
                            apply()
                        }

                        Toast.makeText(context, "تم إضافة الطفل ${child?.name} بنجاح!", Toast.LENGTH_SHORT).show()
                        (activity as MainActivity).goToHome()
                    } else {
                        val errorBody = response.errorBody()?.string()
                        val errorMsg = ApiClient.parseError(errorBody)
                        Log.e(TAG, "Create child failed: $errorMsg")
                        Toast.makeText(context, "فشل إضافة الطفل: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Network error: ${e.message}", e)
                    Toast.makeText(context, "خطأ في الاتصال: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
                    setLoading(false, confirmButton, progressBar)
                }
            }
        }

        return view
    }

    private fun setLoading(loading: Boolean, button: MaterialButton, progressBar: ProgressBar?) {
        button.isEnabled = !loading
        button.text = if (loading) "جاري الإضافة..." else "تأكيد"
        progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
    }
}

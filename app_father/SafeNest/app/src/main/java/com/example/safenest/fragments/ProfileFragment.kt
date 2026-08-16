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
import com.example.safenest.viewmodel.ProfileViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class ProfileFragment : Fragment() {

    companion object {
        private const val TAG = "ProfileFragment"
    }

    private val viewModel: ProfileViewModel by viewModels()

    private var progressBar: ProgressBar? = null
    private var nameEditText: TextInputEditText? = null
    private var emailTv: TextView? = null
    private var phoneEditText: TextInputEditText? = null
    private var saveButton: MaterialButton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)

        progressBar = view.findViewById(R.id.progressBar)
        nameEditText = view.findViewById(R.id.nameEditText)
        emailTv = view.findViewById(R.id.emailText)
        phoneEditText = view.findViewById(R.id.phoneEditText)
        saveButton = view.findViewById(R.id.saveButton)

        saveButton?.setOnClickListener { saveProfile() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Load profile state
                launch {
                    viewModel.profileState.collect { state ->
                        when (state) {
                            is Result.Loading -> progressBar?.visibility = View.VISIBLE

                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                nameEditText?.setText(state.data.name)
                                emailTv?.text = state.data.email
                                phoneEditText?.setText(state.data.phoneNumber ?: "")
                                viewModel.clearProfileState()
                            }

                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                Toast.makeText(context, "فشل تحميل الملف الشخصي", Toast.LENGTH_SHORT).show()
                                viewModel.clearProfileState()
                            }

                            null -> Unit
                        }
                    }
                }

                // Update profile state
                launch {
                    viewModel.updateProfileState.collect { state ->
                        when (state) {
                            is Result.Loading -> {
                                progressBar?.visibility = View.VISIBLE
                                saveButton?.isEnabled = false
                            }
                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                saveButton?.isEnabled = true
                                Toast.makeText(context, "تم حفظ التغييرات بنجاح", Toast.LENGTH_SHORT).show()
                                viewModel.clearUpdateProfileState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                saveButton?.isEnabled = true
                                Toast.makeText(context, "فشل الحفظ: ${state.message}", Toast.LENGTH_LONG).show()
                                viewModel.clearUpdateProfileState()
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }

        viewModel.getProfile()
    }

    private fun saveProfile() {
        val name = nameEditText?.text.toString().trim()
        val phone = phoneEditText?.text.toString().trim()

        if (name.isEmpty()) {
            Toast.makeText(context, "الرجاء إدخال الاسم", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.updateProfile(name, phone.ifEmpty { null })
    }
}

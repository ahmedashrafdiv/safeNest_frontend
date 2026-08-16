package com.example.safenest.fragments

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.SettingsViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class SettingsFragment : Fragment() {

    companion object {
        private const val TAG = "SettingsFragment"
    }

    private val viewModel: SettingsViewModel by viewModels()

    private var progressBar: ProgressBar? = null
    private var currentPasswordInput: TextInputEditText? = null
    private var newPasswordInput: TextInputEditText? = null
    private var changePasswordButton: MaterialButton? = null
    private var logoutButton: MaterialButton? = null
    private var deleteAccountButton: MaterialButton? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_settings, container, false)

        progressBar = view.findViewById(R.id.progressBar)
        currentPasswordInput = view.findViewById(R.id.currentPasswordInput)
        newPasswordInput = view.findViewById(R.id.newPasswordInput)
        changePasswordButton = view.findViewById(R.id.changePasswordButton)
        logoutButton = view.findViewById(R.id.logoutButton)
        deleteAccountButton = view.findViewById(R.id.deleteAccountButton)

        changePasswordButton?.setOnClickListener { performChangePassword() }
        logoutButton?.setOnClickListener { performLogout() }
        deleteAccountButton?.setOnClickListener { confirmDeleteAccount() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Change password state
                launch {
                    viewModel.changePasswordState.collect { state ->
                        when (state) {
                            is Result.Loading -> {
                                progressBar?.visibility = View.VISIBLE
                                changePasswordButton?.isEnabled = false
                            }
                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                changePasswordButton?.isEnabled = true
                                Toast.makeText(context, "تم تغيير كلمة المرور بنجاح", Toast.LENGTH_SHORT).show()
                                currentPasswordInput?.text?.clear()
                                newPasswordInput?.text?.clear()
                                viewModel.clearChangePasswordState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                changePasswordButton?.isEnabled = true
                                Toast.makeText(context, "فشل التغيير: ${state.message}", Toast.LENGTH_LONG).show()
                                viewModel.clearChangePasswordState()
                            }
                            null -> Unit
                        }
                    }
                }

                // Delete account state
                launch {
                    viewModel.deleteAccountState.collect { state ->
                        when (state) {
                            is Result.Loading -> progressBar?.visibility = View.VISIBLE
                            is Result.Success -> {
                                progressBar?.visibility = View.GONE
                                Toast.makeText(context, "تم حذف الحساب", Toast.LENGTH_SHORT).show()
                                (activity as MainActivity).goToLogin()
                                viewModel.clearDeleteAccountState()
                            }
                            is Result.Error -> {
                                progressBar?.visibility = View.GONE
                                Toast.makeText(context, "فشل حذف الحساب", Toast.LENGTH_SHORT).show()
                                viewModel.clearDeleteAccountState()
                            }
                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun performChangePassword() {
        val currentPassword = currentPasswordInput?.text.toString()
        val newPassword = newPasswordInput?.text.toString()

        if (currentPassword.isEmpty()) {
            Toast.makeText(context, "الرجاء إدخال كلمة المرور الحالية", Toast.LENGTH_SHORT).show()
            return
        }
        if (newPassword.length < 8) {
            Toast.makeText(context, "كلمة المرور الجديدة يجب أن تكون 8 أحرف على الأقل", Toast.LENGTH_SHORT).show()
            return
        }

        viewModel.changePassword(currentPassword, newPassword)
    }

    private fun performLogout() {
        com.example.safenest.network.ApiClient.clearCredentials()
        Toast.makeText(context, "تم تسجيل الخروج", Toast.LENGTH_SHORT).show()
        (activity as MainActivity).goToLogin()
    }

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(requireContext())
            .setTitle("حذف الحساب")
            .setMessage("هل أنت متأكد من حذف حسابك؟ هذا الإجراء لا يمكن التراجع عنه.")
            .setPositiveButton("حذف") { _, _ -> viewModel.deleteAccount() }
            .setNegativeButton("إلغاء", null)
            .show()
    }
}

package com.example.safenest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
import com.example.safenest.viewmodel.LoginViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    companion object {
        private const val TAG = "LoginFragment"
    }

    private val viewModel: LoginViewModel by viewModels()

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var loginButton: MaterialButton
    private lateinit var childPhoneLink: LinearLayout
    private lateinit var existingAccountLink: LinearLayout
    private var progressBar: ProgressBar? = null

    // Held so the Success collector can pass the email to OtpVerificationFragment
    private var pendingEmail: String = ""
    private var pendingPassword: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login, container, false)

        emailEditText = view.findViewById(R.id.editTextTextEmailAddress)
        passwordEditText = view.findViewById(R.id.pass)
        emailInputLayout = view.findViewById(R.id.emailInputLayout)
        passwordInputLayout = view.findViewById(R.id.passwordInputLayout)
        loginButton = view.findViewById(R.id.loginBtn)
        childPhoneLink = view.findViewById(R.id.childPhoneLink)
        existingAccountLink = view.findViewById(R.id.existingAccountLink)

        loginButton.setOnClickListener { performSignUp() }

        childPhoneLink.setOnClickListener {
            (activity as MainActivity).navigateToFragment(LinkingGuideFragment())
        }
        existingAccountLink.setOnClickListener {
            (activity as MainActivity).navigateToFragment(LoginSignInFragment())
        }

        emailEditText.setOnFocusChangeListener { _, _ -> emailInputLayout.error = null }
        passwordEditText.setOnFocusChangeListener { _, _ -> passwordInputLayout.error = null }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.registerState.collect { state ->
                    when (state) {
                        is Result.Loading -> setLoading(true)

                        is Result.Success -> {
                            setLoading(false)
                            Toast.makeText(
                                context,
                                "تم إنشاء الحساب بنجاح! تحقق من بريدك الإلكتروني",
                                Toast.LENGTH_LONG
                            ).show()
                            (activity as MainActivity).navigateToFragment(
                                OtpVerificationFragment.newInstance(pendingEmail, pendingPassword)
                            )
                            viewModel.clearRegisterState()
                        }

                        is Result.Error -> {
                            setLoading(false)
                            Toast.makeText(
                                context,
                                "فشل التسجيل: ${state.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            viewModel.clearRegisterState()
                        }

                        null -> Unit
                    }
                }
            }
        }
    }

    private fun performSignUp() {
        val email = emailEditText.text.toString().trim()
        val password = passwordEditText.text.toString()

        emailInputLayout.error = null
        passwordInputLayout.error = null

        var isValid = true
        if (email.isEmpty()) {
            emailInputLayout.error = "الرجاء إدخال البريد الإلكتروني"
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout.error = "البريد الإلكتروني غير صالح"
            isValid = false
        }
        if (password.isEmpty()) {
            passwordInputLayout.error = "الرجاء إدخال كلمة المرور"
            isValid = false
        } else if (password.length < 8) {
            passwordInputLayout.error = "كلمة المرور يجب أن تكون 8 أحرف على الأقل"
            isValid = false
        }
        if (!isValid) return

        pendingEmail = email
        pendingPassword = password
        viewModel.register(email, password)
    }

    private fun setLoading(loading: Boolean) {
        loginButton.isEnabled = !loading
        progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
        loginButton.text = if (loading) "جاري التسجيل..." else "تسجيل"
    }
}

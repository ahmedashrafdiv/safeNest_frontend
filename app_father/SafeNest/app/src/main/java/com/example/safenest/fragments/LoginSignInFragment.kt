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
import com.example.safenest.viewmodel.LoginSignInViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class LoginSignInFragment : Fragment() {

    companion object {
        private const val TAG = "LoginSignInFragment"
    }

    private val viewModel: LoginSignInViewModel by viewModels()

    private lateinit var emailEditText: TextInputEditText
    private lateinit var passwordEditText: TextInputEditText
    private lateinit var emailInputLayout: TextInputLayout
    private lateinit var passwordInputLayout: TextInputLayout
    private lateinit var loginButton: MaterialButton
    private lateinit var childPhoneLink: LinearLayout
    private var progressBar: ProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login_signin, container, false)

        emailEditText = view.findViewById(R.id.editTextTextEmailAddress)
        passwordEditText = view.findViewById(R.id.pass)
        emailInputLayout = view.findViewById(R.id.emailInputLayout)
        passwordInputLayout = view.findViewById(R.id.passwordInputLayout)
        loginButton = view.findViewById(R.id.loginBtn)
        childPhoneLink = view.findViewById(R.id.childPhoneLink)

        loginButton.setOnClickListener { performLogin() }

        childPhoneLink.setOnClickListener {
            (activity as MainActivity).navigateToFragment(LoginFragment())
        }

        view.findViewById<View>(R.id.forgotPasswordText).setOnClickListener {
            (activity as MainActivity).navigateToFragment(ForgotPasswordFragment())
        }

        view.findViewById<View>(R.id.backButton).setOnClickListener {
            if (!parentFragmentManager.popBackStackImmediate()) {
                activity?.finish()
            }
        }

        emailEditText.setOnFocusChangeListener { _, _ -> emailInputLayout.error = null }
        passwordEditText.setOnFocusChangeListener { _, _ -> passwordInputLayout.error = null }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.loginState.collect { state ->
                    when (state) {
                        is Result.Loading -> setLoading(true)

                        is Result.Success -> {
                            setLoading(false)
                            Toast.makeText(context, "تم تسجيل الدخول بنجاح!", Toast.LENGTH_SHORT).show()
                            // MainActivity refreshes the FCM token after this navigation.
                            // Do not start a view-lifecycle callback after this fragment is removed.
                            (activity as MainActivity).goToHome()
                            viewModel.clearLoginState()
                        }

                        is Result.Error -> {
                            setLoading(false)
                            val message = state.message
                            val toastMsg = when {
                                message.contains("verified", ignoreCase = true) ->
                                    "الرجاء التحقق من بريدك الإلكتروني أولاً"
                                message.contains("Invalid", ignoreCase = true) ||
                                message.contains("password", ignoreCase = true) ->
                                    "البريد الإلكتروني أو كلمة المرور غير صحيحة"
                                else -> "فشل تسجيل الدخول: $message"
                            }
                            Toast.makeText(context, toastMsg, Toast.LENGTH_LONG).show()
                            viewModel.clearLoginState()
                        }

                        null -> Unit
                    }
                }
            }
        }
    }

    private fun performLogin() {
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
        } else if (password.length < 6) {
            passwordInputLayout.error = "كلمة المرور يجب أن تكون 6 أحرف على الأقل"
            isValid = false
        }
        if (!isValid) return

        viewModel.login(email, password)
    }

    private fun setLoading(loading: Boolean) {
        loginButton.isEnabled = !loading
        progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
        loginButton.text = if (loading) "جاري تسجيل الدخول..." else "تسجيل الدخول"
    }
}

package com.example.safenest.fragments

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
import com.example.safenest.viewmodel.ForgotPasswordViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ForgotPasswordFragment : Fragment() {

    companion object {
        private const val TAG = "ForgotPasswordFragment"
    }

    private val viewModel: ForgotPasswordViewModel by viewModels()

    private var emailEditText: TextInputEditText? = null
    private var emailInputLayout: TextInputLayout? = null
    private var sendBtn: MaterialButton? = null
    private var progressBar: ProgressBar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_forgot_password, container, false)

        emailEditText = view.findViewById(R.id.emailEditText)
        emailInputLayout = view.findViewById(R.id.emailInputLayout)
        sendBtn = view.findViewById(R.id.sendBtn)
        progressBar = view.findViewById(R.id.progressBar)

        view.findViewById<MaterialButton?>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        sendBtn?.setOnClickListener { performForgotPassword() }

        emailEditText?.setOnFocusChangeListener { _, _ -> emailInputLayout?.error = null }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.forgotPasswordState.collect { state ->
                    when (state) {
                        is Result.Loading -> setLoading(true)
                        is Result.Success -> {
                            setLoading(false)
                            Toast.makeText(context, "تم إرسال رابط إعادة التعيين إلى بريدك الإلكتروني", Toast.LENGTH_LONG).show()
                            val email = emailEditText?.text.toString().trim()
                            (activity as MainActivity).navigateToFragment(
                                ResetPasswordFragment.newInstance(email)
                            )
                            viewModel.clearForgotPasswordState()
                        }
                        is Result.Error -> {
                            setLoading(false)
                            Toast.makeText(context, "فشل الإرسال: ${state.message}", Toast.LENGTH_LONG).show()
                            viewModel.clearForgotPasswordState()
                        }
                        null -> Unit
                    }
                }
            }
        }
    }

    private fun performForgotPassword() {
        val email = emailEditText?.text.toString().trim()

        emailInputLayout?.error = null

        if (email.isEmpty()) {
            emailInputLayout?.error = "الرجاء إدخال البريد الإلكتروني"
            return
        }
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            emailInputLayout?.error = "البريد الإلكتروني غير صالح"
            return
        }

        viewModel.forgotPassword(email)
    }

    private fun setLoading(loading: Boolean) {
        sendBtn?.isEnabled = !loading
        sendBtn?.text = if (loading) "جاري الإرسال..." else "إرسال رابط الإعادة"
        progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
    }
}

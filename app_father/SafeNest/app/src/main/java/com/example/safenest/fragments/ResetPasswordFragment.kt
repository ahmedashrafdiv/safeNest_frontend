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
import com.example.safenest.viewmodel.ResetPasswordViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ResetPasswordFragment : Fragment() {

    companion object {
        private const val ARG_EMAIL = "email"

        fun newInstance(email: String): ResetPasswordFragment {
            val fragment = ResetPasswordFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL, email)
            fragment.arguments = args
            return fragment
        }
    }

    private val viewModel: ResetPasswordViewModel by viewModels()

    private var email: String = ""

    private var otpEditText: TextInputEditText? = null
    private var otpInputLayout: TextInputLayout? = null
    private var newPasswordEditText: TextInputEditText? = null
    private var newPasswordInputLayout: TextInputLayout? = null
    private var confirmPasswordEditText: TextInputEditText? = null
    private var confirmPasswordInputLayout: TextInputLayout? = null
    private var resetBtn: MaterialButton? = null
    private var progressBar: ProgressBar? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        email = arguments?.getString(ARG_EMAIL) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_reset_password, container, false)

        otpEditText = view.findViewById(R.id.otpEditText)
        otpInputLayout = view.findViewById(R.id.otpInputLayout)
        newPasswordEditText = view.findViewById(R.id.newPasswordEditText)
        newPasswordInputLayout = view.findViewById(R.id.newPasswordInputLayout)
        confirmPasswordEditText = view.findViewById(R.id.confirmPasswordEditText)
        confirmPasswordInputLayout = view.findViewById(R.id.confirmPasswordInputLayout)
        resetBtn = view.findViewById(R.id.resetBtn)
        progressBar = view.findViewById(R.id.progressBar)

        view.findViewById<MaterialButton?>(R.id.backButton)?.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        resetBtn?.setOnClickListener { performResetPassword() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.resetPasswordState.collect { state ->
                    when (state) {
                        is Result.Loading -> setLoading(true)
                        is Result.Success -> {
                            setLoading(false)
                            Toast.makeText(context, "تم تغيير كلمة المرور بنجاح! قم بتسجيل الدخول", Toast.LENGTH_LONG).show()
                            (activity as MainActivity).navigateToFragment(LoginSignInFragment())
                            viewModel.clearResetPasswordState()
                        }
                        is Result.Error -> {
                            setLoading(false)
                            Toast.makeText(context, "فشل التغيير: ${state.message}", Toast.LENGTH_LONG).show()
                            viewModel.clearResetPasswordState()
                        }
                        null -> Unit
                    }
                }
            }
        }
    }

    private fun performResetPassword() {
        val otp = otpEditText?.text.toString().trim()
        val newPassword = newPasswordEditText?.text.toString()
        val confirmPassword = confirmPasswordEditText?.text.toString()

        otpInputLayout?.error = null
        newPasswordInputLayout?.error = null
        confirmPasswordInputLayout?.error = null

        var isValid = true
        if (otp.isEmpty() || otp.length != 6) {
            otpInputLayout?.error = "الرجاء إدخال رمز التحقق المكون من 6 أرقام"
            isValid = false
        }
        if (newPassword.length < 8) {
            newPasswordInputLayout?.error = "كلمة المرور يجب أن تكون 8 أحرف على الأقل"
            isValid = false
        }
        if (newPassword != confirmPassword) {
            confirmPasswordInputLayout?.error = "كلمتا المرور غير متطابقتين"
            isValid = false
        }
        if (!isValid) return

        viewModel.resetPassword(email, otp, newPassword)
    }

    private fun setLoading(loading: Boolean) {
        resetBtn?.isEnabled = !loading
        resetBtn?.text = if (loading) "جاري التغيير..." else "تغيير كلمة المرور"
        progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
    }
}

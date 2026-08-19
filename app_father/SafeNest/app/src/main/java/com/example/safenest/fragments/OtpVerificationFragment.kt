package com.example.safenest.fragments

import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.util.OtpCodeValidator
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.OtpVerificationViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class OtpVerificationFragment : Fragment() {

    companion object {
        private const val ARG_EMAIL = "email"
        private const val ARG_PASSWORD = "password"
        private const val RESEND_COOLDOWN_MS = 60000L

        fun newInstance(email: String, password: String): OtpVerificationFragment {
            val fragment = OtpVerificationFragment()
            val args = Bundle()
            args.putString(ARG_EMAIL, email)
            args.putString(ARG_PASSWORD, password)
            fragment.arguments = args
            return fragment
        }
    }

    private val viewModel: OtpVerificationViewModel by viewModels()

    private lateinit var emailDisplay: TextView
    private lateinit var errorMessage: TextView
    private lateinit var verifyBtn: MaterialButton
    private lateinit var resendBtn: TextView
    private lateinit var timerText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var otpCodeInput: TextInputEditText

    private var email: String = ""
    private var password: String = ""
    private var countDownTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        email = arguments?.getString(ARG_EMAIL) ?: ""
        password = arguments?.getString(ARG_PASSWORD) ?: ""
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_otp_verification, container, false)

        emailDisplay = view.findViewById(R.id.emailDisplay)
        errorMessage = view.findViewById(R.id.errorMessage)
        verifyBtn = view.findViewById(R.id.verifyBtn)
        resendBtn = view.findViewById(R.id.resendBtn)
        timerText = view.findViewById(R.id.timerText)
        progressBar = view.findViewById(R.id.progressBar)
        otpCodeInput = view.findViewById(R.id.otpCodeEditText)

        emailDisplay.text = email
        setupOtpCodeInput()

        verifyBtn.setOnClickListener { verifyOtp() }
        resendBtn.setOnClickListener { viewModel.resendOtp(email) }
        view.findViewById<View>(R.id.backButton).setOnClickListener {
            if (!parentFragmentManager.popBackStackImmediate()) {
                activity?.finish()
            }
        }

        startResendCooldown()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // Collect verify state
                launch {
                    viewModel.verifyState.collect { state ->
                        when (state) {
                            is Result.Loading -> setLoading(true)

                            is Result.Success -> {
                                // Don't stop loading — immediately trigger auto-login
                                Toast.makeText(context, "تم التحقق بنجاح!", Toast.LENGTH_SHORT).show()
                                viewModel.login(email, password)
                                viewModel.clearVerifyState()
                            }

                            is Result.Error -> {
                                setLoading(false)
                                errorMessage.text = "رمز التحقق غير صحيح"
                                errorMessage.visibility = View.VISIBLE
                                clearOtpCode()
                                viewModel.clearVerifyState()
                            }

                            null -> Unit
                        }
                    }
                }

                // Collect auto-login state
                launch {
                    viewModel.loginState.collect { state ->
                        when (state) {
                            is Result.Loading -> Unit // already loading from verify step

                            is Result.Success -> {
                                setLoading(false)
                                (activity as MainActivity).goToHomeAfterAuth(AddChildFragment())
                                viewModel.clearLoginState()
                            }

                            is Result.Error -> {
                                setLoading(false)
                                // Auto-login failed but verification succeeded — navigate anyway
                                (activity as MainActivity).goToHomeAfterAuth(AddChildFragment())
                                viewModel.clearLoginState()
                            }

                            null -> Unit
                        }
                    }
                }

                // Collect resend state
                launch {
                    viewModel.resendState.collect { state ->
                        when (state) {
                            is Result.Loading -> setLoading(true)

                            is Result.Success -> {
                                setLoading(false)
                                Toast.makeText(context, "تم إعادة إرسال الرمز", Toast.LENGTH_SHORT).show()
                                startResendCooldown()
                                clearOtpCode()
                                viewModel.clearResendState()
                            }

                            is Result.Error -> {
                                setLoading(false)
                                Toast.makeText(context, "فشل إعادة الإرسال: ${state.message}", Toast.LENGTH_LONG).show()
                                viewModel.clearResendState()
                            }

                            null -> Unit
                        }
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        countDownTimer?.cancel()
    }

    private fun setupOtpCodeInput() {
        otpCodeInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                errorMessage.visibility = View.GONE
            }
        })
    }

    private fun clearOtpCode() {
        otpCodeInput.text?.clear()
        otpCodeInput.requestFocus()
    }

    private fun verifyOtp() {
        val otp = otpCodeInput.text?.toString().orEmpty()
        OtpCodeValidator.error(otp)?.let { error ->
            errorMessage.text = error
            errorMessage.visibility = View.VISIBLE
            return
        }
        viewModel.verifyEmail(email, otp)
    }

    private fun startResendCooldown() {
        resendBtn.isEnabled = false
        resendBtn.alpha = 0.5f
        timerText.visibility = View.VISIBLE
        countDownTimer?.cancel()
        countDownTimer = object : CountDownTimer(RESEND_COOLDOWN_MS, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timerText.text = "إعادة الإرسال بعد ${millisUntilFinished / 1000} ثانية"
            }
            override fun onFinish() {
                resendBtn.isEnabled = true
                resendBtn.alpha = 1.0f
                timerText.visibility = View.GONE
            }
        }.start()
    }

    private fun setLoading(loading: Boolean) {
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        verifyBtn.isEnabled = !loading
        verifyBtn.text = if (loading) "جاري التحقق..." else "تحقق"
        otpCodeInput.isEnabled = !loading
    }
}

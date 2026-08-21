package com.safenest.kids

import android.os.Bundle
import android.text.InputType
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.ParentVerificationRequest
import com.safenest.kids.util.ParentVerificationDecider
import com.safenest.kids.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ParentVerificationDialog : DialogFragment() {

    private lateinit var passwordLayout: TextInputLayout
    private lateinit var passwordInput: TextInputEditText
    private lateinit var errorText: TextView
    private lateinit var confirmButton: Button
    private lateinit var progress: ProgressBar
    private var verifying = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.dialog_parent_verification, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog?.setCanceledOnTouchOutside(!verifying)

        val email = requireArguments().getString(ARG_PARENT_EMAIL).orEmpty()
        val emailInput = view.findViewById<TextInputEditText>(R.id.input_parent_email)
        passwordLayout = view.findViewById(R.id.layout_parent_password)
        passwordInput = view.findViewById(R.id.input_parent_password)
        errorText = view.findViewById(R.id.tv_parent_verification_error)
        confirmButton = view.findViewById(R.id.btn_parent_verification_confirm)
        progress = view.findViewById(R.id.progress_parent_verification)

        emailInput.setText(email)
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD

        view.findViewById<View>(R.id.btn_parent_verification_cancel).setOnClickListener { dismiss() }
        confirmButton.setOnClickListener { verifyPassword() }
    }

    override fun onDestroyView() {
        passwordInput.text?.clear()
        super.onDestroyView()
    }

    private fun verifyPassword() {
        if (verifying) return
        val passwordChars = passwordInput.text?.toString()?.toCharArray() ?: CharArray(0)
        if (passwordChars.isEmpty()) {
            passwordLayout.error = getString(R.string.parent_verification_password_required)
            return
        }

        passwordLayout.error = null
        errorText.visibility = View.GONE
        setVerifying(true)
        val deviceId = PrefsHelper(requireContext()).getDeviceId()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val outcome = try {
                val response = ApiClient.apiService.verifyParentPassword(
                    deviceId,
                    ParentVerificationRequest(String(passwordChars)),
                )
                ParentVerificationDecider.outcome(
                    httpCode = response.code(),
                    verified = response.body()?.verified == true,
                    errorCode = ParentVerificationDecider.errorCodeOf(response.errorBody()?.string()),
                )
            } catch (_: Exception) {
                ParentVerificationDecider.offlineOutcome()
            } finally {
                passwordChars.fill('\u0000')
            }

            withContext(Dispatchers.Main) {
                if (!isAdded) return@withContext
                if (outcome == ParentVerificationDecider.Outcome.VERIFIED) {
                    parentFragmentManager.setFragmentResult(
                        REQUEST_KEY,
                        bundleOf(BUNDLE_ACTION to requireAction().name),
                    )
                    dismiss()
                } else {
                    setVerifying(false)
                    errorText.setText(outcomeMessage(outcome))
                    errorText.visibility = View.VISIBLE
                    passwordInput.text?.clear()
                    passwordInput.requestFocus()
                }
            }
        }
    }

    private fun setVerifying(inProgress: Boolean) {
        verifying = inProgress
        confirmButton.isEnabled = !inProgress
        passwordInput.isEnabled = !inProgress
        progress.visibility = if (inProgress) View.VISIBLE else View.GONE
    }

    private fun outcomeMessage(outcome: ParentVerificationDecider.Outcome): Int = when (outcome) {
        ParentVerificationDecider.Outcome.WRONG_PASSWORD -> R.string.parent_verification_wrong_password
        ParentVerificationDecider.Outcome.LOCKED -> R.string.parent_verification_locked
        ParentVerificationDecider.Outcome.SESSION_EXPIRED -> R.string.parent_verification_session_expired
        ParentVerificationDecider.Outcome.NOT_AUTHORIZED -> R.string.parent_verification_not_authorized
        ParentVerificationDecider.Outcome.ACCOUNT_UNAVAILABLE -> R.string.parent_verification_account_unavailable
        ParentVerificationDecider.Outcome.UNAVAILABLE -> R.string.parent_verification_unavailable
        ParentVerificationDecider.Outcome.VERIFIED -> R.string.parent_verification_unavailable
    }

    private fun requireAction(): ParentControlAction =
        ParentControlAction.valueOf(requireArguments().getString(ARG_ACTION).orEmpty())

    companion object {
        const val TAG = "parent_verification_dialog"
        const val REQUEST_KEY = "parent_verification_result"
        const val BUNDLE_ACTION = "parent_verification_action_name"
        private const val ARG_ACTION = "parent_verification_action"
        private const val ARG_PARENT_EMAIL = "parent_verification_parent_email"

        fun newInstance(action: ParentControlAction, parentEmail: String): ParentVerificationDialog =
            ParentVerificationDialog().apply {
                arguments = bundleOf(
                    ARG_ACTION to action.name,
                    ARG_PARENT_EMAIL to parentEmail,
                )
            }
    }
}

package com.safenest.kids

import android.os.Build
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.ChildDeviceClaimRequest
import com.safenest.kids.network.ChildDeviceClaimResponse
import com.safenest.kids.pairing.OtpInputValidator
import com.safenest.kids.service.PlacePolicySyncWorker
import com.safenest.kids.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** Layngo child OTP entry: six real cells, calm recovery, and no legacy PIN endpoint. */
class PairingFragment : Fragment() {
    private lateinit var otpCells: List<AppCompatEditText>
    private lateinit var confirmButton: MaterialButton
    private lateinit var newCodeButton: MaterialButton
    private lateinit var errorText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var prefsHelper: PrefsHelper
    private var isDistributingInput = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_pairing, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsHelper = PrefsHelper(requireContext())
        otpCells = listOf(
            view.findViewById(R.id.otp_digit_1),
            view.findViewById(R.id.otp_digit_2),
            view.findViewById(R.id.otp_digit_3),
            view.findViewById(R.id.otp_digit_4),
            view.findViewById(R.id.otp_digit_5),
            view.findViewById(R.id.otp_digit_6),
        )
        confirmButton = view.findViewById(R.id.btn_link)
        newCodeButton = view.findViewById(R.id.btn_new_code)
        errorText = view.findViewById(R.id.tv_error)
        progressBar = view.findViewById(R.id.progress_bar)

        configureOtpCells()
        confirmButton.setOnClickListener { claimCurrentCode() }
        newCodeButton.setOnClickListener { clearCodeAndFocus() }
        view.findViewById<MaterialButton>(R.id.btn_back).setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        otpCells.first().requestFocus()
    }

    private fun configureOtpCells() {
        otpCells.forEachIndexed { index, cell ->
            cell.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(value: Editable?) {
                    if (isDistributingInput) return
                    val digits = OtpInputValidator.asciiDigits(value?.toString().orEmpty())
                    if (digits.length > 1) {
                        distributeDigits(digits, index)
                        return
                    }
                    if (digits.length == 1 && index < otpCells.lastIndex) {
                        otpCells[index + 1].requestFocus()
                    }
                    hideError()
                }
            })
            cell.setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN && cell.text.isNullOrEmpty() && index > 0) {
                    otpCells[index - 1].requestFocus()
                    otpCells[index - 1].text?.clear()
                    true
                } else {
                    false
                }
            }
        }
    }

    private fun distributeDigits(raw: String, startIndex: Int) {
        isDistributingInput = true
        raw.take(otpCells.size - startIndex).forEachIndexed { offset, digit ->
            otpCells[startIndex + offset].setText(digit.toString())
        }
        isDistributingInput = false
        otpCells[minOf(startIndex + raw.length, otpCells.lastIndex)].requestFocus()
        hideError()
    }

    private fun claimCurrentCode() {
        val code = OtpInputValidator.asciiDigits(
            otpCells.joinToString(separator = "") { it.text?.toString().orEmpty() },
        )
        if (!OtpInputValidator.isComplete(code)) {
            showValidationError("أدخل الرمز المكوّن من ستة أرقام.")
            return
        }
        setLoading(true)
        val request = ChildDeviceClaimRequest(
            pairingCode = code,
            deviceId = prefsHelper.getDeviceId(),
            platform = "android",
            model = "${Build.MANUFACTURER} ${Build.MODEL}".take(128),
            osVersion = "Android ${Build.VERSION.RELEASE}".take(64),
            appVersion = BuildConfig.VERSION_NAME.take(64),
        )
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val result = runCatching { ApiClient.apiService.claimChildDevice(request) }
            withContext(Dispatchers.Main) {
                result.onSuccess { response ->
                    val body = response.body()
                    if (response.isSuccessful && body != null) {
                        completePairing(body)
                    } else if (response.code() in OTP_FAILURE_CODES) {
                        showOtpError()
                    } else {
                        showValidationError("تعذر إتمام الربط حالياً. حاول مرة أخرى.")
                    }
                }.onFailure {
                    showValidationError("حدث خطأ في الاتصال. حاول مرة أخرى.")
                }
            }
        }
    }

    private fun completePairing(response: ChildDeviceClaimResponse) {
        prefsHelper.setChildId(response.childId)
        prefsHelper.setPaired(true)
        prefsHelper.setJustPaired(true)
        prefsHelper.setLastAppsSent(false)
        prefsHelper.setDeviceToken(response.accessToken)
        PlacePolicySyncWorker.enqueueImmediate(requireContext())
        PlacePolicySyncWorker.enqueuePeriodic(requireContext())
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, PermissionsFragment())
            .commit()
    }

    private fun showOtpError() {
        errorText.text = "الرمز غير صحيح أو انتهت صلاحيته. اطلب رمزًا جديدًا من هاتف الوالد."
        errorText.visibility = View.VISIBLE
        otpCells.forEach { it.setBackgroundResource(R.drawable.otp_cell_error_background) }
        newCodeButton.visibility = View.VISIBLE
        setLoading(false)
    }

    private fun showValidationError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
        setLoading(false)
    }

    private fun hideError() {
        errorText.visibility = View.GONE
        otpCells.forEach { it.setBackgroundResource(R.drawable.otp_cell_background) }
        newCodeButton.visibility = View.GONE
    }

    private fun clearCodeAndFocus() {
        isDistributingInput = true
        otpCells.forEach { it.text?.clear() }
        isDistributingInput = false
        hideError()
        otpCells.first().requestFocus()
    }

    private fun setLoading(loading: Boolean) {
        confirmButton.isEnabled = !loading
        newCodeButton.isEnabled = !loading
        progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        confirmButton.text = if (loading) "جارٍ التحقق…" else "تأكيد الرمز"
    }

    private companion object {
        val OTP_FAILURE_CODES = setOf(400, 404, 409, 410)
    }
}

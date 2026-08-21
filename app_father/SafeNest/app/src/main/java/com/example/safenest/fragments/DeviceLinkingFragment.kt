package com.example.safenest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.R
import com.example.safenest.network.ChildDevicePairingResponse
import com.example.safenest.network.ChildDevicePairingStatusResponse
import com.example.safenest.pairing.PairingOtpDisplay
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.ChildDevicesViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Layngo OTP linking: Arabic RTL, one parent action per screen, lifecycle-bound polling only. */
class DeviceLinkingFragment : Fragment() {
    private enum class LinkScreen { READY, CODE, WAITING, SUCCESS }

    private val viewModel: ChildDevicesViewModel by viewModels()
    private var countdownJob: Job? = null
    private var pollingJob: Job? = null
    private var currentPairing: ChildDevicePairingResponse? = null
    private var expiresAtMillis: Long = 0L

    private lateinit var backButton: MaterialButton
    private lateinit var stepText: TextView
    private lateinit var readyContent: View
    private lateinit var codeContent: View
    private lateinit var waitingContent: View
    private lateinit var successContent: View
    private lateinit var pairingCodeText: TextView
    private lateinit var expiryText: TextView
    private lateinit var errorText: TextView
    private lateinit var readyErrorText: TextView
    private lateinit var startButton: MaterialButton
    private lateinit var confirmCodeButton: MaterialButton
    private lateinit var resendButton: MaterialButton

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = inflater.inflate(R.layout.fragment_device_linking, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        backButton = view.findViewById(R.id.btn_link_back)
        stepText = view.findViewById(R.id.tv_link_step)
        readyContent = view.findViewById(R.id.link_ready_content)
        codeContent = view.findViewById(R.id.link_code_content)
        waitingContent = view.findViewById(R.id.link_waiting_content)
        successContent = view.findViewById(R.id.link_success_content)
        pairingCodeText = view.findViewById(R.id.tv_pairing_code)
        expiryText = view.findViewById(R.id.tv_pairing_expiry)
        errorText = view.findViewById(R.id.tv_linking_error)
        readyErrorText = view.findViewById(R.id.tv_ready_linking_error)
        startButton = view.findViewById(R.id.btn_start_linking)
        confirmCodeButton = view.findViewById(R.id.btn_confirm_child_code)
        resendButton = view.findViewById(R.id.btn_resend_pairing_code)

        backButton.setOnClickListener { parentFragmentManager.popBackStack() }
        startButton.setOnClickListener { requestPairingCode() }
        view.findViewById<MaterialButton>(R.id.btn_link_later).setOnClickListener { parentFragmentManager.popBackStack() }
        confirmCodeButton.setOnClickListener { beginVerification() }
        resendButton.setOnClickListener { requestPairingCode() }
        view.findViewById<MaterialButton>(R.id.btn_continue_protection).setOnClickListener { parentFragmentManager.popBackStack() }
        view.findViewById<MaterialButton>(R.id.btn_return_child_page).setOnClickListener { parentFragmentManager.popBackStack() }

        observePairing()
        render(LinkScreen.READY)
    }

    private fun observePairing() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.pairingState.collect { state ->
                        when (state) {
                            is Result.Loading -> setCodeRequestLoading(true)
                            is Result.Success -> {
                                setCodeRequestLoading(false)
                                currentPairing = state.data
                                expiresAtMillis = parseExpiry(state.data.expiresAt)
                                showCode(state.data)
                                viewModel.clearPairingState()
                            }
                            is Result.Error -> {
                                setCodeRequestLoading(false)
                                if (currentPairing == null) {
                                    readyErrorText.text = state.message
                                    readyErrorText.visibility = View.VISIBLE
                                } else {
                                    showCodeError(state.message)
                                }
                                viewModel.clearPairingState()
                            }
                            null -> Unit
                        }
                    }
                }
                launch {
                    viewModel.pairingStatusState.collect { state ->
                        when (state) {
                            is Result.Success -> handlePairingStatus(state.data)
                            is Result.Error -> showWaitingRetryState()
                            else -> Unit
                        }
                    }
                }
            }
        }
    }

    private fun requestPairingCode() {
        errorText.visibility = View.GONE
        readyErrorText.visibility = View.GONE
        countdownJob?.cancel()
        pollingJob?.cancel()
        viewModel.createPairing()
    }

    private fun showCode(pairing: ChildDevicePairingResponse) {
        pairingCodeText.text = PairingOtpDisplay.spacedCode(pairing.pairingCode)
        render(LinkScreen.CODE)
        startCountdown()
    }

    private fun beginVerification() {
        val pairing = currentPairing ?: return
        if (System.currentTimeMillis() >= expiresAtMillis) {
            showCodeError("انتهت صلاحية الرمز. يمكنك إرسال رمز جديد الآن.")
            resendButton.isEnabled = true
            return
        }
        render(LinkScreen.WAITING)
        startPolling(pairing.pairingId)
    }

    private fun startPolling(pairingId: String) {
        pollingJob?.cancel()
        pollingJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                viewModel.refreshPairingStatus(pairingId)
                delay(PAIRING_STATUS_POLL_MILLIS)
            }
        }
    }

    private fun handlePairingStatus(status: ChildDevicePairingStatusResponse) {
        when (status.status.uppercase(Locale.ROOT)) {
            "CLAIMED" -> {
                pollingJob?.cancel()
                viewModel.clearPairingStatusState()
                render(LinkScreen.SUCCESS)
            }
            "EXPIRED" -> {
                pollingJob?.cancel()
                viewModel.clearPairingStatusState()
                render(LinkScreen.CODE)
                showCodeError("انتهت صلاحية الرمز. يمكنك إرسال رمز جديد الآن.")
                resendButton.isEnabled = true
            }
        }
    }

    private fun showWaitingRetryState() {
        // Transient network errors remain in the automatic waiting state; the next poll retries safely.
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        resendButton.isEnabled = false
        countdownJob = viewLifecycleOwner.lifecycleScope.launch {
            while (true) {
                val seconds = PairingOtpDisplay.secondsRemaining(expiresAtMillis, System.currentTimeMillis())
                if (seconds == 0L) {
                    expiryText.text = "انتهت صلاحية الرمز"
                    resendButton.isEnabled = true
                    return@launch
                }
                expiryText.text = "◷ الرمز صالح لمدة ${seconds / 60}:${"%02d".format(seconds % 60)} دقائق"
                delay(1_000)
            }
        }
    }

    private fun parseExpiry(value: String): Long {
        val normalized = value.substringBefore('.').substringBefore('+').substringBefore('Z')
        val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        return parser.parse(normalized)?.time ?: 0L
    }

    private fun setCodeRequestLoading(loading: Boolean) {
        startButton.isEnabled = !loading
        resendButton.isEnabled = !loading && System.currentTimeMillis() >= expiresAtMillis
        if (loading) startButton.text = "جارٍ إنشاء الرمز…" else startButton.text = "ابدأ الربط"
    }

    private fun showCodeError(message: String) {
        errorText.text = message
        errorText.visibility = View.VISIBLE
    }

    private fun render(screen: LinkScreen) {
        val ready = screen == LinkScreen.READY
        val code = screen == LinkScreen.CODE
        readyContent.visibility = if (ready) View.VISIBLE else View.GONE
        codeContent.visibility = if (code) View.VISIBLE else View.GONE
        waitingContent.visibility = if (screen == LinkScreen.WAITING) View.VISIBLE else View.GONE
        successContent.visibility = if (screen == LinkScreen.SUCCESS) View.VISIBLE else View.GONE
        backButton.visibility = if (ready || screen == LinkScreen.SUCCESS) View.GONE else View.VISIBLE
        stepText.text = when (screen) {
            LinkScreen.READY -> "ربط الجهاز • الخطوة 1 من 2"
            LinkScreen.CODE, LinkScreen.WAITING -> "ربط الجهاز • الخطوة 2 من 2"
            LinkScreen.SUCCESS -> ""
        }
    }

    override fun onDestroyView() {
        countdownJob?.cancel()
        pollingJob?.cancel()
        viewModel.clearPairingStatusState()
        super.onDestroyView()
    }

    private companion object {
        const val PAIRING_STATUS_POLL_MILLIS = 2_000L
    }
}

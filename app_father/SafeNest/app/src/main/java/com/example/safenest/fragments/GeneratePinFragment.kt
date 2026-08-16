package com.example.safenest.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.safenest.R
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.DevicesViewModel
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class GeneratePinFragment : Fragment() {

    companion object {
        private const val TAG = "GeneratePinFragment"
    }

    private val viewModel: DevicesViewModel by viewModels()
    private var countdownJob: Job? = null

    private var pinCodeText: TextView? = null
    private var countdownText: TextView? = null
    private var generateNewPinBtn: MaterialButton? = null
    private var progressBar: ProgressBar? = null
    private var errorText: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_generate_pin, container, false)

        pinCodeText = view.findViewById(R.id.pinCodeText)
        countdownText = view.findViewById(R.id.countdownText)
        generateNewPinBtn = view.findViewById(R.id.generateNewPinBtn)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)

        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        generateNewPinBtn?.setOnClickListener {
            hideError()
            val childId = viewModel.getSelectedChildId() ?: run {
                showError("لم يتم تحديد الطفل")
                return@setOnClickListener
            }
            viewModel.generatePin(childId)
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.generatePinState.collect { state ->
                    when (state) {
                        is Result.Loading -> {
                            progressBar?.visibility = View.VISIBLE
                            generateNewPinBtn?.isEnabled = false
                            pinCodeText?.text = "------"
                            countdownText?.text = ""
                            hideError()
                        }

                        is Result.Success -> {
                            progressBar?.visibility = View.GONE
                            generateNewPinBtn?.isEnabled = true
                            pinCodeText?.text = state.data.pin
                            val sdf = java.text.SimpleDateFormat(
                                "yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault()
                            )
                            sdf.timeZone = java.util.TimeZone.getTimeZone("UTC")
                            val expireTime = sdf.parse(
                                state.data.expiresAt.substringBefore(".")
                            )?.time ?: 0L
                            val remainingSeconds = ((expireTime - System.currentTimeMillis()) / 1000)
                                .toInt().coerceAtLeast(0)
                            startCountdown(remainingSeconds)
                            viewModel.clearGeneratePinState()
                        }

                        is Result.Error -> {
                            progressBar?.visibility = View.GONE
                            generateNewPinBtn?.isEnabled = true
                            showError(state.message)
                            viewModel.clearGeneratePinState()
                        }

                        null -> Unit
                    }
                }
            }
        }

        // Initial load
        val childId = viewModel.getSelectedChildId() ?: run {
            showError("لم يتم تحديد الطفل")
            return
        }
        viewModel.generatePin(childId)
    }

    private fun startCountdown(seconds: Int) {
        countdownJob?.cancel()
        countdownJob = viewLifecycleOwner.lifecycleScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                val m = remaining / 60
                val s = remaining % 60
                countdownText?.text = "ينتهي خلال: %02d:%02d".format(m, s)
                delay(1000)
                remaining--
            }
            // PIN expired
            countdownText?.text = "انتهى الكود — اطلب كوداً جديداً"
            pinCodeText?.text = "------"
            generateNewPinBtn?.isEnabled = true
        }
    }

    private fun showError(message: String) {
        errorText?.text = message
        errorText?.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorText?.visibility = View.GONE
    }

    override fun onDestroyView() {
        countdownJob?.cancel()
        pinCodeText = null
        countdownText = null
        generateNewPinBtn = null
        progressBar = null
        errorText = null
        super.onDestroyView()
    }
}

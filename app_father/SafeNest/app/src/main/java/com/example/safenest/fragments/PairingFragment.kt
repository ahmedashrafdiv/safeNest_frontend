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
import com.example.safenest.MainActivity
import com.example.safenest.R
import com.example.safenest.util.Result
import com.example.safenest.viewmodel.DevicesViewModel
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

class PairingFragment : Fragment() {

    companion object {
        private const val TAG = "PairingFragment"
    }

    private val viewModel: DevicesViewModel by viewModels()

    private var pairingCodeInput: TextInputEditText? = null
    private var deviceNameInput: TextInputEditText? = null
    private var deviceTypeInput: TextInputEditText? = null
    private var pairBtn: MaterialButton? = null
    private var progressBar: ProgressBar? = null
    private var errorText: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_device_pairing, container, false)

        pairingCodeInput = view.findViewById(R.id.pairingCodeInput)
        deviceNameInput = view.findViewById(R.id.deviceNameInput)
        deviceTypeInput = view.findViewById(R.id.deviceTypeInput)
        pairBtn = view.findViewById(R.id.pairBtn)
        progressBar = view.findViewById(R.id.progressBar)
        errorText = view.findViewById(R.id.errorText)

        view.findViewById<MaterialButton>(R.id.backButton).setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        pairBtn?.setOnClickListener { performPairing() }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.pairDeviceState.collect { state ->
                    when (state) {
                        is Result.Loading -> setLoading(true)
                        is Result.Success -> {
                            setLoading(false)
                            (activity as MainActivity).goToHome()
                            viewModel.clearPairDeviceState()
                        }
                        is Result.Error -> {
                            setLoading(false)
                            showError(state.message)
                            viewModel.clearPairDeviceState()
                        }
                        null -> Unit
                    }
                }
            }
        }
    }

    private fun performPairing() {
        val code = pairingCodeInput?.text.toString().trim().uppercase()
        val deviceName = deviceNameInput?.text.toString().trim()
        val deviceType = deviceTypeInput?.text.toString().trim()

        if (code.isEmpty() || code.length != 6) { showError(getString(R.string.error_pairing_code_invalid)); return }
        if (deviceName.isEmpty()) { showError(getString(R.string.error_device_name_required)); return }
        if (deviceType.isEmpty()) { showError(getString(R.string.error_device_type_required)); return }

        hideError()
        viewModel.pairDevice(code, deviceName, deviceType)
    }

    private fun showError(message: String) {
        errorText?.text = message
        errorText?.visibility = View.VISIBLE
    }

    private fun hideError() {
        errorText?.visibility = View.GONE
    }

    private fun setLoading(loading: Boolean) {
        pairBtn?.isEnabled = !loading
        pairBtn?.text = if (loading) getString(R.string.pairing_in_progress) else getString(R.string.pair_device)
        progressBar?.visibility = if (loading) View.VISIBLE else View.GONE
    }
}

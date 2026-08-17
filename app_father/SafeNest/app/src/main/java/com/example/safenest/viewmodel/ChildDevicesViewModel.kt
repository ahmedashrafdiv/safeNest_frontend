package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.ChildDeviceAuditResult
import com.example.safenest.network.ChildDevicePairingResponse
import com.example.safenest.network.ChildDeviceSummary
import com.example.safenest.repository.ChildDeviceRepository
import com.example.safenest.util.Result
import com.example.safenest.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChildDevicesViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ChildDeviceRepository()
    private val sessionManager = SessionManager(application)
    private val _devicesState = MutableStateFlow<Result<List<ChildDeviceSummary>>?>(null)
    val devicesState: StateFlow<Result<List<ChildDeviceSummary>>?> = _devicesState.asStateFlow()
    private val _pairingState = MutableStateFlow<Result<ChildDevicePairingResponse>?>(null)
    val pairingState: StateFlow<Result<ChildDevicePairingResponse>?> = _pairingState.asStateFlow()
    private val _revokeState = MutableStateFlow<Result<ChildDeviceAuditResult>?>(null)
    val revokeState: StateFlow<Result<ChildDeviceAuditResult>?> = _revokeState.asStateFlow()

    fun selectedChildId(): String? = sessionManager.getSelectedChildId()

    fun loadDevices() {
        val childId = selectedChildId() ?: run {
            _devicesState.value = Result.Error("Select a child before managing devices")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _devicesState.value = Result.Loading
            _devicesState.value = repository.listDevices(childId)
        }
    }

    fun createPairing() {
        val childId = selectedChildId() ?: run {
            _pairingState.value = Result.Error("Select a child before pairing a device")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _pairingState.value = Result.Loading
            _pairingState.value = repository.createPairing(childId)
        }
    }

    fun revokeDevice(deviceId: String) {
        val childId = selectedChildId() ?: run {
            _revokeState.value = Result.Error("Select a child before revoking a device")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            _revokeState.value = Result.Loading
            _revokeState.value = repository.revokeDevice(childId, deviceId, "parent_revoked")
        }
    }
}

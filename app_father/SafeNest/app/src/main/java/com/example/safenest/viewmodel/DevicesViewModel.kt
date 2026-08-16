package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.DeviceOut
import com.example.safenest.network.DeviceStatusResponse
import com.example.safenest.network.GeneratePinResponse
import com.example.safenest.repository.DeviceRepository
import com.example.safenest.util.Result
import com.example.safenest.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DevicesViewModel(application: Application) : AndroidViewModel(application) {

    private val deviceRepository = DeviceRepository()
    private val sessionManager = SessionManager(application)

    private val _devicesState = MutableStateFlow<Result<List<DeviceOut>>?>(null)
    val devicesState: StateFlow<Result<List<DeviceOut>>?> = _devicesState.asStateFlow()

    private val _devicesStatusState = MutableStateFlow<Result<List<DeviceStatusResponse>>?>(null)
    val devicesStatusState: StateFlow<Result<List<DeviceStatusResponse>>?> = _devicesStatusState.asStateFlow()

    private val _pairDeviceState = MutableStateFlow<Result<DeviceOut>?>(null)
    val pairDeviceState: StateFlow<Result<DeviceOut>?> = _pairDeviceState.asStateFlow()

    private val _deleteDeviceState = MutableStateFlow<Result<Map<String, Any>>?>(null)
    val deleteDeviceState: StateFlow<Result<Map<String, Any>>?> = _deleteDeviceState.asStateFlow()

    private val _generatePinState = MutableStateFlow<Result<GeneratePinResponse>?>(null)
    val generatePinState: StateFlow<Result<GeneratePinResponse>?> = _generatePinState.asStateFlow()

    fun listDevices() {
        viewModelScope.launch(Dispatchers.IO) {
            _devicesState.value = Result.Loading
            _devicesState.value = deviceRepository.listDevices()
        }
    }

    fun clearDevicesState() {
        _devicesState.value = null
    }

    fun listDevicesStatus() {
        viewModelScope.launch(Dispatchers.IO) {
            _devicesStatusState.value = Result.Loading
            _devicesStatusState.value = deviceRepository.listDevicesStatus()
        }
    }

    fun clearDevicesStatusState() {
        _devicesStatusState.value = null
    }

    fun pairDevice(deviceId: String, deviceName: String, deviceType: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _pairDeviceState.value = Result.Loading
            _pairDeviceState.value = deviceRepository.pairDevice(deviceId, deviceName, deviceType)
        }
    }

    fun clearPairDeviceState() {
        _pairDeviceState.value = null
    }

    fun deleteDevice(deviceId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _deleteDeviceState.value = Result.Loading
            _deleteDeviceState.value = deviceRepository.deleteDevice(deviceId)
        }
    }

    fun clearDeleteDeviceState() {
        _deleteDeviceState.value = null
    }

    fun generatePin(childId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _generatePinState.value = Result.Loading
            _generatePinState.value = deviceRepository.generatePin(childId)
        }
    }

    fun clearGeneratePinState() {
        _generatePinState.value = null
    }

    fun getSelectedChildId(): String? = sessionManager.getSelectedChildId()
}

package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.AlertOut
import com.example.safenest.repository.AlertRepository
import com.example.safenest.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class NotificationsViewModel(application: Application) : AndroidViewModel(application) {

    private val alertRepository = AlertRepository()

    private val _alertsState = MutableStateFlow<Result<List<AlertOut>>?>(null)
    val alertsState: StateFlow<Result<List<AlertOut>>?> = _alertsState.asStateFlow()

    private val _updateAlertState = MutableStateFlow<Result<AlertOut>?>(null)
    val updateAlertState: StateFlow<Result<AlertOut>?> = _updateAlertState.asStateFlow()

    private val _deleteAlertState = MutableStateFlow<Result<Map<String, Any>>?>(null)
    val deleteAlertState: StateFlow<Result<Map<String, Any>>?> = _deleteAlertState.asStateFlow()

    fun listAlerts() {
        viewModelScope.launch(Dispatchers.IO) {
            _alertsState.value = Result.Loading
            _alertsState.value = alertRepository.listAlerts()
        }
    }

    fun clearAlertsState() {
        _alertsState.value = null
    }

    fun updateAlert(alertId: String, isResolved: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            _updateAlertState.value = Result.Loading
            _updateAlertState.value = alertRepository.updateAlert(alertId, isResolved)
        }
    }

    fun clearUpdateAlertState() {
        _updateAlertState.value = null
    }

    fun deleteAlert(alertId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _deleteAlertState.value = Result.Loading
            _deleteAlertState.value = alertRepository.deleteAlert(alertId)
        }
    }

    fun clearDeleteAlertState() {
        _deleteAlertState.value = null
    }
}

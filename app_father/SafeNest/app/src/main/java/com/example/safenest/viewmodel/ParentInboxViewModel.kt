package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.AccessRequestItem
import com.example.safenest.repository.ParentInboxData
import com.example.safenest.repository.ParentInboxRepository
import com.example.safenest.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ParentInboxViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ParentInboxRepository()
    private val _state = MutableStateFlow<Result<ParentInboxData>?>(null)
    val state: StateFlow<Result<ParentInboxData>?> = _state.asStateFlow()

    private val _decisionState = MutableStateFlow<Result<AccessRequestItem>?>(null)
    val decisionState: StateFlow<Result<AccessRequestItem>?> = _decisionState.asStateFlow()

    private val _alertResolutionState = MutableStateFlow<Result<com.example.safenest.network.AlertOut>?>(null)
    val alertResolutionState: StateFlow<Result<com.example.safenest.network.AlertOut>?> = _alertResolutionState.asStateFlow()

    fun load() {
        viewModelScope.launch(Dispatchers.IO) {
            _state.value = Result.Loading
            _state.value = repository.load()
        }
    }

    fun approve(requestId: String, grantedSeconds: Int?) {
        viewModelScope.launch(Dispatchers.IO) {
            _decisionState.value = Result.Loading
            _decisionState.value = repository.approve(requestId, grantedSeconds)
        }
    }

    fun reject(requestId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _decisionState.value = Result.Loading
            _decisionState.value = repository.reject(requestId)
        }
    }

    fun resolveAlert(alertId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _alertResolutionState.value = Result.Loading
            _alertResolutionState.value = repository.resolveAlert(alertId)
        }
    }

    fun clearAlertResolutionState() {
        _alertResolutionState.value = null
    }

    fun clearDecisionState() {
        _decisionState.value = null
    }
}

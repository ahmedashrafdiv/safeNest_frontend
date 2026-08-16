package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.repository.DigitalControlRepository
import com.example.safenest.util.Result
import com.example.safenest.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class VideoHistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val digitalControlRepository = DigitalControlRepository()
    private val sessionManager = SessionManager(application)

    private val _videoHistoryState = MutableStateFlow<Result<List<Map<String, Any>>>?>(null)
    val videoHistoryState: StateFlow<Result<List<Map<String, Any>>>?> = _videoHistoryState.asStateFlow()

    private val _clearVideoHistoryState = MutableStateFlow<Result<Unit>?>(null)
    val clearVideoHistoryState: StateFlow<Result<Unit>?> = _clearVideoHistoryState.asStateFlow()

    fun getVideoHistory(childId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _videoHistoryState.value = Result.Loading
            _videoHistoryState.value = digitalControlRepository.getVideoHistory(childId)
        }
    }

    fun clearVideoHistoryState() {
        _videoHistoryState.value = null
    }

    fun clearVideoHistory(childId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _clearVideoHistoryState.value = Result.Loading
            _clearVideoHistoryState.value = digitalControlRepository.clearVideoHistory(childId)
        }
    }

    fun clearClearVideoHistoryState() {
        _clearVideoHistoryState.value = null
    }

    fun getSelectedChildId(): String? = sessionManager.getSelectedChildId()
}

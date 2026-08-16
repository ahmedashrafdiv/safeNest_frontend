package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.MessageResponse
import com.example.safenest.repository.ParentRepository
import com.example.safenest.util.Result
import com.example.safenest.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val parentRepository = ParentRepository()
    private val sessionManager = SessionManager(application)

    private val _changePasswordState = MutableStateFlow<Result<MessageResponse>?>(null)
    val changePasswordState: StateFlow<Result<MessageResponse>?> = _changePasswordState.asStateFlow()

    private val _deleteAccountState = MutableStateFlow<Result<MessageResponse>?>(null)
    val deleteAccountState: StateFlow<Result<MessageResponse>?> = _deleteAccountState.asStateFlow()

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _changePasswordState.value = Result.Loading
            _changePasswordState.value = parentRepository.changePassword(currentPassword, newPassword)
        }
    }

    fun clearChangePasswordState() {
        _changePasswordState.value = null
    }

    fun deleteAccount() {
        viewModelScope.launch(Dispatchers.IO) {
            _deleteAccountState.value = Result.Loading
            val result = parentRepository.deleteAccount()
            if (result is Result.Success) {
                sessionManager.clearAll()
            }
            _deleteAccountState.value = result
        }
    }

    fun clearDeleteAccountState() {
        _deleteAccountState.value = null
    }
}

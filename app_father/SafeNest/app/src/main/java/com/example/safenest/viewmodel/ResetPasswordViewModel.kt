package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.MessageResponse
import com.example.safenest.repository.AuthRepository
import com.example.safenest.util.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResetPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()

    private val _resetPasswordState = MutableStateFlow<Result<MessageResponse>?>(null)
    val resetPasswordState: StateFlow<Result<MessageResponse>?> = _resetPasswordState.asStateFlow()

    fun resetPassword(email: String, otp: String, newPassword: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _resetPasswordState.value = Result.Loading
            _resetPasswordState.value = authRepository.resetPassword(email, otp, newPassword)
        }
    }

    fun clearResetPasswordState() {
        _resetPasswordState.value = null
    }
}

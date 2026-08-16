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

class ForgotPasswordViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()

    private val _forgotPasswordState = MutableStateFlow<Result<MessageResponse>?>(null)
    val forgotPasswordState: StateFlow<Result<MessageResponse>?> = _forgotPasswordState.asStateFlow()

    fun forgotPassword(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _forgotPasswordState.value = Result.Loading
            _forgotPasswordState.value = authRepository.forgotPassword(email)
        }
    }

    fun clearForgotPasswordState() {
        _forgotPasswordState.value = null
    }
}

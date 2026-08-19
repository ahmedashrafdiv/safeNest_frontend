package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.MessageResponse
import com.example.safenest.network.TokenResponse
import com.example.safenest.repository.AuthRepository
import com.example.safenest.util.Result
import com.example.safenest.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val sessionManager = SessionManager(application)

    // ── Register ──────────────────────────────────────────────────────────────

    private val _registerState = MutableStateFlow<Result<MessageResponse>?>(null)
    val registerState: StateFlow<Result<MessageResponse>?> = _registerState.asStateFlow()

    fun register(name: String, email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _registerState.value = Result.Loading
            _registerState.value = authRepository.register(
                name = name,
                email = email,
                password = password,
                phone = null
            )
        }
    }

    fun clearRegisterState() {
        _registerState.value = null
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    private val _loginState = MutableStateFlow<Result<TokenResponse>?>(null)
    val loginState: StateFlow<Result<TokenResponse>?> = _loginState.asStateFlow()

    fun login(email: String, password: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _loginState.value = Result.Loading
            val result = authRepository.login(email, password)
            if (result is Result.Success) {
                sessionManager.saveToken(result.data.accessToken)
                sessionManager.saveParentId(result.data.parentId)
            }
            _loginState.value = result
        }
    }

    fun clearLoginState() {
        _loginState.value = null
    }
}

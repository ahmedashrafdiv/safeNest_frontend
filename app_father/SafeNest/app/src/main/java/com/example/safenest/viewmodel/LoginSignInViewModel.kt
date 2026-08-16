package com.example.safenest.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.safenest.network.TokenResponse
import com.example.safenest.repository.AuthRepository
import com.example.safenest.repository.ParentRepository
import com.example.safenest.util.Result
import com.example.safenest.util.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LoginSignInViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val parentRepository = ParentRepository()
    private val sessionManager = SessionManager(application)

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

    fun sendFcmToken(token: String) {
        viewModelScope.launch {
            try { parentRepository.updateFcmToken(token) } catch (_: Exception) { }
        }
    }
}

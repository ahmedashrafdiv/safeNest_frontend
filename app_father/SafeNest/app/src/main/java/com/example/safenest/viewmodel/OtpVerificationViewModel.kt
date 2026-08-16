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

class OtpVerificationViewModel(application: Application) : AndroidViewModel(application) {

    private val authRepository = AuthRepository()
    private val sessionManager = SessionManager(application)

    // verify email
    private val _verifyState = MutableStateFlow<Result<MessageResponse>?>(null)
    val verifyState: StateFlow<Result<MessageResponse>?> = _verifyState.asStateFlow()

    // resend OTP
    private val _resendState = MutableStateFlow<Result<MessageResponse>?>(null)
    val resendState: StateFlow<Result<MessageResponse>?> = _resendState.asStateFlow()

    // auto-login after verification
    private val _loginState = MutableStateFlow<Result<TokenResponse>?>(null)
    val loginState: StateFlow<Result<TokenResponse>?> = _loginState.asStateFlow()

    fun verifyEmail(email: String, otp: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _verifyState.value = Result.Loading
            _verifyState.value = authRepository.verifyEmail(email, otp)
        }
    }

    fun clearVerifyState() {
        _verifyState.value = null
    }

    fun resendOtp(email: String) {
        viewModelScope.launch(Dispatchers.IO) {
            _resendState.value = Result.Loading
            _resendState.value = authRepository.resendOtp(email)
        }
    }

    fun clearResendState() {
        _resendState.value = null
    }

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

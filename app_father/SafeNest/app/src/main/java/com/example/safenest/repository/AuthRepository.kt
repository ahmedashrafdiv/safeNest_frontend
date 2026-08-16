package com.example.safenest.repository

import com.example.safenest.network.ApiClient
import com.example.safenest.network.EmailVerificationRequest
import com.example.safenest.network.ForgotPasswordRequest
import com.example.safenest.network.LoginRequest
import com.example.safenest.network.MessageResponse
import com.example.safenest.network.RegisterRequest
import com.example.safenest.network.ResendOtpRequest
import com.example.safenest.network.ResetPasswordRequest
import com.example.safenest.network.TokenResponse
import com.example.safenest.util.Result

class AuthRepository : BaseRepository() {

    private val api = ApiClient.apiService

    suspend fun login(email: String, password: String): Result<TokenResponse> =
        safeApiCall { api.login(LoginRequest(email = email, password = password)) }

    suspend fun register(
        name: String,
        email: String,
        password: String,
        phone: String? = null
    ): Result<MessageResponse> =
        safeApiCall {
            api.register(
                RegisterRequest(
                    name = name,
                    email = email,
                    password = password,
                    phoneNumber = phone
                )
            )
        }

    suspend fun verifyEmail(email: String, otp: String): Result<MessageResponse> =
        safeApiCall { api.verifyEmail(EmailVerificationRequest(email = email, otp = otp)) }

    suspend fun resendOtp(email: String): Result<MessageResponse> =
        safeApiCall { api.resendOtp(ResendOtpRequest(email = email)) }

    suspend fun forgotPassword(email: String): Result<MessageResponse> =
        safeApiCall { api.forgotPassword(ForgotPasswordRequest(email = email)) }

    suspend fun resetPassword(
        email: String,
        otp: String,
        newPassword: String
    ): Result<MessageResponse> =
        safeApiCall {
            api.resetPassword(
                ResetPasswordRequest(
                    email = email,
                    otp = otp,
                    newPassword = newPassword
                )
            )
        }
}

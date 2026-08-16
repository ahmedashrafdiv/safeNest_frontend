package com.example.safenest.repository

import com.example.safenest.network.ApiClient
import com.example.safenest.network.ChangePasswordRequest
import com.example.safenest.network.FCMTokenUpdateRequest
import com.example.safenest.network.MessageResponse
import com.example.safenest.network.ParentResponse
import com.example.safenest.network.ParentUpdateRequest
import com.example.safenest.util.Result

class ParentRepository : BaseRepository() {

    private val api = ApiClient.apiService

    suspend fun getProfile(): Result<ParentResponse> =
        safeApiCall { api.getProfile() }

    suspend fun updateProfile(name: String, phoneNumber: String?): Result<ParentResponse> =
        safeApiCall { api.updateProfile(ParentUpdateRequest(name = name, phoneNumber = phoneNumber)) }

    suspend fun changePassword(currentPassword: String, newPassword: String): Result<MessageResponse> =
        safeApiCall { api.changePassword(ChangePasswordRequest(currentPassword = currentPassword, newPassword = newPassword)) }

    suspend fun deleteAccount(): Result<MessageResponse> =
        safeApiCall { api.deleteAccount() }

    suspend fun updateFcmToken(fcmToken: String): Result<MessageResponse> =
        safeApiCall { api.updateFcmToken(FCMTokenUpdateRequest(fcmToken = fcmToken)) }
}

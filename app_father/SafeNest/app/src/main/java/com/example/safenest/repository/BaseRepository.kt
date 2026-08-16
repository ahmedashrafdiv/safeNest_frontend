package com.example.safenest.repository

import com.example.safenest.network.ApiClient
import com.example.safenest.util.Result
import retrofit2.Response

abstract class BaseRepository {

    protected suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> {
        return try {
            val response = call()
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null) {
                    Result.Success(body)
                } else {
                    Result.Error("Empty response")
                }
            } else {
                val errorMessage = ApiClient.parseError(response.errorBody()?.string())
                Result.Error(errorMessage)
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Network error")
        }
    }
}

package com.example.safenest.network

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private var appContext: Context? = null
    private const val PREFS_NAME = "SafeNestPrefs"
    private const val KEY_ACCESS_TOKEN = "access_token"

    // Call this once in your Application class or MainActivity.onCreate()
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    // Auth interceptor: automatically adds Bearer token to every request
    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val token = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.getString(KEY_ACCESS_TOKEN, null)

        val requestBuilder = original.newBuilder()
        if (token != null) {
            requestBuilder.header("Authorization", "Bearer $token")
        }
        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(SafeNestApiService.BASE_URL)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: SafeNestApiService = retrofit.create(SafeNestApiService::class.java)

    // Helper to parse error responses
    fun parseError(errorBody: String?): String {
        return try {
            val error = Gson().fromJson(errorBody, ErrorResponse::class.java)
            error.getMessage()
        } catch (e: Exception) {
            errorBody ?: "Unknown error occurred"
        }
    }

    // Convenience: get saved token
    fun getToken(): String? {
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.getString(KEY_ACCESS_TOKEN, null)
    }

    // Convenience: get saved parent ID
    fun getParentId(): String? {
        return appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.getString("parent_id", null)
    }

    // Clear saved credentials (used on logout)
    fun clearCredentials() {
        appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            ?.edit()?.clear()?.apply()
    }
}

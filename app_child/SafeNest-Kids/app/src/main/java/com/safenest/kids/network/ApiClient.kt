package com.safenest.kids.network

import android.content.Context
import android.util.Log
import com.safenest.kids.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import com.safenest.kids.util.PrefsHelper

object ApiClient {
    private const val TAG = "ApiClient"
    private val BASE_URL = BuildConfig.API_BASE_URL

    /** Paths that must remain unauthenticated (no Bearer token). */
    private val UNAUTHENTICATED_PATHS = listOf(
        "/api/devices/link-device",
        "/api/devices/generate-pin",
        "/api/child-devices/claim",
    )

    private lateinit var prefsHelper: PrefsHelper

    /**
     * Must be called once before any network calls are made â€” typically from
     * [MainActivity.onCreate] or a custom Application class.
     */
    fun init(context: Context) {
        prefsHelper = PrefsHelper(context.applicationContext)
    }

    /** Interceptor that attaches the device access token to authenticated requests. */
    private val authInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val path = originalRequest.url.encodedPath

        // Skip auth for unauthenticated endpoints
        if (UNAUTHENTICATED_PATHS.any { path.endsWith(it) }) {
            return@Interceptor chain.proceed(originalRequest)
        }

        // Attempt to attach token
        val token: String? = if (::prefsHelper.isInitialized) {
            prefsHelper.getDeviceToken()
        } else {
            Log.w(TAG, "ApiClient.init() has not been called yet â€” skipping auth header")
            null
        }

        if (token != null) {
            val authenticatedRequest = originalRequest.newBuilder()
                .header("Authorization", "Bearer $token")
                .build()
            chain.proceed(authenticatedRequest)
        } else {
            Log.w(TAG, "No device access token available â€” sending request without Authorization header")
            chain.proceed(originalRequest)
        }
    }

    private val okHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val apiService: KidsApiService by lazy {
        retrofit.create(KidsApiService::class.java)
    }

    fun parseError(errorBody: String?): String {
        if (errorBody.isNullOrEmpty()) return "Unknown server error"
        return try {
            val jsonObject = JSONObject(errorBody)
            jsonObject.optString("message", "Unknown server error")
        } catch (e: Exception) {
            "Unknown server error"
        }
    }
}

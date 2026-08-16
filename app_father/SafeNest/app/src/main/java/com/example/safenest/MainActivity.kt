package com.example.safenest

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import com.example.safenest.fragments.HomeFragment
import com.example.safenest.fragments.LoginFragment
import com.example.safenest.network.ApiClient
import com.example.safenest.network.FCMTokenUpdateRequest
import com.example.safenest.util.SessionManager
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize ApiClient so the auth interceptor can read the token
        ApiClient.init(this)

        // Temporary FCM token logger for testing
        com.google.firebase.messaging.FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    android.util.Log.d("FCM_TOKEN", "Token: ${task.result}")
                } else {
                    android.util.Log.e("FCM_TOKEN", "Failed: ${task.exception?.message}")
                }
            }

        // Check if user is already logged in
        if (SessionManager(this).isLoggedIn()) {
            showHomeFragment()
            // Start periodic location sync
            scheduleLocationSync()
            // Proactively push current FCM token to server
            refreshAndPushFcmToken()
        } else {
            showLoginFragment()
        }
    }

    private fun showLoginFragment() {
        val loginFragment = LoginFragment()
        replaceFragment(loginFragment)
    }

    private fun showHomeFragment() {
        val homeFragment = HomeFragment()
        replaceFragment(homeFragment)
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    // Navigate between fragments (adds to backstack)
    fun navigateToFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    // Go to home after successful login (clears back stack)
    fun goToHome() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        showHomeFragment()
        scheduleLocationSync()
        refreshAndPushFcmToken()
    }

    // Go back to login (for logout or 401)
    fun goToLogin() {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        showLoginFragment()
    }

    // Alias used in some fragments for logout
    fun logout() {
        WorkManager.getInstance(this).cancelUniqueWork(LocationSyncWorker.WORK_NAME)
        ApiClient.clearCredentials()
        goToLogin()
    }

    // Navigate to a fragment after authentication (clears back stack)
    fun goToHomeAfterAuth(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        replaceFragment(fragment)
    }

    // Schedule periodic location sync via WorkManager
    private fun scheduleLocationSync() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val workRequest = PeriodicWorkRequest.Builder(
            LocationSyncWorker::class.java,
            15, TimeUnit.MINUTES  // Minimum period allowed by WorkManager
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this)
            .enqueueUniquePeriodicWork(
                LocationSyncWorker.WORK_NAME,
                androidx.work.ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
        Log.d(TAG, "Location sync worker scheduled")
    }

    // Push current FCM token to server proactively on app start / after login
    private fun refreshAndPushFcmToken() {
        if (!SessionManager(this).isLoggedIn()) {
            Log.d(TAG, "Not logged in — skipping FCM token push on startup")
            return
        }
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w(TAG, "FCM token fetch failed: ${task.exception?.message}")
                return@addOnCompleteListener
            }
            val token = task.result
            Log.d(TAG, "FCM token obtained: ${token.take(10)}...")
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request = FCMTokenUpdateRequest(fcmToken = token)
                    val response = ApiClient.apiService.updateFcmToken(request)
                    if (response.isSuccessful) {
                        Log.d(TAG, "FCM token pushed on startup successfully")
                    } else {
                        Log.w(TAG, "FCM token push on startup failed: ${response.code()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error pushing FCM token on startup: ${e.message}", e)
                }
            }
        }
    }
}
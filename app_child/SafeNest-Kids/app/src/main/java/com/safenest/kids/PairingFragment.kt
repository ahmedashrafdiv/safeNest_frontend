package com.safenest.kids

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.messaging.FirebaseMessaging
import com.safenest.kids.network.ApiClient
import com.safenest.kids.network.LinkDeviceRequest
import com.safenest.kids.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PairingFragment : Fragment() {

    private lateinit var etPinCode: EditText
    private lateinit var btnLink: Button
    private lateinit var tvError: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var prefsHelper: PrefsHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_pairing, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefsHelper = PrefsHelper(requireContext())

        etPinCode = view.findViewById(R.id.et_pin_code)
        btnLink = view.findViewById(R.id.btn_link)
        tvError = view.findViewById(R.id.tv_error)
        progressBar = view.findViewById(R.id.progress_bar)

        btnLink.setOnClickListener {
            val pin = etPinCode.text.toString().trim()
            if (pin.length != 6) {
                tvError.text = "يرجى إدخال كود مكون من 6 أرقام"
                tvError.visibility = View.VISIBLE
                return@setOnClickListener
            }

            tvError.visibility = View.GONE
            btnLink.isEnabled = false
            progressBar.visibility = View.VISIBLE

            // ── Step 1: Retrieve FCM token BEFORE calling link-device ──
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.e("Pairing", "Fetching FCM token failed", task.exception)
                    // Still on main thread (addOnCompleteListener default)
                    showError("فشل الحصول على رمز الإشعارات. حاول مرة أخرى.")
                    return@addOnCompleteListener
                }

                val fcmToken = task.result
                Log.e("Pairing", "FCM token retrieved for pairing: $fcmToken")

                // ── Step 2: Build the request with the real FCM token ──
                val deviceId = prefsHelper.getDeviceId()
                val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"
                val request = LinkDeviceRequest(
                    pinCode = pin,
                    deviceId = deviceId,
                    deviceName = deviceName,
                    deviceType = "Smartphone",
                    fcmToken = fcmToken
                )

                Log.e("Pairing", "link-device request body: pin_code=$pin, device_id=$deviceId, device_name=$deviceName, device_type=Smartphone, fcm_token=$fcmToken")

                // ── Step 3: Fire the network call only after token is ready ──
                lifecycleScope.launch(Dispatchers.IO) {
                    try {
                        val response = ApiClient.apiService.linkDevice(request)
                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                val body = response.body()
                                if (body != null && body.success) {
                                    prefsHelper.setChildId(body.childId)
                                    prefsHelper.setParentId(body.parentId)
                                    prefsHelper.setPaired(true)
                                    prefsHelper.setJustPaired(true)
                                    prefsHelper.setLastAppsSent(false)

                                    if (body.accessToken != null) {
                                        prefsHelper.setDeviceToken(body.accessToken)
                                    } else {
                                        Log.w("Pairing", "link-device response did not include access_token — backend update may not be live yet")
                                    }

                                    Toast.makeText(requireContext(), "تم الربط بنجاح!", Toast.LENGTH_SHORT).show()

                                    parentFragmentManager.beginTransaction()
                                        .replace(R.id.fragment_container, PermissionsFragment())
                                        .commit()
                                } else {
                                    showError(body?.message ?: "الكود غير صحيح أو انتهت صلاحيته")
                                }
                            } else {
                                val errorBody = response.errorBody()?.string()
                                val errorMsg = ApiClient.parseError(errorBody)
                                showError(errorMsg)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            showError("حدث خطأ في الاتصال. حاول مرة أخرى.")
                        }
                    }
                }
            }
        }
    }

    private fun showError(message: String) {
        tvError.text = message
        tvError.visibility = View.VISIBLE
        btnLink.isEnabled = true
        progressBar.visibility = View.GONE
    }
}

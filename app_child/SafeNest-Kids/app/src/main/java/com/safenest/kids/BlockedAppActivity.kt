package com.safenest.kids

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.safenest.kids.network.ApiClient
import com.safenest.kids.security.ParentSettingsAccessDecider
import com.safenest.kids.util.PrefsHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BlockedAppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ApiClient.init(this)
        setContentView(R.layout.activity_blocked_app)

        val reason = intent.getStringExtra("blocked_reason") ?: "blocked"
        val title = findViewById<TextView>(R.id.tv_blocked_title)
        val subtitle = findViewById<TextView>(R.id.tv_blocked_subtitle)
        val parentAuthorize = findViewById<Button>(R.id.btn_parent_authorize)

        when (reason) {
            "uninstall_protection" -> {
                title.text = "يلزم تفويض ولي الأمر"
                subtitle.text = "حذف Layngo محمي على هذا الجهاز. اطلب موافقة ولي الأمر."
            }
            "protection_settings" -> {
                title.text = "إعدادات الحماية مقفلة"
                subtitle.text = "لا يمكن تغيير صلاحيات Layngo إلا بموافقة ولي الأمر."
                parentAuthorize.visibility = android.view.View.VISIBLE
                parentAuthorize.setOnClickListener {
                    requestParentAuthorization(ParentControlAction.OPEN_ACCESSIBILITY_SETTINGS)
                }
            }
            "child_app_launch_protection" -> {
                title.text = "إعدادات Layngo محمية"
                subtitle.text = "يمكن لولي الأمر فقط فتح إعدادات الجهاز اللازمة للحماية."
                parentAuthorize.visibility = android.view.View.VISIBLE
                parentAuthorize.setOnClickListener {
                    requestParentAuthorization(ParentControlAction.OPEN_LOCATION_SETTINGS)
                }
            }
            "protected_home_long_press", "protected_home_launch" -> {
                title.text = "حماية Layngo مفعّلة"
                subtitle.text = "تحتاج إدارة Layngo إلى موافقة ولي الأمر."
            }
            "protected_home_policy" -> {
                title.text = "هذا التطبيق غير متاح الآن"
                subtitle.text = "تُطبّق Layngo قواعد الحماية التي وضعها ولي الأمر."
            }
            "time_limit" -> {
                title.text = "انتهى وقت هذا التطبيق"
                subtitle.text = "لقد استنفدت الوقت المخصص لهذا التطبيق اليوم."
            }
            "allowlist" -> {
                title.text = "هذا التطبيق غير مسموح"
                subtitle.text = "يسمح ولي الأمر بتطبيقات محددة فقط على هذا الجهاز."
            }
            else -> {
                title.text = "هذا التطبيق محظور"
                subtitle.text = "تواصل مع ولي الأمر لمزيد من المعلومات."
            }
        }

        supportFragmentManager.setFragmentResultListener(
            ParentVerificationDialog.REQUEST_KEY,
            this,
        ) { _, result ->
            when (result.getString(ParentVerificationDialog.BUNDLE_ACTION)) {
                ParentControlAction.OPEN_LOCATION_SETTINGS.name -> openLocationSettingsForVerifiedParent()
                ParentControlAction.OPEN_ACCESSIBILITY_SETTINGS.name -> openAccessibilitySettingsForVerifiedParent()
            }
        }
    }

    override fun onBackPressed() {
        startActivity(Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
        finish()
    }

    private fun requestParentAuthorization(action: ParentControlAction) {
        val prefs = PrefsHelper(this)
        val cachedEmail = prefs.getParentEmail()
        if (!cachedEmail.isNullOrBlank()) {
            showParentVerification(action, cachedEmail)
            return
        }

        val button = findViewById<Button>(R.id.btn_parent_authorize)
        val subtitle = findViewById<TextView>(R.id.tv_blocked_subtitle)
        button.isEnabled = false
        subtitle.text = "جارِ التحقق من حساب ولي الأمر…"
        lifecycleScope.launch(Dispatchers.IO) {
            val parentEmail = runCatching {
                val response = ApiClient.apiService.getSessionProfile(prefs.getDeviceId())
                response.body()?.parentEmail?.takeIf { response.isSuccessful && it.isNotBlank() }
            }.getOrNull()

            withContext(Dispatchers.Main) {
                if (isFinishing || isDestroyed) return@withContext
                button.isEnabled = true
                if (parentEmail.isNullOrBlank()) {
                    subtitle.text = "تعذر الاتصال بحساب ولي الأمر. تأكد من الإنترنت ثم حاول مرة أخرى."
                } else {
                    prefs.setParentEmail(parentEmail)
                    showParentVerification(action, parentEmail)
                }
            }
        }
    }

    private fun showParentVerification(action: ParentControlAction, parentEmail: String) {
        ParentVerificationDialog.newInstance(action, parentEmail)
            .show(supportFragmentManager, ParentVerificationDialog.TAG)
    }

    private fun openLocationSettingsForVerifiedParent() {
        grantTemporarySettingsAccess()
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }

    private fun openAccessibilitySettingsForVerifiedParent() {
        grantTemporarySettingsAccess()
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
    }

    private fun grantTemporarySettingsAccess() {
        PrefsHelper(this).setParentSettingsAccessUntil(
            ParentSettingsAccessDecider.expiresAt(System.currentTimeMillis()),
        )
    }
}

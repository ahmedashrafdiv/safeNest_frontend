package com.safenest.kids

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class BlockedAppActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_blocked_app)

        // ── Show reason-specific messaging ────────────────────────
        val reason = intent.getStringExtra("blocked_reason") ?: "blocked"
        val tvTitle = findViewById<TextView>(R.id.tv_blocked_title)
        val tvSubtitle = findViewById<TextView>(R.id.tv_blocked_subtitle)

        if (reason == "time_limit") {
            tvTitle.text = "انتهى وقت هذا التطبيق"
            tvSubtitle.text = "لقد استنفدت الوقت المخصص لهذا التطبيق اليوم"
            tvTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.orange_accent))
        } else if (reason == "allowlist") {
            tvTitle.text = "هذا التطبيق غير مسموح"
            tvSubtitle.text = "يسمح ولي الأمر بتطبيقات محددة فقط على هذا الجهاز"
            tvTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.red_warning))
        } else {
            tvTitle.text = "هذا التطبيق محظور"
            tvSubtitle.text = "تواصل مع ولي الأمر لمزيد من المعلومات"
            tvTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.red_warning))
        }

        // Auto-return to home after 2 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(homeIntent)
            finish()
        }, 2000)
    }

    // Prevent the user from pressing back to re-enter the blocked app
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        startActivity(homeIntent)
        finish()
    }
}

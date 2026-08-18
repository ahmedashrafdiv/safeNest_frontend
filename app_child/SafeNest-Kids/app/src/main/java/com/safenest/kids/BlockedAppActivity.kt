package com.safenest.kids

import android.content.Intent
import android.os.Bundle
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

        if (reason == "uninstall_protection") {
            tvTitle.text = "Parent authorization is required"
            tvSubtitle.text = "Layngo removal is protected on this device. Ask a parent to approve recovery."
            tvTitle.setTextColor(androidx.core.content.ContextCompat.getColor(this, R.color.red_warning))
        } else if (reason == "time_limit") {
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

        // Keep the parent-authorized Layngo block screen visible until the child leaves it.
        // Back still routes to the launcher below rather than reopening the blocked package.
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

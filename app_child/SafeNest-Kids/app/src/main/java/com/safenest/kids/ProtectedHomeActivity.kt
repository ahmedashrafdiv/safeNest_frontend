package com.safenest.kids

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.safenest.kids.security.ProtectedHomeDecider
import com.safenest.kids.security.ProtectedHomeLongPressAction
import com.safenest.kids.service.AppPolicyDecider
import com.safenest.kids.util.InstalledAppsHelper
import com.safenest.kids.util.PrefsHelper

/** Layngo-owned HOME-role activity where Layngo can safely control its own long-press behavior. */
class ProtectedHomeActivity : AppCompatActivity() {

    private lateinit var prefs: PrefsHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = PrefsHelper(this)
        setContentView(createHomeContent())
    }

    override fun onResume() {
        super.onResume()
        (findViewById<GridLayout>(HOME_GRID_ID))?.let { grid ->
            grid.removeAllViews()
            populateAppTiles(grid)
        }
    }

    private fun createHomeContent(): View {
        val root = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#FFFCF7"))
            isFillViewport = true
        }
        val content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(28), dp(20), dp(36))
        }
        root.addView(content)

        content.addView(TextView(this).apply {
            text = "Layngo Protected Home"
            setTextColor(Color.parseColor("#15385F"))
            textSize = 24f
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        })
        content.addView(TextView(this).apply {
            text = "حماية الشاشة الرئيسية مفعّلة"
            setTextColor(Color.parseColor("#2CA39D"))
            textSize = 16f
            setPadding(0, dp(6), 0, dp(24))
        })

        val grid = GridLayout(this).apply {
            id = HOME_GRID_ID
            columnCount = 4
            alignmentMode = GridLayout.ALIGN_MARGINS
            useDefaultMargins = false
        }
        content.addView(grid)
        populateAppTiles(grid)
        return root
    }

    private fun populateAppTiles(grid: GridLayout) {
        val ownTile = HomeTile(
            packageName = packageName,
            label = getString(R.string.app_name),
            icon = applicationInfo.loadIcon(packageManager),
        )
        addTile(grid, ownTile)
        InstalledAppsHelper.getInstalledApps(this).forEach { (appPackage, label) ->
            val icon = runCatching { packageManager.getApplicationIcon(appPackage) }.getOrNull() ?: return@forEach
            addTile(grid, HomeTile(appPackage, label, icon))
        }
    }

    private fun addTile(grid: GridLayout, tile: HomeTile) {
        val tileView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            isClickable = true
            isFocusable = true
            setPadding(dp(6), dp(10), dp(6), dp(10))
            background = roundedBackground("#FFFFFF", "#E7DED2")
            contentDescription = tile.label
            setOnClickListener { openTile(tile) }
            setOnLongClickListener {
                when (ProtectedHomeDecider.longPressAction(tile.packageName, packageName)) {
                    ProtectedHomeLongPressAction.BLOCK_LAYNGO -> showLayngoProtection("protected_home_long_press")
                    ProtectedHomeLongPressAction.SHOW_APP_ACTIONS -> showOtherAppActions(tile, this)
                }
                true
            }
        }
        tileView.addView(ImageView(this).apply {
            setImageDrawable(tile.icon)
            contentDescription = null
            layoutParams = LinearLayout.LayoutParams(dp(48), dp(48))
        })
        tileView.addView(TextView(this).apply {
            text = tile.label
            textSize = 11f
            gravity = Gravity.CENTER
            maxLines = 2
            setTextColor(Color.parseColor("#15385F"))
            setPadding(0, dp(7), 0, 0)
        })
        grid.addView(tileView, GridLayout.LayoutParams().apply {
            width = (resources.displayMetrics.widthPixels - dp(64)) / 4
            height = GridLayout.LayoutParams.WRAP_CONTENT
            setMargins(dp(4), dp(4), dp(4), dp(4))
        })
    }

    private fun openTile(tile: HomeTile) {
        if (tile.packageName == packageName) {
            showLayngoProtection("protected_home_launch")
            return
        }
        if (AppPolicyDecider.shouldBlock(
                packageName = tile.packageName,
                childPackage = packageName,
                mode = prefs.getAppControlMode(),
                allowedPackages = prefs.getAllowedApps(),
                blockedPackages = prefs.getBlockedApps(),
            )
        ) {
            startActivity(Intent(this, BlockedAppActivity::class.java).apply {
                putExtra("blocked_package", tile.packageName)
                putExtra("blocked_reason", "protected_home_policy")
            })
            return
        }
        packageManager.getLaunchIntentForPackage(tile.packageName)?.let(::startActivity)
    }

    private fun showOtherAppActions(tile: HomeTile, anchor: View) {
        PopupMenu(this, anchor).apply {
            menu.add(MENU_OPEN, MENU_OPEN, 0, "فتح التطبيق")
            menu.add(MENU_APP_INFO, MENU_APP_INFO, 1, "معلومات التطبيق")
            setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    MENU_OPEN -> openTile(tile)
                    MENU_APP_INFO -> startActivity(Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${tile.packageName}")
                    })
                }
                true
            }
            show()
        }
    }

    private fun showLayngoProtection(reason: String) {
        startActivity(Intent(this, BlockedAppActivity::class.java).apply {
            putExtra("blocked_package", packageName)
            putExtra("blocked_reason", reason)
        })
    }

    private fun roundedBackground(fill: String, stroke: String): GradientDrawable = GradientDrawable().apply {
        setColor(Color.parseColor(fill))
        setStroke(dp(1), Color.parseColor(stroke))
        cornerRadius = dp(18).toFloat()
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private data class HomeTile(
        val packageName: String,
        val label: String,
        val icon: android.graphics.drawable.Drawable,
    )

    private companion object {
        const val HOME_GRID_ID = 0x6A790001
        const val MENU_OPEN = 1
        const val MENU_APP_INFO = 2
    }
}

package com.example.safenest.util

import android.content.Context
import android.content.SharedPreferences

class SessionManager(context: Context) {

    companion object {
        private const val PREFS_NAME = "SafeNestPrefs"

        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_PARENT_ID = "parent_id"
        private const val KEY_SELECTED_CHILD_ID = "selected_child_id"
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // ── Token ────────────────────────────────────────────────────────────────

    fun saveToken(token: String) {
        prefs.edit().putString(KEY_ACCESS_TOKEN, token).apply()
    }

    fun getToken(): String? = prefs.getString(KEY_ACCESS_TOKEN, null)

    fun isLoggedIn(): Boolean = !getToken().isNullOrEmpty()

    // ── Parent ID ─────────────────────────────────────────────────────────────

    fun saveParentId(parentId: String) {
        prefs.edit().putString(KEY_PARENT_ID, parentId).apply()
    }

    fun getParentId(): String? = prefs.getString(KEY_PARENT_ID, null)

    // ── Selected Child ID ─────────────────────────────────────────────────────

    fun saveSelectedChildId(childId: String) {
        prefs.edit().putString(KEY_SELECTED_CHILD_ID, childId).apply()
    }

    fun getSelectedChildId(): String? = prefs.getString(KEY_SELECTED_CHILD_ID, null)

    // ── Logout ────────────────────────────────────────────────────────────────

    fun clearAll() {
        prefs.edit().clear().apply()
    }
}

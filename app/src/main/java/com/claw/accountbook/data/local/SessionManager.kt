package com.claw.accountbook.data.local

import android.content.Context
import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Session 管理器
 * 使用 SharedPreferences 持久化登录状态，应用重启后自动恢复
 */
@Singleton
class SessionManager @Inject constructor(
    private val context: Context
) {
    companion object {
        private const val PREF_NAME = "account_book_session"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_USERNAME = "username"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * 保存登录状态
     */
    fun saveLoginSession(userId: Long, username: String) {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .putLong(KEY_USER_ID, userId)
            .putString(KEY_USERNAME, username)
            .apply()
    }

    /**
     * 清除登录状态（退出登录）
     */
    fun clearSession() {
        prefs.edit()
            .putBoolean(KEY_IS_LOGGED_IN, false)
            .remove(KEY_USER_ID)
            .remove(KEY_USERNAME)
            .apply()
    }

    /**
     * 是否已登录
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * 获取已保存的用户 ID（未登录返回 -1）
     */
    fun getSavedUserId(): Long {
        return prefs.getLong(KEY_USER_ID, -1L)
    }

    /**
     * 获取已保存的用户名
     */
    fun getSavedUsername(): String? {
        return prefs.getString(KEY_USERNAME, null)
    }
}

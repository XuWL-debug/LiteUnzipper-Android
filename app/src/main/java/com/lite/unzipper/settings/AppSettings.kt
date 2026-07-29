package com.lite.unzipper.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

class AppSettings(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val darkMode: DarkMode
        get() = DarkMode.fromValue(prefs.getInt(KEY_DARK_MODE, DarkMode.SYSTEM.value))

    var threadCount: Int
        get() = prefs.getInt(KEY_THREAD_COUNT, DEFAULT_THREAD_COUNT)
        set(value) { prefs.edit().putInt(KEY_THREAD_COUNT, value.coerceIn(1, 4)).apply() }

    fun applyDarkMode() {
        AppCompatDelegate.setDefaultNightMode(darkMode.delegateMode)
    }

    fun setDarkMode(mode: DarkMode) {
        prefs.edit().putInt(KEY_DARK_MODE, mode.value).apply()
        AppCompatDelegate.setDefaultNightMode(mode.delegateMode)
    }

    companion object {
        private const val PREFS_NAME = "lite_unzipper_prefs"
        private const val KEY_DARK_MODE = "dark_mode"
        private const val KEY_THREAD_COUNT = "thread_count"
        val DEFAULT_THREAD_COUNT: Int = Runtime.getRuntime().availableProcessors().coerceIn(1, 4)
    }
}

enum class DarkMode(val value: Int, val delegateMode: Int, val label: String) {
    SYSTEM(0, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, "跟随系统"),
    LIGHT(1, AppCompatDelegate.MODE_NIGHT_NO, "浅色"),
    DARK(2, AppCompatDelegate.MODE_NIGHT_YES, "深色");

    companion object {
        fun fromValue(value: Int): DarkMode = entries.firstOrNull { it.value == value } ?: SYSTEM
    }
}

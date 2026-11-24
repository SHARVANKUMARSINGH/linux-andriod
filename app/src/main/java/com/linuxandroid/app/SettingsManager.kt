package com.linuxandroid.app

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import java.security.SecureRandom
import java.util.Locale

/**
 * Settings Manager for persistent configuration storage
 */
class SettingsManager(private val context: Context) {
    companion object {
        private const val PREFS_NAME = "LinuxAndroidSettings"
        private const val LANGUAGE = "language"
        private const val THEME = "theme_mode"
        private const val TURBOEMU_MODE = "turboemu_mode"
        private const val AUTO_UPDATE = "auto_update"
        private const val DOWNLOAD_DIRECTORY = "download_directory"
        private const val UPLOAD_ENABLED = "upload_enabled"
        private const val ALLOW_ABOUT_UI = "allow_about_ui"
        private const val COLLECTION_LOGGING = "collection_logging"

        val DEFAULT_LANGUAGE: String = Locale.getDefault().language
        val DEFAULT_THEME = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        const val DEFAULT_TURBOEMU = true
        const val DEFAULT_AUTO_UPDATE = true
        const val DEFAULT_DOWNLOAD_DIRECTORY = "downloads/"
        const val DEFAULT_UPLOAD_ENABLED = true
        const val DEFAULT_ALLOW_ABOUT_UI = true
        const val DEFAULT_COLLECTION_LOGGING = false
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLanguage(): String {
        return prefs.getString(LANGUAGE, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE
    }

    fun setLanguage(language: String) {
        prefs.edit().putString(LANGUAGE, language).apply()
    }

    fun getThemeMode(): Int {
        return prefs.getInt(THEME, DEFAULT_THEME)
    }

    fun setThemeMode(mode: Int) {
        prefs.edit().putInt(THEME, mode).apply()
        updateThemeMode(mode)
    }

    fun getTurboEMU(): Boolean {
        return prefs.getBoolean(TURBOEMU_MODE, DEFAULT_TURBOEMU)
    }

    fun setTurboEMU(enabled: Boolean) {
        prefs.edit().putBoolean(TURBOEMU_MODE, enabled).apply()
    }

    fun getAutoUpdate(): Boolean {
        return prefs.getBoolean(AUTO_UPDATE, DEFAULT_AUTO_UPDATE)
    }

    fun setAutoUpdate(enabled: Boolean) {
        prefs.edit().putBoolean(AUTO_UPDATE, enabled).apply()
    }

    fun getDownloadDirectory(): String {
        return prefs.getString(DOWNLOAD_DIRECTORY, DEFAULT_DOWNLOAD_DIRECTORY) ?: DEFAULT_DOWNLOAD_DIRECTORY
    }

    fun setDownloadDirectory(directory: String) {
        prefs.edit().putString(DOWNLOAD_DIRECTORY, directory).apply()
    }

    fun isUploadEnabled(): Boolean {
        return prefs.getBoolean(UPLOAD_ENABLED, DEFAULT_UPLOAD_ENABLED)
    }

    fun setUploadEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(UPLOAD_ENABLED, enabled).apply()
    }

    fun isAllowAboutUI(): Boolean {
        return prefs.getBoolean(ALLOW_ABOUT_UI, DEFAULT_ALLOW_ABOUT_UI)
    }

    fun setAllowAboutUI(allowed: Boolean) {
        prefs.edit().putBoolean(ALLOW_ABOUT_UI, allowed).apply()
    }

    fun isCollectionLogging(): Boolean {
        return prefs.getBoolean(COLLECTION_LOGGING, DEFAULT_COLLECTION_LOGGING)
    }

    fun setCollectionLogging(enabled: Boolean) {
        prefs.edit().putBoolean(COLLECTION_LOGGING, enabled).apply()
    }

    private fun updateThemeMode(mode: Int) {
        when (mode) {
            AppCompatDelegate.MODE_NIGHT_YES -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            AppCompatDelegate.MODE_NIGHT_NO -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
        Log.d("SettingsManager", "Theme mode updated to $mode")
    }

    fun clearAllSettings() {
        prefs.edit().clear().apply()
    }

    fun generateApiKey(): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        val sb = StringBuilder(16)
        val random = SecureRandom()
        for (i in 0 until 16) {
            sb.append(chars[random.nextInt(chars.length)])
        }
        return sb.toString()
    }
}

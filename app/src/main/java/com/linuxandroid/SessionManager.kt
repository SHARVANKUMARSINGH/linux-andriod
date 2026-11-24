package com.linuxandroid

import android.content.Context
import android.content.SharedPreferences
import java.io.File

/**
 * Session Manager for storing Linux container session setup data
 */
class SessionManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "LinuxAndroidSession"
        private const val DISTRO_NAME = "distro_name"
        private const val ARCHITECTURE = "architecture"
        private const val DESKTOP_ENV = "desktop_env"
        private const val BROWSER = "browser"
        private const val WINDOW_MANAGER = "window_manager"
        private const val USE_LIB_TERMUX = "use_lib_termux"
        private const val SHOW_TUI = "show_tui"
        private const val HAS_SESSION = "has_session"
        private const val SESSION_TIME = "session_time"
        private const val USERNAME = "username"
        private const val PASSWORD = "password"
        private const val LAST_DISTRO = "last_distro"
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun initialize(
        distroName: String,
        architecture: String,
        desktopEnv: String,
        browser: String,
        windowManager: String,
        useLibTermux: Boolean,
        showTUI: Boolean
    ) {
        prefs.edit().apply {
            putString(DISTRO_NAME, distroName)
            putString(ARCHITECTURE, architecture)
            putString(DESKTOP_ENV, desktopEnv)
            putString(BROWSER, browser)
            putString(WINDOW_MANAGER, windowManager)
            putBoolean(USE_LIB_TERMUX, useLibTermux)
            putBoolean(SHOW_TUI, showTUI)
            putBoolean(HAS_SESSION, true)
            putLong(SESSION_TIME, System.currentTimeMillis())
            putString(USERNAME, "root")
            putString(PASSWORD, "root")
            if (distroName.isNotEmpty()) {
                putString(LAST_DISTRO, distroName)
            }
            apply()
        }
    }

    fun getSetupData(): SetupData = SetupData(
        distroName = distroName,
        architecture = architecture,
        desktopEnv = desktopEnv,
        browser = browser,
        windowManager = windowManager,
        useLibTermux = useLibTermux,
        showTUI = showTUI
    )

    fun hasSession(): Boolean = prefs.getBoolean(HAS_SESSION, false)

    fun getUsername(): String? = prefs.getString(USERNAME, "root")
    fun getPassword(): String? = prefs.getString(PASSWORD, "root")

    fun setUsername(username: String) {
        prefs.edit().putString(USERNAME, username).apply()
    }

    fun setPassword(password: String) {
        prefs.edit().putString(PASSWORD, password).apply()
    }

    val distroName: String
        get() = prefs.getString(DISTRO_NAME, "Linux") ?: "Linux"

    val architecture: String
        get() = prefs.getString(ARCHITECTURE, "x86_64") ?: "x86_64"

    val desktopEnv: String
        get() = prefs.getString(DESKTOP_ENV, "Gnome") ?: "Gnome"

    val browser: String
        get() = prefs.getString(BROWSER, "Firefox") ?: "Firefox"

    val windowManager: String
        get() = prefs.getString(WINDOW_MANAGER, "") ?: ""

    val useLibTermux: Boolean
        get() = prefs.getBoolean(USE_LIB_TERMUX, false)

    val showTUI: Boolean
        get() = prefs.getBoolean(SHOW_TUI, false)

    val lastDistro: String
        get() = prefs.getString(LAST_DISTRO, "session") ?: "session"

    fun clearSession() {
        prefs.edit().clear().apply()
    }

    fun saveSession() {
        prefs.edit().putLong(SESSION_TIME, System.currentTimeMillis()).apply()
    }

    fun delete() {
        clearSession()
        // Also delete the actual Linux environment directory
        val envDir = File("/storage/emulated/0/LinuxAndroid")
        if (envDir.exists()) {
            envDir.deleteRecursively()
        }
    }
}

data class SetupData(
    val distroName: String,
    val architecture: String,
    val desktopEnv: String,
    val browser: String,
    val windowManager: String,
    val useLibTermux: Boolean,
    val showTUI: Boolean
)

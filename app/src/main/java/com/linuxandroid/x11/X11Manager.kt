package com.linuxandroid.x11

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log

/**
 * X11Manager manages the lifecycle and connection to the Termux:X11 display server.
 * Termux:X11 (package: com.termux.x11) provides the high-performance X11 display server
 * on Android that renders the Linux desktop environment (XFCE, GNOME, etc.).
 */
class X11Manager(private val context: Context) {

    companion object {
        const val TAG = "X11Manager"
        const val TERMUX_X11_PACKAGE = "com.termux.x11"
        const val TERMUX_X11_ACTIVITY = "com.termux.x11.MainActivity"
        const val TERMUX_X11_DOWNLOAD_URL = "https://github.com/termux/termux-x11/releases/latest"
        const val DEFAULT_DISPLAY = ":0"
        const val DEFAULT_PULSE_SERVER = "127.0.0.1:4713"
    }

    /**
     * Checks whether the Termux:X11 application is installed on the device.
     */
    fun isTermuxX11Installed(): Boolean {
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(
                    TERMUX_X11_PACKAGE,
                    PackageManager.PackageInfoFlags.of(0)
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(TERMUX_X11_PACKAGE, 0)
            }
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    /**
     * Launches the Termux:X11 display server activity so the Linux GUI can be rendered.
     */
    fun launchTermuxX11(): Boolean {
        return try {
            val intent = Intent().apply {
                setClassName(TERMUX_X11_PACKAGE, TERMUX_X11_ACTIVITY)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch Termux:X11 activity: ${e.message}")
            // Fallback: try launching via launch intent
            try {
                val launchIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_X11_PACKAGE)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            } catch (fallbackError: Exception) {
                Log.e(TAG, "Fallback launch also failed: ${fallbackError.message}")
            }
            false
        }
    }

    /**
     * Opens the official GitHub release page to download Termux:X11 APK.
     */
    fun openTermuxX11DownloadPage() {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(TERMUX_X11_DOWNLOAD_URL)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open Termux:X11 download page: ${e.message}")
        }
    }
}

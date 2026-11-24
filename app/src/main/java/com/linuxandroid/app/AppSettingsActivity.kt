package com.linuxandroid.app

import android.content.Context
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.linuxandroid.R

/**
 * App Settings Activity for Linux Android
 * Allows users to configure their environment preferences
 */
class AppSettingsActivity : AppCompatActivity() {

    companion object {
        private const val PREFS_NAME = "LinuxAndroidPrefs"
    }

    private lateinit var sharedPreferences: android.content.SharedPreferences
    private lateinit var aboutTextView: TextView
    private lateinit var resetButton: Button
    private lateinit var settings: SettingsManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_settings)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        settings = SettingsManager(this)

        initViews()
    }

    private fun initViews() {
        aboutTextView = findViewById(R.id.aboutTextView)
        resetButton = findViewById(R.id.resetButton)

        // Generate distribution information
        aboutTextView.text = buildString {
            appendLine("Linux Android v1.0")
            appendLine()
            appendLine("Supported distributions:")
            appendLine("• Alpine Linux")
            appendLine("• Arch Linux")
            appendLine("• Ubuntu")
            appendLine("• Debian")
            appendLine("• Fedora")
            appendLine("• Kali Linux")
            appendLine("• Manjaro")
            appendLine("• EndeavourOS")
            appendLine("• Garuda Linux")
        }

        resetButton.setOnClickListener {
            resetAllSettings()
        }
    }

    private fun resetAllSettings() {
        AlertDialog.Builder(this)
            .setTitle("Reset All Settings")
            .setMessage("This will clear all your settings and distributions. Are you sure?")
            .setCancelable(false)
            .setPositiveButton("Reset") { dialog, _ ->
                sharedPreferences.edit().clear().apply()
                settings.clearAllSettings()
                Toast.makeText(this, "Settings reset complete", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

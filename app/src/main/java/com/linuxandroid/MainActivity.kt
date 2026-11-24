package com.linuxandroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.linuxandroid.setup.SetupActivity
import com.linuxandroid.ui.AboutActivity
import com.linuxandroid.ui.SettingsActivity

class MainActivity : AppCompatActivity() {

    private lateinit var startSetupButton: Button
    private lateinit var startSessionButton: Button
    private lateinit var settingsButton: Button
    private lateinit var aboutButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startSetupButton = findViewById(R.id.startSetupButton)
        startSessionButton = findViewById(R.id.startSessionButton)
        settingsButton = findViewById(R.id.settingsButton)
        aboutButton = findViewById(R.id.aboutButton)

        startSetupButton.setOnClickListener {
            launchSetup()
        }

        startSessionButton.setOnClickListener {
            launchSession()
        }

        settingsButton.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        aboutButton.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }

        // Check if there's an existing session
        checkExistingSession()
    }

    private fun launchSetup() {
        val intent = Intent(this, SetupActivity::class.java)
        startActivity(intent)
    }

    private fun launchSession() {
        val intent = Intent(this, SessionActivity::class.java)
        startActivity(intent)
    }

    private fun checkExistingSession() {
        val prefs = getSharedPreferences("LinuxAndroid", MODE_PRIVATE)
        val hasSession = prefs.getBoolean("has_session", false) &&
                        (System.currentTimeMillis() - prefs.getLong("session_time", 0) < 24 * 60 * 60 * 1000)

        startSessionButton.visibility = if (hasSession) View.VISIBLE else View.GONE
        startSessionButton.isEnabled = hasSession
        if (hasSession) {
            startSessionButton.text = "Continue ${prefs.getString("last_distro", "session")} Session"
        }
    }
}

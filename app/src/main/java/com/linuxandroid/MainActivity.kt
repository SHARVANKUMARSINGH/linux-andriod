package com.linuxandroid

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.linuxandroid.setup.SetupActivity
import com.linuxandroid.ui.AboutActivity
import com.linuxandroid.ui.SettingsActivity
import com.linuxandroid.x11.DesktopActivity

class MainActivity : AppCompatActivity() {

    private lateinit var startSetupButton: Button
    private lateinit var startSessionButton: Button
    private lateinit var settingsButton: Button
    private lateinit var aboutButton: Button
    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sessionManager = SessionManager(this)

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

        checkExistingSession()
    }

    override fun onResume() {
        super.onResume()
        checkExistingSession()
    }

    private fun launchSetup() {
        val intent = Intent(this, SetupActivity::class.java)
        startActivity(intent)
    }

    private fun launchSession() {
        if (sessionManager.isEmbeddedDisplay) {
            val intent = Intent(this, DesktopActivity::class.java).apply {
                putExtra("EXTRA_DISTRO", sessionManager.distroName)
                putExtra("EXTRA_DESKTOP", sessionManager.desktopEnv)
            }
            startActivity(intent)
        } else {
            val intent = Intent(this, SessionActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkExistingSession() {
        val hasSession = sessionManager.hasSession()
        startSessionButton.visibility = if (hasSession) View.VISIBLE else View.GONE
        startSessionButton.isEnabled = hasSession
        if (hasSession) {
            startSessionButton.text = "✨ Resume ${sessionManager.distroName} (${sessionManager.desktopEnv})"
        }
    }
}

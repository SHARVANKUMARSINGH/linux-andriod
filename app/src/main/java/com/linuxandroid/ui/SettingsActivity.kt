package com.linuxandroid.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.linuxandroid.MainActivity
import com.linuxandroid.R
import com.linuxandroid.SessionManager

class SettingsActivity : AppCompatActivity() {

    private lateinit var usernameRadioGroup: RadioGroup
    private lateinit var rootUserRadio: RadioButton
    private lateinit var standardUserRadio: RadioButton
    private lateinit var usernameEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmEditText: EditText
    private lateinit var saveButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        usernameRadioGroup = findViewById(R.id.usernameRadioGroup)
        rootUserRadio = findViewById(R.id.rootUserRadio)
        standardUserRadio = findViewById(R.id.standardUserRadio)
        usernameEditText = findViewById(R.id.usernameEditText)
        passwordEditText = findViewById(R.id.passwordEditText)
        confirmEditText = findViewById(R.id.confirmEditText)
        saveButton = findViewById(R.id.saveButton)

        // Load current settings
        loadSettings()

        usernameRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            usernameEditText.isEnabled = checkedId == R.id.standardUserRadio
        }

        saveButton.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        val sessionManager = SessionManager(this)
        val currentUsername = sessionManager.getUsername() ?: "root"

        if (currentUsername == "root") {
            rootUserRadio.isChecked = true
            usernameEditText.isEnabled = false
        } else {
            standardUserRadio.isChecked = true
            usernameEditText.isEnabled = true
            usernameEditText.setText(currentUsername)
        }
    }

    private fun saveSettings() {
        val username = if (rootUserRadio.isChecked) {
            "root"
        } else {
            usernameEditText.text.toString().takeIf { it.isNotBlank() } ?: run {
                Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show()
                return
            }
        }

        val password = passwordEditText.text.toString()
        val confirm = confirmEditText.text.toString()

        if (password.isEmpty()) {
            Toast.makeText(this, "Please enter a password", Toast.LENGTH_SHORT).show()
            return
        }

        if (password != confirm) {
            Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
            return
        }

        if (password.length < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
            return
        }

        val sessionManager = SessionManager(this)
        sessionManager.setUsername(username)
        sessionManager.setPassword(password)

        Toast.makeText(this, "Settings saved successfully", Toast.LENGTH_SHORT).show()

        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

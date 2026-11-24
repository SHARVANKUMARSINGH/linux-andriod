package com.linuxandroid.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.linuxandroid.R

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)

        findViewById<TextView>(R.id.aboutText).text = buildString {
            appendLine("Linux on Android v1.0")
            appendLine()
            appendLine("Run Linux distributions directly on your Android device.")
            appendLine()
            appendLine("Features:")
            appendLine("• Multiple Linux distributions")
            appendLine("• Desktop environment support")
            appendLine("• Terminal session management")
            appendLine("• PDF document viewer")
            appendLine()
            appendLine("Supported Distributions:")
            appendLine("• Alpine Linux, Arch Linux, Ubuntu")
            appendLine("• Debian, Fedora, Kali Linux")
            appendLine("• Manjaro, EndeavourOS, Garuda Linux")
            appendLine()
            appendLine("License: MIT")
            appendLine()
            appendLine("Made with ❤️ for the open-source community")
        }

        findViewById<Button>(R.id.closeButton).setOnClickListener {
            finish()
        }
    }
}

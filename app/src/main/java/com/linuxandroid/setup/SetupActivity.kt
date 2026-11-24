package com.linuxandroid.setup

import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.linuxandroid.R
import com.linuxandroid.SessionManager

class SetupActivity : AppCompatActivity() {

    private lateinit var distroSpinner: Spinner
    private lateinit var desktopSpinner: Spinner
    private lateinit var browserSpinner: Spinner
    private lateinit var archRadioGroup: RadioGroup
    private lateinit var windowManagerRadioGroup: RadioGroup
    private lateinit var installButton: Button
    private lateinit var statusText: android.widget.TextView

    private val distros = arrayOf(
        "Alpine Linux",
        "Arch Linux",
        "Ubuntu",
        "Debian",
        "Fedora",
        "Kali Linux",
        "Manjaro",
        "EndeavourOS",
        "Garuda Linux"
    )

    private val desktopEnvs = arrayOf(
        "Gnome",
        "KDE Plasma",
        "XFCE",
        "LXQt",
        "MATE",
        "Cinnamon",
        "i3WM",
        "Sway",
        "No Desktop"
    )

    private val browsers = arrayOf(
        "Firefox",
        "Chromium",
        "None"
    )

    private val distroDefaults = mapOf(
        "Alpine Linux" to "xfce4",
        "Arch Linux" to "gnome",
        "Ubuntu" to "gnome",
        "Debian" to "gnome",
        "Fedora" to "kde",
        "Kali Linux" to "gnome",
        "Manjaro" to "plasma",
        "EndeavourOS" to "gnome",
        "Garuda Linux" to "plasma"
    )

    private val browserDefaults = mapOf(
        "Firefox" to "firefox-esr",
        "Chromium" to "chromium",
        "None" to ""
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        statusText = findViewById(R.id.statusText)
        distroSpinner = findViewById(R.id.distroSpinner)
        desktopSpinner = findViewById(R.id.desktopSpinner)
        browserSpinner = findViewById(R.id.browserSpinner)
        archRadioGroup = findViewById(R.id.archRadioGroup)
        windowManagerRadioGroup = findViewById(R.id.windowManagerRadioGroup)
        installButton = findViewById(R.id.installButton)

        // Set up distro spinner
        val distroAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, distros)
        distroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        distroSpinner.adapter = distroAdapter

        distroSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                updateDesktopSpinner(distroDefaults[distros[position]] ?: "gnome")
                checkARM64Needed()
            }
            override fun onNothingSelected(parent: AdapterView<*>) {}
        }

        // Set up desktop environment spinner
        val desktopAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, desktopEnvs)
        desktopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        desktopSpinner.adapter = desktopAdapter

        // Set up browser spinner
        val browserAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, browsers)
        browserAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        browserSpinner.adapter = browserAdapter

        // Set arm64 as default
        findViewById<RadioButton>(R.id.arm64Radio).isChecked = true

        installButton.setOnClickListener {
            startInstallation()
        }
    }

    private fun updateDesktopSpinner(defaultStr: String) {
        val sortedEnvs = desktopEnvs.sortedBy { it.contains(defaultStr, ignoreCase = true) }
        val desktopAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortedEnvs)
        desktopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        desktopSpinner.adapter = desktopAdapter
    }

    private fun checkARM64Needed() {
        val selectedDistro = distroSpinner.selectedItem.toString()
        val isARM64Needed = selectedDistro == "Alpine Linux" ||
            selectedDistro == "Arch Linux" ||
            selectedDistro == "Kali Linux" ||
            selectedDistro == "Ubuntu" ||
            selectedDistro == "Debian"

        findViewById<RadioButton>(R.id.arm64Radio).isEnabled = isARM64Needed
        if (!isARM64Needed) {
            findViewById<RadioButton>(R.id.x86_64Radio).isChecked = true
        }
    }

    private fun startInstallation() {
        val distro = distroSpinner.selectedItem.toString()
        val arch = when (archRadioGroup.checkedRadioButtonId) {
            R.id.arm64Radio -> "arm64"
            else -> "x86_64"
        }
        val desktop = desktopSpinner.selectedItem.toString()
        val browser = browserSpinner.selectedItem.toString()
        val windowManager = when (windowManagerRadioGroup.checkedRadioButtonId) {
            R.id.turmuxRadio -> "turmux"
            R.id.turmuxX11Radio -> "turmux-x11"
            else -> ""
        }

        // Show installation started
        installButton.isEnabled = false
        statusText.text = "Preparing ${distro} installation..."

        // Start the session activity
        val sessionManager = SessionManager(this)
        sessionManager.initialize(
            distroName = distro,
            architecture = arch,
            desktopEnv = desktop.split(" ").first(),
            browser = browser,
            windowManager = windowManager,
            useLibTermux = arch == "arm64",
            showTUI = windowManager.isEmpty()
        )

        android.util.Log.d("SetupActivity", "Setup complete: distro=$distro, arch=$arch, desktop=$desktop, windowManager=$windowManager")

        Toast.makeText(this, "Installation prepared! Tap the app icon to start Linux.", Toast.LENGTH_LONG).show()

        // Open session activity after a short delay
        handler.postDelayed({
            try {
                val intent = android.content.Intent(this, com.linuxandroid.SessionActivity::class.java)
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("SetupActivity", "Failed to open session activity", e)
                statusText.text = "Error launching session. Please try again."
                installButton.isEnabled = true
            }
        }, 1500)
    }

    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
}

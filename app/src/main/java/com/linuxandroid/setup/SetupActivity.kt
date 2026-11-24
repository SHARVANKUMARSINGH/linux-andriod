package com.linuxandroid.setup

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ProgressBar
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.linuxandroid.R
import com.linuxandroid.SessionActivity
import com.linuxandroid.SessionManager
import com.linuxandroid.proot.LinuxBootloader
import com.linuxandroid.x11.DesktopActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SetupActivity : AppCompatActivity() {

    private lateinit var distroSpinner: Spinner
    private lateinit var desktopSpinner: Spinner
    private lateinit var browserSpinner: Spinner
    private lateinit var archRadioGroup: RadioGroup
    private lateinit var windowManagerRadioGroup: RadioGroup
    private lateinit var installButton: Button
    private lateinit var statusText: TextView
    private var setupProgressBar: ProgressBar? = null

    private val distros = arrayOf(
        "Ubuntu 24.04 LTS",
        "Debian 12 Bookworm",
        "Kali Linux",
        "Arch Linux",
        "Alpine Linux",
        "Fedora 41",
        "Manjaro",
        "Pop!_OS",
        "EndeavourOS",
        "Garuda Linux"
    )

    private val desktopEnvs = arrayOf(
        "XFCE (Recommended, Lightweight)",
        "GNOME (Modern Desktop)",
        "KDE Plasma (Feature Rich)",
        "LXQt (Ultra Lightweight)",
        "MATE (Classic Desktop)",
        "Cinnamon (Sleek)",
        "i3WM (Tiling Window Manager)",
        "Terminal Only (CLI)"
    )

    private val browsers = arrayOf(
        "Firefox (Optimized)",
        "Chromium",
        "None"
    )

    private val distroDefaults = mapOf(
        "Ubuntu 24.04 LTS" to "XFCE",
        "Debian 12 Bookworm" to "XFCE",
        "Kali Linux" to "XFCE",
        "Arch Linux" to "XFCE",
        "Alpine Linux" to "XFCE",
        "Fedora 41" to "GNOME",
        "Manjaro" to "KDE Plasma",
        "Pop!_OS" to "GNOME",
        "EndeavourOS" to "XFCE",
        "Garuda Linux" to "KDE Plasma"
    )

    private val handler = Handler(Looper.getMainLooper())

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
        setupProgressBar = findViewById(R.id.setupProgressBar)

        // Set up distro spinner
        val distroAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, distros)
        distroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        distroSpinner.adapter = distroAdapter

        distroSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                updateDesktopSpinner(distroDefaults[distros[position]] ?: "XFCE")
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
        val sortedEnvs = desktopEnvs.sortedByDescending { it.contains(defaultStr, ignoreCase = true) }
        val desktopAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, sortedEnvs)
        desktopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        desktopSpinner.adapter = desktopAdapter
    }

    private fun checkARM64Needed() {
        val isARM64Supported = true
        findViewById<RadioButton>(R.id.arm64Radio).isEnabled = isARM64Supported
    }

    private fun startInstallation() {
        val distro = distroSpinner.selectedItem.toString().split(" ").first()
        val arch = when (archRadioGroup.checkedRadioButtonId) {
            R.id.arm64Radio -> "arm64"
            else -> "x86_64"
        }
        val desktopFull = desktopSpinner.selectedItem.toString()
        val desktop = desktopFull.split(" ").first()
        val browser = browserSpinner.selectedItem.toString().split(" ").first()

        val isEmbedded = findViewById<RadioButton>(R.id.embeddedDisplayRadio)?.isChecked == true
        val isX11 = findViewById<RadioButton>(R.id.turmuxX11Radio)?.isChecked == true

        val displayMode = when {
            isEmbedded -> "embedded"
            isX11 -> "termux_x11"
            else -> "cli"
        }

        val windowManager = when {
            isEmbedded -> "embedded-x11"
            isX11 -> "turmux-x11"
            else -> "turmux"
        }

        // Disable button & show progress
        installButton.isEnabled = false
        setupProgressBar?.visibility = View.VISIBLE
        statusText.text = "Preparing $distro ($desktop) environment..."

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Initialize rootfs directory and boot script
                val rootfsDir = File(filesDir, "rootfs")
                if (!rootfsDir.exists()) {
                    rootfsDir.mkdirs()
                }

                val dirs = listOf("etc", "tmp", "root", "dev", "proc", "sys", "usr/bin")
                for (d in dirs) {
                    File(rootfsDir, d).mkdirs()
                }

                // Setup DNS
                File(rootfsDir, "etc/resolv.conf").writeText("nameserver 1.1.1.1\nnameserver 8.8.8.8\n")

                // Write start-linux.sh
                val scriptContent = LinuxBootloader.generateStartScript(
                    targetDir = rootfsDir.absolutePath,
                    distroName = distro,
                    desktopEnv = desktop,
                    isX11 = isEmbedded || isX11,
                    username = "root"
                )
                val startScript = File(filesDir, "start-linux.sh")
                startScript.writeText(scriptContent)
                startScript.setExecutable(true, false)

                // Save session parameters
                val sessionManager = SessionManager(this@SetupActivity)
                sessionManager.initialize(
                    distroName = distro,
                    architecture = arch,
                    desktopEnv = desktop,
                    browser = browser,
                    windowManager = windowManager,
                    useLibTermux = arch == "arm64",
                    showTUI = !isEmbedded && !isX11,
                    displayMode = displayMode
                )

                withContext(Dispatchers.Main) {
                    setupProgressBar?.visibility = View.GONE
                    statusText.text = "Configuration complete! Launching Linux..."
                    Toast.makeText(
                        this@SetupActivity,
                        "Starting $distro ($desktop)...",
                        Toast.LENGTH_SHORT
                    ).show()

                    handler.postDelayed({
                        if (isEmbedded) {
                            val intent = Intent(this@SetupActivity, DesktopActivity::class.java).apply {
                                putExtra("EXTRA_DISTRO", distro)
                                putExtra("EXTRA_DESKTOP", desktop)
                            }
                            startActivity(intent)
                        } else {
                            val intent = Intent(this@SetupActivity, SessionActivity::class.java)
                            startActivity(intent)
                        }
                        finish()
                    }, 500)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setupProgressBar?.visibility = View.GONE
                    installButton.isEnabled = true
                    statusText.text = "Setup error: ${e.message}"
                }
            }
        }
    }
}
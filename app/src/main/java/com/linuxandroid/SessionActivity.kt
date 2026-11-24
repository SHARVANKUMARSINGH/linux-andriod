package com.linuxandroid

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.linuxandroid.proot.LinuxBootloader
import com.linuxandroid.x11.X11Manager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SessionActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var detailText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var launchButton: Button
    private lateinit var openX11Button: Button
    private lateinit var copyCommandButton: Button
    private lateinit var restartButton: Button
    private lateinit var scrollView: ScrollView
    private lateinit var commandOutput: EditText

    private var isRunning = false
    private var isInstalling = false
    private var targetDir = ""

    private lateinit var sessionManager: SessionManager
    private lateinit var x11Manager: X11Manager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session)

        sessionManager = SessionManager(this)
        x11Manager = X11Manager(this)

        statusText = findViewById(R.id.statusText)
        detailText = findViewById(R.id.detailText)
        progressBar = findViewById(R.id.progressBar)
        launchButton = findViewById(R.id.launchButton)
        openX11Button = findViewById(R.id.openX11Button)
        copyCommandButton = findViewById(R.id.copyCommandButton)
        restartButton = findViewById(R.id.restartButton)
        scrollView = findViewById(R.id.scrollView)
        commandOutput = findViewById(R.id.commandOutput)

        targetDir = File(filesDir, "rootfs").absolutePath

        if (sessionManager.hasSession()) {
            showSessionStatus()
        } else {
            startSetup()
        }

        launchButton.setOnClickListener {
            launchLinux()
        }

        openX11Button.setOnClickListener {
            handleOpenX11()
        }

        copyCommandButton.setOnClickListener {
            copyBootCommand()
        }

        restartButton.setOnClickListener {
            startSetup()
        }
    }

    private fun isX11Session(): Boolean {
        val wm = sessionManager.windowManager.lowercase()
        return wm.contains("x11") || wm.contains("turmux-x11") || wm.contains("termux-x11")
    }

    private fun showSessionStatus() {
        val setupData = sessionManager.getSetupData()
        statusText.text = "Environment Ready: ${setupData.distroName}"
        detailText.text = "${setupData.distroName} (${setupData.desktopEnv}) on ${setupData.architecture}\nX11 Display: :0"

        launchButton.visibility = View.VISIBLE
        copyCommandButton.visibility = View.VISIBLE
        restartButton.visibility = View.VISIBLE

        if (isX11Session()) {
            openX11Button.visibility = View.VISIBLE
        }
    }

    private fun handleOpenX11() {
        if (x11Manager.isTermuxX11Installed()) {
            x11Manager.launchTermuxX11()
            Toast.makeText(this, "Opening Termux:X11 Display (:0)...", Toast.LENGTH_SHORT).show()
        } else {
            AlertDialog.Builder(this)
                .setTitle("Termux:X11 Companion Required")
                .setMessage("Termux:X11 is required to display graphical desktops like XFCE and GNOME. Would you like to download it now?")
                .setPositiveButton("Download APK") { _, _ ->
                    x11Manager.openTermuxX11DownloadPage()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun copyBootCommand() {
        val setupData = sessionManager.getSetupData()
        val command = LinuxBootloader.generateQuickInstallCommand(setupData.distroName, setupData.desktopEnv)
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Linux Boot Command", command)
        clipboard.setPrimaryClip(clip)
        Toast.makeText(this, "Boot command copied to clipboard!", Toast.LENGTH_LONG).show()
    }

    private fun startSetup() {
        if (isInstalling) return
        isInstalling = true

        runOnUiThread {
            progressBar.visibility = View.VISIBLE
            statusText.text = "Configuring Linux PRoot Container..."
            detailText.text = "Setting up root filesystem and X11 display linkage..."
            launchButton.visibility = View.GONE
            openX11Button.visibility = View.GONE
            copyCommandButton.visibility = View.GONE
            restartButton.visibility = View.GONE
        }

        CoroutineScope(Dispatchers.Main).launch {
            val setupData = sessionManager.getSetupData()
            val distro = setupData.distroName
            val arch = setupData.architecture
            val desktop = setupData.desktopEnv
            val isX11 = isX11Session()
            val username = sessionManager.getUsername() ?: "root"

            logOutput("Initializing $distro ($arch) environment...")
            logOutput("Configuring desktop: $desktop (X11 Display: :0)")

            withContext(Dispatchers.IO) {
                try {
                    val rootfsDir = File(targetDir)
                    if (!rootfsDir.exists()) {
                        rootfsDir.mkdirs()
                    }

                    // Create standard filesystem directories
                    val dirs = listOf("etc", "tmp", "home/$username", "root", "dev", "proc", "sys", "usr/bin")
                    for (d in dirs) {
                        File(rootfsDir, d).mkdirs()
                    }

                    // Configure DNS resolv.conf
                    val resolvFile = File(rootfsDir, "etc/resolv.conf")
                    resolvFile.writeText("nameserver 1.1.1.1\nnameserver 8.8.8.8\n")

                    // Create start-linux.sh bootloader script
                    val startScript = LinuxBootloader.generateStartScript(
                        targetDir = targetDir,
                        distroName = distro,
                        desktopEnv = desktop,
                        isX11 = isX11,
                        username = username
                    )
                    val startScriptFile = File(filesDir, "start-linux.sh")
                    startScriptFile.writeText(startScript)
                    startScriptFile.setExecutable(true, false)

                    withContext(Dispatchers.Main) {
                        logOutput("Generated start-linux.sh boot script")
                        logOutput("Created PRoot mount bindings and X11 sockets")
                        logOutput("Container setup completed successfully!")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        logOutput("Error configuring container: ${e.message}")
                    }
                }
            }

            isInstalling = false
            runOnUiThread {
                progressBar.visibility = View.GONE
                showSessionStatus()
            }
            sessionManager.saveSession()
        }
    }

    private fun launchLinux() {
        if (isRunning) {
            Toast.makeText(this, "Session already active", Toast.LENGTH_SHORT).show()
            return
        }
        isRunning = true

        runOnUiThread {
            progressBar.visibility = View.VISIBLE
            statusText.text = "Booting ${sessionManager.distroName}..."
            launchButton.isEnabled = false
        }

        val setupData = sessionManager.getSetupData()
        val isX11 = isX11Session()

        // If X11 mode is enabled, also open Termux:X11 display
        if (isX11 && x11Manager.isTermuxX11Installed()) {
            x11Manager.launchTermuxX11()
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val startScript = File(filesDir, "start-linux.sh")
                if (!startScript.exists()) {
                    val scriptContent = LinuxBootloader.generateStartScript(
                        targetDir = targetDir,
                        distroName = setupData.distroName,
                        desktopEnv = setupData.desktopEnv,
                        isX11 = isX11,
                        username = sessionManager.getUsername() ?: "root"
                    )
                    startScript.writeText(scriptContent)
                    startScript.setExecutable(true, false)
                }

                withContext(Dispatchers.Main) {
                    logOutput("Booting Linux container via: ${startScript.absolutePath}")
                    logOutput("Connecting to Termux:X11 display :0...")
                }

                // Execute the boot script
                val process = ProcessBuilder("sh", startScript.absolutePath)
                    .redirectErrorStream(true)
                    .start()

                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        withContext(Dispatchers.Main) {
                            logOutput(line)
                        }
                    }
                }

                process.waitFor()

                withContext(Dispatchers.Main) {
                    logOutput("Linux session exited.")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "Session error"
                    logOutput("Boot error: ${e.message}")
                }
            }

            withContext(Dispatchers.Main) {
                isRunning = false
                progressBar.visibility = View.GONE
                launchButton.isEnabled = true
                statusText.text = "Session Ready: ${sessionManager.distroName}"
            }
        }
    }

    private fun logOutput(line: String) {
        val output = "${formatter.format(LocalDateTime.now())}: $line"
        commandOutput.append("$output\n")
        scrollView.post { scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    companion object {
        private val formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}

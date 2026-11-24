package com.linuxandroid

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class SessionActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var detailText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var launchButton: Button
    private lateinit var restartButton: Button
    private lateinit var scrollView: ScrollView
    private lateinit var commandOutput: EditText

    private var isInstalling = false
    private var installStep = 0
    private var bootstrapUrl = ""
    private var targetDir = ""

    private lateinit var sessionManager: SessionManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session)

        sessionManager = SessionManager(this)

        statusText = findViewById(R.id.statusText)
        detailText = findViewById(R.id.detailText)
        progressBar = findViewById(R.id.progressBar)
        launchButton = findViewById(R.id.launchButton)
        restartButton = findViewById(R.id.restartButton)
        scrollView = findViewById(R.id.scrollView)
        commandOutput = findViewById(R.id.commandOutput)

        // Get credentials from settings
        val username = sessionManager.getUsername() ?: "root"
        val password = sessionManager.getPassword() ?: "root"

        if (sessionManager.hasSession()) {
            showSessionStatus()
        } else {
            startSetup()
        }

        launchButton.setOnClickListener {
            launchLinux()
        }

        restartButton.setOnClickListener {
            checkHomeExistsAndRestart()
        }
    }

    private fun showSessionStatus() {
        statusText.text = "Session ready"
        detailText.text = "${sessionManager.distroName} (${sessionManager.desktopEnv}) for ${sessionManager.architecture}"
        launchButton.visibility = View.VISIBLE
    }

    private fun startSetup() {
        runOnUiThread {
            progressBar.visibility = View.VISIBLE
            statusText.text = "Initializing Linux environment..."
        }

        CoroutineScope(Dispatchers.Main).launch {
            val setupData = sessionManager.getSetupData()

            // Set bootstrap URL based on architecture
            bootstrapUrl = "https://raw.githubusercontent.com/termux/termux-packages/master/bootstrap/${setupData.architecture}.tar.xz"

            targetDir = "/storage/emulated/0/LinuxAndroid"

            // Create target directory
            installStep = 1
            updateStatus("Creating filesystem structure...", "")

            withContext(Dispatchers.IO) {
                try {
                    val process = ProcessBuilder(
                        "mkdir", "-p", targetDir,
                        "$targetDir/tmp", "$targetDir/home",
                        "$targetDir/opt", "$targetDir/var/cache/apt/archives"
                    ).start()
                    process.waitFor()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        logOutput("Error creating directories: ${e.message}")
                    }
                }
            }
            logOutput("Created directory structure")

            // Create base filesystem
            installStep = 2
            updateStatus("Creating base filesystem...", "This may take a minute")

            withContext(Dispatchers.IO) {
                try {
                    val tarProcess = ProcessBuilder(
                        "tar", "xf", bootstrapUrl,
                        "-C", targetDir
                    ).start()
                    tarProcess.waitFor()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        logOutput("Error extracting bootstrap: ${e.message}")
                    }
                }
            }
            logOutput("Extracted bootstrap packages")

            // Set up chroot environment
            installStep = 3
            updateStatus("Configuring environment...", "Setting up hostname and environment")

            withContext(Dispatchers.IO) {
                try {
                    val envProcess = ProcessBuilder(
                        "cp", "/etc/passwd", "$targetDir/etc/passwd"
                    ).start()
                    envProcess.waitFor()
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        logOutput("Error configuring environment: ${e.message}")
                    }
                }
            }
            logOutput("Initialized basic environment")

            // Finalize setup
            installStep = 4
            updateStatus("Setup complete!", "Press the button to launch Linux")

            runOnUiThread {
                progressBar.visibility = View.GONE
                launchButton.visibility = View.VISIBLE
                detailText.text = "${sessionManager.distroName} (${sessionManager.desktopEnv}) for ${sessionManager.architecture}"
            }

            sessionManager.saveSession()
        }
    }

    private fun checkHomeExistsAndRestart() {
        if (!File(targetDir + "/home/root").exists()) {
            Toast.makeText(this, "Linux session not found. Please run setup again.", Toast.LENGTH_SHORT).show()
            return
        }

        updateStatus("Checking and repairing environment...", "")
        launchLinux()
    }

    private fun launchLinux() {
        if (isInstalling) return
        isInstalling = true

        runOnUiThread {
            launchButton.visibility = View.GONE
            progressBar.visibility = View.VISIBLE
            statusText.text = "Starting Linux session..."
            restartButton.visibility = View.VISIBLE
            restartButton.isEnabled = false
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Start bash shell in chroot
                val process = ProcessBuilder(
                    "bash",
                    "-c",
                    "cd $targetDir && mount -t proc proc /proc 2>/dev/null; " +
                            "mount -t sysfs sysfs /sys 2>/dev/null; " +
                            "chroot $targetDir /bin/bash"
                ).apply {
                    directory(File(targetDir))
                }.start()

                // Redirect output
                process.inputStream.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        withContext(Dispatchers.Main) {
                            logOutput(line)
                        }
                    }
                }

                process.waitFor()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    statusText.text = "Error starting session"
                    detailText.text = e.message ?: "Unknown error"
                }
            }

            withContext(Dispatchers.Main) {
                isInstalling = false
                restartButton.isEnabled = true
            }
        }
    }

    private fun updateStatus(status: String, detail: String) {
        runOnUiThread {
            statusText.text = status
            detailText.text = detail
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

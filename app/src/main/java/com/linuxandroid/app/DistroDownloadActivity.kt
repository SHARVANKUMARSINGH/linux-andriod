package com.linuxandroid.app

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.linuxandroid.R
import com.linuxandroid.proot.LinuxBootloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Alternative main activity with distribution download functionality.
 * Note: The primary launcher is com.linuxandroid.MainActivity.
 */
class DistroDownloadActivity : AppCompatActivity() {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var progressBar: ProgressBar
    private lateinit var statusText: TextView
    private lateinit var distroSpinner: Spinner
    private lateinit var desktopSpinner: Spinner
    private lateinit var archCheck: CheckBox
    private lateinit var archLabel: TextView
    private lateinit var setupButton: Button
    private lateinit var statusContainer: LinearLayout

    companion object {
        private const val PREFS_NAME = "LinuxAndroidPrefs"
        private const val KEY_CURRENT_DISTRIBUTION = "current_distribution"
        private const val KEY_CURRENT_DESKTOP = "current_desktop"
        private const val KEY_CURRENT_ARCH = "current_arch"
        private const val KEY_IS_INSTALLED = "is_installed"
        const val TAG = "LinuxAndroid"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val distributions = listOf(
        "Ubuntu ARM64 (recommended)",
        "Debian ARM64",
        "Arch Linux ARM",
        "Fedora ARM",
        "Alpine Linux ARM",
        "Manjaro ARM",
        "Kali ARM64",
        "Pop!_OS ARM64"
    )

    private val desktops = listOf(
        "GNOME",
        "KDE Plasma",
        "XFCE",
        "LXQt",
        "MATE",
        "Cinnamon",
        "i3WM",
        "Sway"
    )

    private val distributionUrls = mapOf(
        "Ubuntu ARM64 (recommended)" to LinuxBootloader.getRootfsUrl("ubuntu", "arm64"),
        "Debian ARM64" to LinuxBootloader.getRootfsUrl("debian", "arm64"),
        "Arch Linux ARM" to LinuxBootloader.getRootfsUrl("arch", "arm64"),
        "Fedora ARM" to LinuxBootloader.getRootfsUrl("fedora", "arm64"),
        "Alpine Linux ARM" to LinuxBootloader.getRootfsUrl("alpine", "arm64"),
        "Manjaro ARM" to LinuxBootloader.getRootfsUrl("arch", "arm64"),
        "Kali ARM64" to LinuxBootloader.getRootfsUrl("kali", "arm64"),
        "Pop!_OS ARM64" to LinuxBootloader.getRootfsUrl("pop", "arm64")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_distro_download)

        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        initViews()
        loadSavedPreferences()
        updateUIBasedOnInstallationState()

        distroSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {}
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val distro = distroSpinner.selectedItem.toString()
                if (distributionUrls.containsKey(distro)) {
                    updateDesktopSpinner(distro)
                }
            }
        }

        setupButton.setOnClickListener {
            performSetup()
        }
    }

    private fun initViews() {
        progressBar = findViewById(R.id.progressBar)
        statusText = findViewById(R.id.statusText)
        distroSpinner = findViewById(R.id.distroSpinner)
        desktopSpinner = findViewById(R.id.desktopSpinner)
        archCheck = findViewById(R.id.archCheck)
        archLabel = findViewById(R.id.archLabel)
        setupButton = findViewById(R.id.setupButton)
        statusContainer = findViewById(R.id.statusContainer)

        // Set up spinners
        val distroAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, distributions)
        distroAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        distroSpinner.adapter = distroAdapter

        val desktopAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, desktops)
        desktopAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        desktopSpinner.adapter = desktopAdapter

        archCheck.setOnCheckedChangeListener { _, isChecked ->
            updateArchLabel(isChecked)
        }
    }

    private fun loadSavedPreferences() {
        val currentDistro = sharedPreferences.getString(KEY_CURRENT_DISTRIBUTION, distributions[0])
        val isArch = sharedPreferences.getBoolean(KEY_CURRENT_ARCH, false)

        // Set distribution
        val distroPosition = distributions.indexOf(currentDistro)
        if (distroPosition >= 0) {
            distroSpinner.setSelection(distroPosition)
        }

        // Toggle architecture checkbox
        archCheck.isChecked = isArch
        updateArchLabel(isArch)
    }

    private fun updateDesktopSpinner(distro: String) {
        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            desktops
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        desktopSpinner.adapter = adapter
    }

    private fun updateArchLabel(isArch: Boolean) {
        archLabel.text = if (isArch) {
            "Native ARM64 mode selected"
        } else {
            "Using emulated x86_64 mode"
        }
    }

    private fun updateUIBasedOnInstallationState() {
        val isInstalled = sharedPreferences.getBoolean(KEY_IS_INSTALLED, false)

        if (isInstalled) {
            setupButton.text = "Reinstall"
            statusContainer.visibility = View.VISIBLE
            val savedDistro = sharedPreferences.getString(KEY_CURRENT_DISTRIBUTION, distributions[0]) ?: distributions[0]
            val savedDesktop = sharedPreferences.getString(KEY_CURRENT_DESKTOP, desktops[0]) ?: desktops[0]
            statusText.text = "$savedDistro installed successfully!\nDesktop: $savedDesktop"
        }
    }

    private fun performSetup() {
        val distro = distroSpinner.selectedItem.toString()
        val desktop = desktopSpinner.selectedItem.toString()

        // Save preferences
        sharedPreferences.edit().apply {
            putString(KEY_CURRENT_DISTRIBUTION, distro)
            putString(KEY_CURRENT_DESKTOP, desktop)
            putBoolean(KEY_CURRENT_ARCH, archCheck.isChecked)
            putBoolean(KEY_IS_INSTALLED, true)
            apply()
        }

        showLoading("Downloading ${distro}...")

        lifecycleScope.launch {
            try {
                val url = distributionUrls[distro] ?: ""

                if (url.isEmpty()) {
                    showError("Invalid distribution selected")
                    return@launch
                }

                val result = downloadBootstrap(url)
                withContext(Dispatchers.Main) {
                    hideLoading()

                    if (result.success) {
                        showToast("Installation successful! Desktop: $desktop")
                        updateUIBasedOnInstallationState()
                    } else {
                        showError("Failed to install: ${result.error}")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    hideLoading()
                    showError("Installation failed: ${e.message}")
                    Log.e(TAG, "Setup error", e)
                }
            }
        }
    }

    private suspend fun downloadBootstrap(url: String): DownloadResult {
        return withContext(Dispatchers.IO) {
            try {
                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    return@withContext DownloadResult(false, "HTTP ${response.code}")
                }

                val body = response.body
                if (body == null) {
                    return@withContext DownloadResult(false, "Empty response body")
                }

                val tempFile = File(cacheDir, "bootstrap_download.tar.xz")
                val inputStream = body.byteStream()

                // Download and save
                FileOutputStream(tempFile).use { output ->
                    inputStream.copyTo(output)
                }

                DownloadResult(true, data = tempFile)
            } catch (e: IOException) {
                DownloadResult(false, e.message ?: "Network error")
            }
        }
    }

    private fun showLoading(message: String) {
        progressBar.visibility = View.VISIBLE
        statusText.text = message
        statusContainer.visibility = View.VISIBLE
        setupButton.isEnabled = false
    }

    private fun hideLoading() {
        progressBar.visibility = View.GONE
        setupButton.isEnabled = true
    }

    private fun showToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e(TAG, "Error: $message")
    }

    data class DownloadResult(
        val success: Boolean,
        val error: String = "",
        val data: File? = null
    )
}

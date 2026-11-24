package com.linuxandroid.vm

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.linuxandroid.R
import com.linuxandroid.models.LinuxVm
import com.linuxandroid.x11.DesktopActivity
import kotlinx.coroutines.launch

class VmInstallActivity : AppCompatActivity() {

    private lateinit var tvDistroEmoji: TextView
    private lateinit var tvVmTitle: TextView
    private lateinit var tvVmSubtitle: TextView
    private lateinit var tvStatusStage: TextView
    private lateinit var tvPercent: TextView
    private lateinit var tvBytesInfo: TextView
    private lateinit var tvSpeedInfo: TextView
    private lateinit var tvLogConsole: TextView
    private lateinit var installProgressIndicator: LinearProgressIndicator
    private lateinit var btnLaunchVm: Button

    private lateinit var vmManager: VmManager
    private lateinit var vmInstaller: VmInstaller
    private var currentVm: LinuxVm? = null

    private val logLines = mutableListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vm_install)

        vmManager = VmManager(this)
        vmInstaller = VmInstaller(this, vmManager)

        initViews()

        val vmId = intent.getStringExtra("EXTRA_VM_ID") ?: vmManager.getActiveVmId()
        currentVm = if (vmId != null) vmManager.getVm(vmId) else vmManager.getActiveVm()

        val vm = currentVm
        if (vm == null) {
            Toast.makeText(this, "No VM selected for installation", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        populateVmInfo(vm)

        if (vm.isInstalled) {
            markComplete(vm)
        } else {
            startInstallProcess(vm)
        }
    }

    private fun initViews() {
        tvDistroEmoji = findViewById(R.id.tvDistroEmoji)
        tvVmTitle = findViewById(R.id.tvVmTitle)
        tvVmSubtitle = findViewById(R.id.tvVmSubtitle)
        tvStatusStage = findViewById(R.id.tvStatusStage)
        tvPercent = findViewById(R.id.tvPercent)
        tvBytesInfo = findViewById(R.id.tvBytesInfo)
        tvSpeedInfo = findViewById(R.id.tvSpeedInfo)
        tvLogConsole = findViewById(R.id.tvLogConsole)
        installProgressIndicator = findViewById(R.id.installProgressIndicator)
        btnLaunchVm = findViewById(R.id.btnLaunchVm)
    }

    private fun populateVmInfo(vm: LinuxVm) {
        tvVmTitle.text = vm.name
        tvVmSubtitle.text = "${vm.architecture.uppercase()} • ${vm.desktopEnv} • ${vm.mirrorUrl.take(40)}..."
        tvDistroEmoji.text = when {
            vm.distro.contains("Ubuntu", ignoreCase = true) -> "🟠"
            vm.distro.contains("Debian", ignoreCase = true) -> "🔴"
            vm.distro.contains("Alpine", ignoreCase = true) -> "🏔️"
            vm.distro.contains("Arch", ignoreCase = true) -> "🔵"
            vm.distro.contains("Kali", ignoreCase = true) -> "🐉"
            vm.distro.contains("Fedora", ignoreCase = true) -> "🎩"
            else -> "🐧"
        }
    }

    private fun addLog(entry: String) {
        logLines.add(entry)
        if (logLines.size > 8) {
            logLines.removeAt(0)
        }
        tvLogConsole.text = logLines.joinToString("\n") { "• $it" }
    }

    private fun startInstallProcess(vm: LinuxVm) {
        btnLaunchVm.isEnabled = false
        btnLaunchVm.text = "Downloading & Configuring VM..."
        addLog("Connecting to mirror: ${vm.mirrorUrl}")

        lifecycleScope.launch {
            vmInstaller.installVm(vm, object : VmInstaller.InstallListener {
                override fun onProgress(
                    stage: String,
                    bytesDownloaded: Long,
                    totalBytes: Long,
                    percentage: Int,
                    speedText: String
                ) {
                    runOnUiThread {
                        tvStatusStage.text = stage
                        tvPercent.text = "$percentage%"
                        installProgressIndicator.progress = percentage

                        if (totalBytes > 0) {
                            val dlMb = bytesDownloaded / (1024 * 1024)
                            val totMb = totalBytes / (1024 * 1024)
                            tvBytesInfo.text = "$dlMb MB / $totMb MB"
                        } else if (bytesDownloaded > 0) {
                            tvBytesInfo.text = "${bytesDownloaded / (1024 * 1024)} MB downloaded"
                        }

                        tvSpeedInfo.text = "Speed: $speedText"

                        if (logLines.lastOrNull() != stage) {
                            addLog(stage)
                        }
                    }
                }

                override fun onComplete(vm: LinuxVm) {
                    runOnUiThread {
                        markComplete(vm)
                    }
                }

                override fun onError(error: String) {
                    runOnUiThread {
                        tvStatusStage.text = "Error installing VM"
                        addLog("Error: $error")
                        btnLaunchVm.isEnabled = true
                        btnLaunchVm.text = "Retry Installation"
                        btnLaunchVm.setOnClickListener {
                            startInstallProcess(vm)
                        }
                    }
                }
            })
        }
    }

    private fun markComplete(vm: LinuxVm) {
        tvStatusStage.text = "🎉 Virtual Machine Ready!"
        tvPercent.text = "100%"
        installProgressIndicator.progress = 100
        tvBytesInfo.text = "Installed: ~${vm.storageSizeMb} MB"
        tvSpeedInfo.text = "Status: Ready"
        addLog("Virtual Machine ready to boot!")

        btnLaunchVm.isEnabled = true
        btnLaunchVm.text = "▶ Launch Linux Desktop"
        btnLaunchVm.setOnClickListener {
            vmManager.setActiveVmId(vm.id)
            val intent = Intent(this, DesktopActivity::class.java).apply {
                putExtra("EXTRA_VM_ID", vm.id)
                putExtra("EXTRA_DISTRO", vm.distro)
                putExtra("EXTRA_DESKTOP", vm.desktopEnv)
            }
            startActivity(intent)
            finish()
        }
    }
}

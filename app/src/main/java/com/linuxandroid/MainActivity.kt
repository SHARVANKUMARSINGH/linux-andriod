package com.linuxandroid

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.linuxandroid.models.LinuxVm
import com.linuxandroid.setup.SetupActivity
import com.linuxandroid.ui.AboutActivity
import com.linuxandroid.ui.SettingsActivity
import com.linuxandroid.vm.VmInstallActivity
import com.linuxandroid.vm.VmManager
import com.linuxandroid.x11.DesktopActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvVmSummarySubtitle: TextView
    private lateinit var tvMachinesHeader: TextView
    private lateinit var cardEmptyState: MaterialCardView
    private lateinit var btnQuickCreateUbuntu: Button
    private lateinit var vmsContainer: LinearLayout
    private lateinit var fabNewVm: ExtendedFloatingActionButton
    private lateinit var btnHeaderSettings: ImageButton
    private lateinit var btnHeaderAbout: ImageButton

    private lateinit var vmManager: VmManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        vmManager = VmManager(this)

        initViews()
        setupListeners()
        refreshVmList()
    }

    override fun onResume() {
        super.onResume()
        refreshVmList()
    }

    private fun initViews() {
        tvVmSummarySubtitle = findViewById(R.id.tvVmSummarySubtitle)
        tvMachinesHeader = findViewById(R.id.tvMachinesHeader)
        cardEmptyState = findViewById(R.id.cardEmptyState)
        btnQuickCreateUbuntu = findViewById(R.id.btnQuickCreateUbuntu)
        vmsContainer = findViewById(R.id.vmsContainer)
        fabNewVm = findViewById(R.id.fabNewVm)
        btnHeaderSettings = findViewById(R.id.btnHeaderSettings)
        btnHeaderAbout = findViewById(R.id.btnHeaderAbout)
    }

    private fun setupListeners() {
        fabNewVm.setOnClickListener {
            startActivity(Intent(this, SetupActivity::class.java))
        }

        btnQuickCreateUbuntu.setOnClickListener {
            quickCreateUbuntu()
        }

        btnHeaderSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btnHeaderAbout.setOnClickListener {
            startActivity(Intent(this, AboutActivity::class.java))
        }
    }

    private fun quickCreateUbuntu() {
        val ubuntuPreset = VmManager.DISTRO_CATALOG.first { it.id == "ubuntu" }
        val vm = vmManager.createVmConfiguration(
            name = "Ubuntu 24.04 LTS",
            distroPreset = ubuntuPreset,
            architecture = "arm64",
            desktopEnv = "XFCE",
            browser = "Firefox"
        )
        Toast.makeText(this, "Created Ubuntu 24.04 LTS! Starting mirror download...", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, VmInstallActivity::class.java).apply {
            putExtra("EXTRA_VM_ID", vm.id)
        }
        startActivity(intent)
    }

    private fun refreshVmList() {
        val vms = vmManager.getAllVms()
        vmsContainer.removeAllViews()

        if (vms.isEmpty()) {
            cardEmptyState.visibility = View.VISIBLE
            tvMachinesHeader.visibility = View.GONE
            tvVmSummarySubtitle.text = "0 Machines Configured"
        } else {
            cardEmptyState.visibility = View.GONE
            tvMachinesHeader.visibility = View.VISIBLE
            tvVmSummarySubtitle.text = "${vms.size} Virtual Machine${if (vms.size > 1) "s" else ""} • In-App Embedded Display"

            for (vm in vms) {
                val card = buildVmCard(vm)
                vmsContainer.addView(card)
            }
        }
    }

    private fun buildVmCard(vm: LinuxVm): View {
        val card = MaterialCardView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                setMargins(0, 0, 0, 20)
            }
            radius = 36f
            cardElevation = 6f
            setCardBackgroundColor(android.graphics.Color.parseColor("#181824"))
            strokeWidth = 2
            strokeColor = android.graphics.Color.parseColor("#33FFFFFF")
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 28, 32, 28)
        }

        // Top Row: Emoji + Title + Status Badge
        val topRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        val emojiTv = TextView(this).apply {
            text = when {
                vm.distro.contains("Ubuntu", ignoreCase = true) -> "🟠"
                vm.distro.contains("Debian", ignoreCase = true) -> "🔴"
                vm.distro.contains("Alpine", ignoreCase = true) -> "🏔️"
                vm.distro.contains("Arch", ignoreCase = true) -> "🔵"
                vm.distro.contains("Kali", ignoreCase = true) -> "🐉"
                vm.distro.contains("Fedora", ignoreCase = true) -> "🎩"
                else -> "🐧"
            }
            textSize = 28f
            setPadding(0, 0, 18, 0)
        }
        topRow.addView(emojiTv)

        val infoCol = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
        }

        val titleTv = TextView(this).apply {
            text = vm.name
            textSize = 17f
            setTextColor(android.graphics.Color.WHITE)
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        infoCol.addView(titleTv)

        val subtitleTv = TextView(this).apply {
            text = "${vm.distroVersion} • ${vm.architecture.uppercase()} • ${vm.desktopEnv}"
            textSize = 12f
            setTextColor(android.graphics.Color.parseColor("#A0A0B0"))
        }
        infoCol.addView(subtitleTv)

        topRow.addView(infoCol)

        // Status Badge
        val statusBadge = TextView(this).apply {
            if (vm.isInstalled) {
                text = "● Ready"
                setTextColor(android.graphics.Color.parseColor("#00FF66"))
            } else {
                text = "⏳ Needs Download"
                setTextColor(android.graphics.Color.parseColor("#FFBB33"))
            }
            textSize = 11f
            setTypeface(null, android.graphics.Typeface.BOLD)
        }
        topRow.addView(statusBadge)

        layout.addView(topRow)

        // Divider
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 1
            ).apply {
                setMargins(0, 20, 0, 20)
            }
            setBackgroundColor(android.graphics.Color.parseColor("#25FFFFFF"))
        }
        layout.addView(divider)

        // Bottom Actions Row
        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
        }

        // Primary Action Button
        val btnLaunch = Button(this).apply {
            layoutParams = LinearLayout.LayoutParams(0, 110, 1f)
            textSize = 13f
            setTypeface(null, android.graphics.Typeface.BOLD)
            if (vm.isInstalled) {
                text = "▶ Start Desktop"
                setOnClickListener {
                    vmManager.setActiveVmId(vm.id)
                    val intent = Intent(this@MainActivity, DesktopActivity::class.java).apply {
                        putExtra("EXTRA_VM_ID", vm.id)
                        putExtra("EXTRA_DISTRO", vm.distro)
                        putExtra("EXTRA_DESKTOP", vm.desktopEnv)
                    }
                    startActivity(intent)
                }
            } else {
                text = "⬇ Download OS"
                setOnClickListener {
                    val intent = Intent(this@MainActivity, VmInstallActivity::class.java).apply {
                        putExtra("EXTRA_VM_ID", vm.id)
                    }
                    startActivity(intent)
                }
            }
        }
        actionsRow.addView(btnLaunch)

        // Terminal Button
        val btnTerminal = Button(this, null, com.google.android.material.R.attr.materialButtonOutlinedStyle).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 110
            ).apply {
                marginStart = 12
            }
            text = "💻 CLI"
            textSize = 12f
            setOnClickListener {
                vmManager.setActiveVmId(vm.id)
                val intent = Intent(this@MainActivity, SessionActivity::class.java)
                startActivity(intent)
            }
        }
        actionsRow.addView(btnTerminal)

        // Delete Button
        val btnDelete = Button(this, null, com.google.android.material.R.attr.borderlessButtonStyle).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 110
            ).apply {
                marginStart = 8
            }
            text = "🗑"
            textSize = 14f
            setTextColor(android.graphics.Color.parseColor("#FF6B6B"))
            setOnClickListener {
                confirmDeleteVm(vm)
            }
        }
        actionsRow.addView(btnDelete)

        layout.addView(actionsRow)
        card.addView(layout)
        return card
    }

    private fun confirmDeleteVm(vm: LinuxVm) {
        AlertDialog.Builder(this)
            .setTitle("Delete ${vm.name}?")
            .setMessage("This will remove the virtual machine and delete its rootfs container storage. This cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                vmManager.deleteVm(vm.id)
                Toast.makeText(this, "Deleted ${vm.name}", Toast.LENGTH_SHORT).show()
                refreshVmList()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}

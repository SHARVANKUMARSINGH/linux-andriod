package com.linuxandroid.setup

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputLayout
import com.linuxandroid.R
import com.linuxandroid.models.DistroPreset
import com.linuxandroid.vm.VmInstallActivity
import com.linuxandroid.vm.VmManager

class SetupActivity : AppCompatActivity() {

    private lateinit var etVmName: EditText
    private lateinit var distroSpinner: Spinner
    private lateinit var customUrlLayout: TextInputLayout
    private lateinit var etCustomMirrorUrl: EditText
    private lateinit var desktopSpinner: Spinner
    private lateinit var archRadioGroup: RadioGroup
    private lateinit var displayModeRadioGroup: RadioGroup
    private lateinit var tvDistroHint: TextView
    private lateinit var tvGuiFeatureBadge: TextView
    private lateinit var btnCreateVm: Button
    private lateinit var chipGroupCategory: com.google.android.material.chip.ChipGroup

    private lateinit var vmManager: VmManager
    private val allPresets = VmManager.DISTRO_CATALOG
    private var displayedPresets = mutableListOf<DistroPreset>()
    private var isCustomSelected = false

    private val desktopOptions = arrayOf(
        "XFCE (Recommended, Lightweight ~150 MB RAM)",
        "Cinnamon (Windows-style GUI)",
        "GNOME (Modern Full Desktop)",
        "KDE Plasma (Feature Rich)",
        "LXQt (Ultra Lightweight)",
        "MATE (Classic Traditional)",
        "i3WM (Tiling Window Manager)",
        "Terminal Only (CLI Server)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        vmManager = VmManager(this)

        initViews()
        setupCategoryChips()
        filterPresetsByCategory("desktop_gui")
        setupDesktopSpinner()

        btnCreateVm.setOnClickListener {
            handleCreateVm()
        }
    }

    private fun initViews() {
        etVmName = findViewById(R.id.etVmName)
        distroSpinner = findViewById(R.id.distroSpinner)
        customUrlLayout = findViewById(R.id.customUrlLayout)
        etCustomMirrorUrl = findViewById(R.id.etCustomMirrorUrl)
        desktopSpinner = findViewById(R.id.desktopSpinner)
        archRadioGroup = findViewById(R.id.archRadioGroup)
        displayModeRadioGroup = findViewById(R.id.displayModeRadioGroup)
        tvDistroHint = findViewById(R.id.tvDistroHint)
        tvGuiFeatureBadge = findViewById(R.id.tvGuiFeatureBadge)
        btnCreateVm = findViewById(R.id.btnCreateVm)
        chipGroupCategory = findViewById(R.id.chipGroupCategory)
    }

    private fun setupCategoryChips() {
        chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            when {
                checkedIds.contains(R.id.chipGuiDesktop) -> {
                    isCustomSelected = false
                    customUrlLayout.visibility = View.GONE
                    tvGuiFeatureBadge.visibility = View.VISIBLE
                    tvGuiFeatureBadge.text = "✨ Pre-Configured Graphical Desktop: Includes Start Menu, Taskbar, Web Browser, Audio & Window Manager"
                    filterPresetsByCategory("desktop_gui")
                }
                checkedIds.contains(R.id.chipMinimalLts) -> {
                    isCustomSelected = false
                    customUrlLayout.visibility = View.GONE
                    tvGuiFeatureBadge.visibility = View.VISIBLE
                    tvGuiFeatureBadge.text = "⚡ Minimal / Server LTS: Ultra-small footprint, low RAM, rock-solid stability"
                    filterPresetsByCategory("minimal_lts")
                }
                checkedIds.contains(R.id.chipSecurity) -> {
                    isCustomSelected = false
                    customUrlLayout.visibility = View.GONE
                    tvGuiFeatureBadge.visibility = View.VISIBLE
                    tvGuiFeatureBadge.text = "🛡️ Cybersecurity Suite: Pre-configured Dragon graphical tools & penetration testing packages"
                    filterPresetsByCategory("security")
                }
                checkedIds.contains(R.id.chipCustom) -> {
                    isCustomSelected = true
                    customUrlLayout.visibility = View.VISIBLE
                    tvGuiFeatureBadge.visibility = View.GONE
                    tvDistroHint.text = "Enter direct download URL for custom rootfs tarball or ISO"
                    btnCreateVm.text = "Create & Download Custom OS"
                }
            }
        }
    }

    private fun filterPresetsByCategory(category: String) {
        displayedPresets.clear()
        displayedPresets.addAll(allPresets.filter { it.category == category })

        val displayList = displayedPresets.map {
            "${it.iconEmoji} ${it.name} (${if (it.isLts) "LTS • ~${it.approxDownloadMb} MB" else "~${it.approxDownloadMb} MB"})"
        }

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, displayList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        distroSpinner.adapter = adapter

        // Default to first item
        distroSpinner.setSelection(0)

        distroSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in displayedPresets.indices) {
                    val selected = displayedPresets[position]
                    tvDistroHint.text = selected.shortDesc
                    btnCreateVm.text = "Create & Download ${selected.name} (~${selected.approxDownloadMb} MB)"
                    etVmName.setText(selected.name)

                    // Auto-select recommended desktop environment
                    val defaultDE = selected.defaultDesktop
                    val deIndex = desktopOptions.indexOfFirst { it.startsWith(defaultDE, ignoreCase = true) }
                    if (deIndex >= 0) {
                        desktopSpinner.setSelection(deIndex)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setupDesktopSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, desktopOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        desktopSpinner.adapter = adapter
        desktopSpinner.setSelection(0) // Default to XFCE
    }

    private fun handleCreateVm() {
        val selectedDistroIndex = distroSpinner.selectedItemPosition
        val vmName = etVmName.text.toString().trim().ifBlank { "Linux VM" }
        val desktop = desktopSpinner.selectedItem.toString().split(" ").first()
        val arch = if (findViewById<RadioButton>(R.id.arm64Radio).isChecked) "arm64" else "x86_64"

        val displayMode = when {
            findViewById<RadioButton>(R.id.embeddedRadio).isChecked -> "embedded"
            findViewById<RadioButton>(R.id.termuxX11Radio).isChecked -> "termux_x11"
            else -> "cli"
        }

        val customUrl = if (isCustomSelected) {
            etCustomMirrorUrl.text.toString().trim().ifBlank { null }
        } else null

        val preset = if (!isCustomSelected && selectedDistroIndex in displayedPresets.indices) {
            displayedPresets[selectedDistroIndex]
        } else {
            allPresets[0]
        }

        // Create the new VM configuration
        val vm = vmManager.createVmConfiguration(
            name = vmName,
            distroPreset = preset,
            architecture = arch,
            desktopEnv = desktop,
            browser = "Firefox",
            customMirrorUrl = customUrl
        )
        vm.displayMode = displayMode
        vmManager.saveVm(vm)

        Toast.makeText(this, "Created ${vm.name}! Starting mirror download...", Toast.LENGTH_SHORT).show()

        // Transition to live install & download screen
        val intent = Intent(this, VmInstallActivity::class.java).apply {
            putExtra("EXTRA_VM_ID", vm.id)
        }
        startActivity(intent)
        finish()
    }
}

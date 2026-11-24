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
    private lateinit var btnCreateVm: Button

    private lateinit var vmManager: VmManager
    private val presets = VmManager.DISTRO_CATALOG

    private val desktopOptions = arrayOf(
        "XFCE (Recommended, Lightweight ~150 MB RAM)",
        "GNOME (Modern Full Desktop)",
        "KDE Plasma (Feature Rich)",
        "LXQt (Ultra Lightweight)",
        "MATE (Classic Traditional)",
        "Cinnamon (Sleek)",
        "i3WM (Tiling Window Manager)",
        "Terminal Only (CLI Server)"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        vmManager = VmManager(this)

        initViews()
        setupDistroSpinner()
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
        btnCreateVm = findViewById(R.id.btnCreateVm)
    }

    private fun setupDistroSpinner() {
        val distroDisplayList = presets.map {
            "${it.iconEmoji} ${it.name} (${if (it.isLts) "LTS • ~${it.approxDownloadMb} MB" else "~${it.approxDownloadMb} MB"})"
        }.toMutableList()
        distroDisplayList.add("💿 Custom Mirror / ISO URL")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, distroDisplayList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        distroSpinner.adapter = adapter

        // Default to Ubuntu 24.04 LTS (index 0)
        distroSpinner.setSelection(0)

        distroSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position < presets.size) {
                    val selected = presets[position]
                    customUrlLayout.visibility = View.GONE
                    tvDistroHint.text = selected.shortDesc
                    btnCreateVm.text = "Create & Download ${selected.name} (~${selected.approxDownloadMb} MB)"
                    if (etVmName.text.isBlank() || presets.any { etVmName.text.toString().startsWith(it.name) }) {
                        etVmName.setText(selected.name)
                    }
                } else {
                    // Custom mirror chosen
                    customUrlLayout.visibility = View.VISIBLE
                    tvDistroHint.text = "Enter direct download URL for rootfs tarball or ISO"
                    btnCreateVm.text = "Create & Download Custom OS"
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

        val customUrl = if (selectedDistroIndex >= presets.size) {
            etCustomMirrorUrl.text.toString().trim().ifBlank { null }
        } else null

        val preset = if (selectedDistroIndex < presets.size) presets[selectedDistroIndex] else presets[0]

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

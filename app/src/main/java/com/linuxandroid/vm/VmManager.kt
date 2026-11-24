package com.linuxandroid.vm

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.linuxandroid.models.DistroPreset
import com.linuxandroid.models.LinuxVm
import java.io.File
import java.util.UUID

class VmManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "LinuxVmMakerPrefs"
        private const val KEY_VM_LIST = "configured_vms"
        private const val KEY_ACTIVE_VM_ID = "active_vm_id"

        val DISTRO_CATALOG = listOf(
            DistroPreset(
                id = "ubuntu",
                name = "Ubuntu 24.04 LTS",
                version = "Noble Numbat",
                shortDesc = "Recommended: Modern LTS, highly compatible, small base (~45 MB)",
                isLts = true,
                isRecommended = true,
                arm64Url = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.1-base-arm64.tar.gz",
                x86_64Url = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.1-base-amd64.tar.gz",
                defaultDesktop = "XFCE",
                approxDownloadMb = 45,
                iconEmoji = "🟠"
            ),
            DistroPreset(
                id = "debian",
                name = "Debian 12 Bookworm",
                version = "12 (Stable LTS)",
                shortDesc = "Ultra-stable & lightweight (~35 MB download)",
                isLts = true,
                isRecommended = false,
                arm64Url = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.1-base-arm64.tar.gz",
                x86_64Url = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.1-base-amd64.tar.gz",
                defaultDesktop = "XFCE",
                approxDownloadMb = 35,
                iconEmoji = "🔴"
            ),
            DistroPreset(
                id = "alpine",
                name = "Alpine Linux 3.21",
                version = "3.21 LTS",
                shortDesc = "Ultra-low footprint (~4 MB download) with musl & busybox",
                isLts = true,
                isRecommended = false,
                arm64Url = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/aarch64/alpine-minirootfs-3.21.0-aarch64.tar.gz",
                x86_64Url = "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/x86_64/alpine-minirootfs-3.21.0-x86_64.tar.gz",
                defaultDesktop = "XFCE",
                approxDownloadMb = 4,
                iconEmoji = "🏔️"
            ),
            DistroPreset(
                id = "arch",
                name = "Arch Linux",
                version = "Rolling Release",
                shortDesc = "Bleeding edge packages, pacman package manager",
                isLts = false,
                isRecommended = false,
                arm64Url = "http://os.archlinuxarm.org/os/ArchLinuxARM-aarch64-latest.tar.gz",
                x86_64Url = "https://geo.mirror.pkgbuild.com/iso/latest/archlinux-bootstrap-x86_64.tar.zst",
                defaultDesktop = "XFCE",
                approxDownloadMb = 42,
                iconEmoji = "🔵"
            ),
            DistroPreset(
                id = "kali",
                name = "Kali Linux",
                version = "2024.3",
                shortDesc = "Penetration testing and security auditing tools",
                isLts = false,
                isRecommended = false,
                arm64Url = "https://images.kali.org/arm-images/kali-linux-2024.3-rootfs-arm64.tar.xz",
                x86_64Url = "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.1-base-amd64.tar.gz",
                defaultDesktop = "XFCE",
                approxDownloadMb = 65,
                iconEmoji = "🐉"
            ),
            DistroPreset(
                id = "fedora",
                name = "Fedora 41",
                version = "41 Generic",
                shortDesc = "Red Hat upstream with latest Linux advancements",
                isLts = false,
                isRecommended = false,
                arm64Url = "https://download.fedoraproject.org/pub/fedora/linux/releases/41/Container/aarch64/images/Fedora-Container-Base-Generic.aarch64-41-1.4.tar.xz",
                x86_64Url = "https://download.fedoraproject.org/pub/fedora/linux/releases/41/Container/x86_64/images/Fedora-Container-Base-Generic.x86_64-41-1.4.tar.xz",
                defaultDesktop = "GNOME",
                approxDownloadMb = 55,
                iconEmoji = "🎩"
            )
        )
    }

    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getAllVms(): List<LinuxVm> {
        val json = prefs.getString(KEY_VM_LIST, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<LinuxVm>>() {}.type
            gson.fromJson(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getVm(id: String): LinuxVm? {
        return getAllVms().find { it.id == id }
    }

    fun saveVm(vm: LinuxVm) {
        val current = getAllVms().toMutableList()
        val index = current.indexOfFirst { it.id == vm.id }
        if (index >= 0) {
            current[index] = vm
        } else {
            current.add(vm)
        }
        val json = gson.toJson(current)
        prefs.edit().putString(KEY_VM_LIST, json).apply()

        // Set as active VM
        setActiveVmId(vm.id)
    }

    fun deleteVm(id: String): Boolean {
        val current = getAllVms().toMutableList()
        val vm = current.find { it.id == id } ?: return false

        // Remove container directory from disk
        try {
            val rootfsDir = File(vm.rootfsPath)
            if (rootfsDir.exists()) {
                rootfsDir.deleteRecursively()
            }
            val startScript = File(vm.startScriptPath)
            if (startScript.exists()) {
                startScript.delete()
            }
        } catch (e: Exception) {
            // Ignore deletion errors
        }

        current.removeAll { it.id == id }
        val json = gson.toJson(current)
        prefs.edit().putString(KEY_VM_LIST, json).apply()

        if (getActiveVmId() == id) {
            val next = current.firstOrNull()?.id
            if (next != null) {
                setActiveVmId(next)
            } else {
                prefs.edit().remove(KEY_ACTIVE_VM_ID).apply()
            }
        }
        return true
    }

    fun getActiveVmId(): String? = prefs.getString(KEY_ACTIVE_VM_ID, null)

    fun setActiveVmId(id: String) {
        prefs.edit().putString(KEY_ACTIVE_VM_ID, id).apply()
    }

    fun getActiveVm(): LinuxVm? {
        val id = getActiveVmId() ?: return getAllVms().firstOrNull()
        return getVm(id) ?: getAllVms().firstOrNull()
    }

    /**
     * Creates a new VM configuration initialized with paths in the app's files directory.
     */
    fun createVmConfiguration(
        name: String,
        distroPreset: DistroPreset,
        architecture: String,
        desktopEnv: String,
        browser: String,
        customMirrorUrl: String? = null
    ): LinuxVm {
        val id = "vm_" + UUID.randomUUID().toString().take(8)
        val vmDir = File(context.filesDir, "vms/$id")
        val rootfsDir = File(vmDir, "rootfs")
        val startScript = File(vmDir, "start-linux.sh")

        val mirrorUrl = if (!customMirrorUrl.isNullOrBlank()) {
            customMirrorUrl
        } else {
            if (architecture == "arm64") distroPreset.arm64Url else distroPreset.x86_64Url
        }

        val vm = LinuxVm(
            id = id,
            name = name.ifBlank { "${distroPreset.name} ($desktopEnv)" },
            distro = distroPreset.name,
            distroVersion = distroPreset.version,
            architecture = architecture,
            desktopEnv = desktopEnv,
            browser = browser,
            displayMode = "embedded",
            mirrorUrl = mirrorUrl,
            rootfsPath = rootfsDir.absolutePath,
            startScriptPath = startScript.absolutePath,
            isInstalled = false,
            storageSizeMb = 0
        )

        saveVm(vm)
        return vm
    }
}

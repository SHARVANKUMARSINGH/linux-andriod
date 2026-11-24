package com.linuxandroid.models

import java.io.Serializable

/**
 * Represents a configured Linux Virtual Machine / Container instance.
 */
data class LinuxVm(
    val id: String,
    var name: String,
    val distro: String,
    val distroVersion: String,
    val architecture: String,
    var desktopEnv: String,
    var browser: String = "Firefox",
    var displayMode: String = "embedded",
    var mirrorUrl: String,
    val rootfsPath: String,
    val startScriptPath: String,
    var isInstalled: Boolean = false,
    var storageSizeMb: Long = 0,
    val createdAt: Long = System.currentTimeMillis()
) : Serializable

data class DistroPreset(
    val id: String,
    val name: String,
    val version: String,
    val shortDesc: String,
    val isLts: Boolean,
    val isRecommended: Boolean,
    val isGuiBased: Boolean = true,
    val category: String = "desktop_gui",
    val arm64Url: String,
    val x86_64Url: String,
    val defaultDesktop: String,
    val approxDownloadMb: Int,
    val iconEmoji: String
)

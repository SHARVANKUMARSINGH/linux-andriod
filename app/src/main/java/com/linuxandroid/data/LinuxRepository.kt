package com.linuxandroid.data

import com.linuxandroid.models.Environment
import com.linuxandroid.models.LinuxDistro
import com.linuxandroid.models.Architecture

class LinuxRepository {
    private val distros = listOf(
        LinuxDistro(
            id = "debian",
            name = "debian",
            description = "Debian Stable - A highly stable and popular Linux distribution",
            iconRes = android.R.drawable.ic_dialog_info
        ),
        LinuxDistro(
            id = "ubuntu",
            name = "ubuntu",
            description = "Ubuntu LTS - A user-friendly Linux distribution",
            iconRes = android.R.drawable.ic_dialog_email
        ),
        LinuxDistro(
            id = "arch",
            name = "arch",
            description = "Arch Linux - A lightweight and flexible Linux distribution",
            iconRes = android.R.drawable.ic_dialog_dialer
        ),
        LinuxDistro(
            id = "alpine",
            name = "alpine",
            description = "Alpine Linux - A security-oriented, lightweight Linux distribution",
            iconRes = android.R.drawable.ic_dialog_map
        ),
        LinuxDistro(
            id = "fedora",
            name = "fedora",
            description = "Fedora Workstation - A bleeding-edge Linux distribution",
            iconRes = android.R.drawable.ic_dialog_alert
        ),
        LinuxDistro(
            id = "pop",
            name = "pop",
            description = "Pop!_OS - An Ubuntu-based distribution developed by System76",
            iconRes = android.R.drawable.btn_star
        ),
        LinuxDistro(
            id = "manjaro",
            name = "manjaro",
            description = "Manjaro - A user-friendly Linux distribution based on Arch",
            iconRes = android.R.drawable.ic_menu_gallery
        )
    )

    private val environments = listOf(
        Environment(
            id = "gnome",
            name = "gnome",
            displayName = "GNOME",
            iconRes = android.R.drawable.ic_dialog_info
        ),
        Environment(
            id = "kde",
            name = "kde-plasma",
            displayName = "KDE Plasma",
            iconRes = android.R.drawable.ic_dialog_map
        ),
        Environment(
            id = "xfce",
            name = "xfce",
            displayName = "XFCE",
            iconRes = android.R.drawable.ic_dialog_email
        ),
        Environment(
            id = "cinnamon",
            name = "cinnamon",
            displayName = "Cinnamon",
            iconRes = android.R.drawable.ic_dialog_alert
        )
    )

    fun getDistros(): List<LinuxDistro> = distros

    fun getEnvironment(id: String): Environment? = environments.find { it.id == id }

    fun getEnvironments(): List<Environment> = environments

    private val installations = mutableMapOf<String, Installation>()

    data class Installation(
        val distro: LinuxDistro,
        val environment: Environment,
        val architecture: Architecture
    )

    fun saveInstallation(distroId: String, environmentId: String, architecture: Architecture) {
        val distro = distros.find { it.id == distroId }!!
        val environment = environments.find { it.id == environmentId }!!
        installations[distroId] = Installation(distro, environment, architecture)
    }

    fun getInstallation(distroId: String): Installation? = installations[distroId]

    fun hasInstallation(distroId: String): Boolean = installations.containsKey(distroId)
}

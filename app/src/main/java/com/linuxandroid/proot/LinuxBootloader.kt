package com.linuxandroid.proot

import java.io.File

/**
 * LinuxBootloader generates real PRoot-based Linux container boot scripts,
 * environment configuration, and X11 display linkage similar to Andronix and AnLinux.
 *
 * PRoot uses ptrace to simulate a real Linux root environment (chroot, mount, setuid)
 * completely in Android user space without requiring root permissions.
 */
object LinuxBootloader {

    /**
     * Returns the official rootfs URL for the selected distribution and architecture.
     */
    fun getRootfsUrl(distroName: String, arch: String): String {
        val isArm64 = arch.equals("arm64", ignoreCase = true) || arch.equals("aarch64", ignoreCase = true)
        val normalized = distroName.lowercase()

        return when {
            normalized.contains("ubuntu") || normalized.contains("mint") || normalized.contains("zorin") || normalized.contains("pop") -> {
                if (isArm64) {
                    "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.5-base-arm64.tar.gz"
                } else {
                    "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.5-base-amd64.tar.gz"
                }
            }
            normalized.contains("debian") -> {
                if (isArm64) {
                    "https://github.com/termux/proot-distro/releases/download/v4.29.0/debian-trixie-aarch64-pd-v4.29.0.tar.xz"
                } else {
                    "https://github.com/termux/proot-distro/releases/download/v4.29.0/debian-trixie-x86_64-pd-v4.29.0.tar.xz"
                }
            }
            normalized.contains("alpine") -> {
                if (isArm64) {
                    "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/aarch64/alpine-minirootfs-3.21.0-aarch64.tar.gz"
                } else {
                    "https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/x86_64/alpine-minirootfs-3.21.0-x86_64.tar.gz"
                }
            }
            normalized.contains("arch") -> {
                if (isArm64) {
                    "https://github.com/termux/proot-distro/releases/download/v4.29.0/archlinux-aarch64-pd-v4.29.0.tar.xz"
                } else {
                    "https://github.com/termux/proot-distro/releases/download/v4.29.0/archlinux-x86_64-pd-v4.29.0.tar.xz"
                }
            }
            normalized.contains("kali") -> {
                if (isArm64) {
                    "https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-arm64.tar.xz"
                } else {
                    "https://kali.download/nethunter-images/current/rootfs/kali-nethunter-rootfs-minimal-amd64.tar.xz"
                }
            }
            normalized.contains("fedora") -> {
                if (isArm64) {
                    "https://github.com/termux/proot-distro/releases/download/v4.29.0/fedora-aarch64-pd-v4.29.0.tar.xz"
                } else {
                    "https://github.com/termux/proot-distro/releases/download/v4.29.0/fedora-x86_64-pd-v4.29.0.tar.xz"
                }
            }
            else -> {
                if (isArm64) {
                    "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.5-base-arm64.tar.gz"
                } else {
                    "https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.5-base-amd64.tar.gz"
                }
            }
        }
    }

    /**
     * Generates a real PRoot boot script with X11 socket bindings, audio, and desktop environment startup.
     */
    fun generateStartScript(
        targetDir: String,
        distroName: String,
        desktopEnv: String,
        isX11: Boolean,
        username: String = "root"
    ): String {
        val desktopCommand = getDesktopStartupCommand(desktopEnv)

        return """
            #!/bin/bash
            # ==============================================================================
            # Real Linux on Android Bootloader (PRoot + Termux:X11)
            # Distribution: $distroName
            # Desktop: $desktopEnv
            # User: $username
            # ==============================================================================
            
            ROOTFS="$targetDir"
            export PULSE_SERVER=127.0.0.1:4713
            export DISPLAY=:0
            export TMPDIR=/tmp
            export HOME=/$username
            export USER=$username
            export SHELL=/bin/bash
            export TERM=xterm-256color
            export LANG=C.UTF-8
            export MOZ_FAKE_NO_SANDBOX=1
            
            # Setup X11 socket directory
            mkdir -p /tmp/.X11-unix
            chmod 1777 /tmp/.X11-unix
            
            # Setup DNS nameserver in rootfs
            mkdir -p "${'$'}ROOTFS/etc"
            echo "nameserver 1.1.1.1" > "${'$'}ROOTFS/etc/resolv.conf"
            echo "nameserver 8.8.8.8" >> "${'$'}ROOTFS/etc/resolv.conf"
            
            # PRoot execution command with full device/system bindings
            PROOT_BIN="proot"
            if ! command -v proot >/dev/null 2>&1; then
                if [ -f "/data/data/com.termux/files/usr/bin/proot" ]; then
                    PROOT_BIN="/data/data/com.termux/files/usr/bin/proot"
                fi
            fi
            
            PROOT_CMD="${'$'}PROOT_BIN \
              --kill-on-exit \
              --link2symlink \
              -0 \
              -r ${'$'}ROOTFS \
              -b /dev \
              -b /proc \
              -b /sys \
              -b /tmp/.X11-unix:/tmp/.X11-unix \
              -b /sdcard:/sdcard \
              -w /$username \
              /usr/bin/env -i \
              HOME=/$username \
              USER=$username \
              PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \
              TERM=xterm-256color \
              LANG=C.UTF-8 \
              DISPLAY=:0 \
              PULSE_SERVER=127.0.0.1:4713 \
              MOZ_FAKE_NO_SANDBOX=1"
              
            ${if (isX11 && desktopCommand.isNotEmpty()) {
                """
                echo "Starting $distroName with $desktopEnv on Termux:X11 (DISPLAY=:0)..."
                ${'$'}PROOT_CMD /bin/bash -c "$desktopCommand"
                """.trimIndent()
            } else {
                """
                echo "Starting $distroName interactive shell..."
                ${'$'}PROOT_CMD /bin/bash --login
                """.trimIndent()
            }}
        """.trimIndent()
    }

    /**
     * Returns the desktop launcher command for the given desktop environment.
     */
    fun getDesktopStartupCommand(desktopEnv: String): String {
        return when (desktopEnv.lowercase()) {
            "xfce", "xfce4" -> "dbus-launch --exit-with-session startxfce4"
            "gnome" -> "dbus-launch --exit-with-session gnome-session"
            "kde", "kde plasma", "plasma" -> "dbus-launch --exit-with-session startplasma-x11"
            "lxqt" -> "dbus-launch --exit-with-session startlxqt"
            "mate" -> "dbus-launch --exit-with-session mate-session"
            "cinnamon" -> "dbus-launch --exit-with-session cinnamon-session"
            "i3", "i3wm" -> "dbus-launch --exit-with-session i3"
            "sway" -> "sway"
            else -> ""
        }
    }

    /**
     * Generates a one-line installer command for Termux/Andronix style execution.
     */
    fun generateQuickInstallCommand(distro: String, desktop: String): String {
        val distroId = distro.lowercase().split(" ").first()
        val desktopId = desktop.lowercase().split(" ").first()
        return "curl -sSL https://raw.githubusercontent.com/SHARVANKUMARSINGH/linux-andriod/main/scripts/proot_installer.sh | bash -s -- --distro $distroId --desktop $desktopId"
    }
}

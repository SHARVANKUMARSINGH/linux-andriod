#!/bin/bash
# ==============================================================================
# Linux on Android - Real PRoot & Termux:X11 Desktop Installer
# (Compatible with Andronix / AnLinux / Termux)
# ==============================================================================

set -e

DISTRO="ubuntu"
DESKTOP="xfce"
ARCH="arm64"
INSTALL_DIR="${HOME:-/data/data/com.termux/files/home}/linux-android"

# Parse arguments
while [[ "$#" -gt 0 ]]; do
    case $1 in
        --distro) DISTRO="$2"; shift ;;
        --desktop) DESKTOP="$2"; shift ;;
        --arch) ARCH="$2"; shift ;;
        --dir) INSTALL_DIR="$2"; shift ;;
        *) echo "Unknown parameter: $1"; exit 1 ;;
    esac
    shift
done

echo "============================================================"
echo "    🐧 Linux on Android PRoot & Termux:X11 Installer       "
echo "============================================================"
echo "Target Distribution: $DISTRO"
echo "Desktop Environment: $DESKTOP"
echo "Architecture:        $ARCH"
echo "Install Directory:   $INSTALL_DIR"
echo "============================================================"

mkdir -p "$INSTALL_DIR"

# 1. Update Termux repositories and install required packages
if command -v pkg >/dev/null 2>&1; then
    echo "[1/4] Installing core dependencies (proot, x11-repo, pulseaudio, wget)..."
    pkg update -y
    pkg install -y proot wget curl tar xz-utils pulseaudio
    # Enable x11-repo for Termux:X11 integration
    pkg install -y x11-repo || true
    pkg install -y termux-x11-nightly || true
fi

# 2. Setup root filesystem
echo "[2/4] Setting up $DISTRO root filesystem..."
ROOTFS_DIR="$INSTALL_DIR/rootfs"
mkdir -p "$ROOTFS_DIR"

# Rootfs URLs
case "$DISTRO" in
    ubuntu)
        ROOTFS_URL="https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.1-base-arm64.tar.gz"
        ;;
    debian)
        ROOTFS_URL="https://github.com/debuerreotype/docker-debian-artifacts/raw/dist-arm64/12/rootfs.tar.xz"
        ;;
    alpine)
        ROOTFS_URL="https://dl-cdn.alpinelinux.org/alpine/v3.21/releases/aarch64/alpine-minirootfs-3.21.0-aarch64.tar.gz"
        ;;
    arch)
        ROOTFS_URL="http://os.archlinuxarm.org/os/ArchLinuxARM-aarch64-latest.tar.gz"
        ;;
    *)
        ROOTFS_URL="https://cdimage.ubuntu.com/ubuntu-base/releases/24.04/release/ubuntu-base-24.04.1-base-arm64.tar.gz"
        ;;
esac

ARCHIVE_FILE="$INSTALL_DIR/rootfs.tar.gz"
if [ ! -d "$ROOTFS_DIR/etc" ]; then
    echo "Downloading rootfs from: $ROOTFS_URL..."
    curl -L -o "$ARCHIVE_FILE" "$ROOTFS_URL"
    echo "Extracting rootfs (this may take a few moments)..."
    tar -xf "$ARCHIVE_FILE" -C "$ROOTFS_DIR" --exclude="dev/*" 2>/dev/null || tar -xf "$ARCHIVE_FILE" -C "$ROOTFS_DIR"
    rm -f "$ARCHIVE_FILE"
fi

# Setup DNS in rootfs
mkdir -p "$ROOTFS_DIR/etc"
echo "nameserver 1.1.1.1" > "$ROOTFS_DIR/etc/resolv.conf"
echo "nameserver 8.8.8.8" >> "$ROOTFS_DIR/etc/resolv.conf"

# 3. Create Desktop startup script
echo "[3/4] Configuring $DESKTOP desktop environment & Termux:X11 display..."

DESKTOP_EXEC="startxfce4"
case "$DESKTOP" in
    xfce|xfce4) DESKTOP_EXEC="dbus-launch --exit-with-session startxfce4" ;;
    gnome)      DESKTOP_EXEC="dbus-launch --exit-with-session gnome-session" ;;
    kde|plasma) DESKTOP_EXEC="dbus-launch --exit-with-session startplasma-x11" ;;
    lxqt)       DESKTOP_EXEC="dbus-launch --exit-with-session startlxqt" ;;
    mate)       DESKTOP_EXEC="dbus-launch --exit-with-session mate-session" ;;
    cinnamon)   DESKTOP_EXEC="dbus-launch --exit-with-session cinnamon-session" ;;
    i3|i3wm)    DESKTOP_EXEC="dbus-launch --exit-with-session i3" ;;
esac

# Create boot script
START_SCRIPT="$INSTALL_DIR/start-$DISTRO.sh"
cat > "$START_SCRIPT" << EOF
#!/bin/bash
export PULSE_SERVER=127.0.0.1:4713
export DISPLAY=:0
export MOZ_FAKE_NO_SANDBOX=1

# Start PulseAudio for sound support
pulseaudio --start --load="module-native-protocol-tcp auth-ip-acl=127.0.0.1 auth-anonymous=1" --exit-idle-time=-1 2>/dev/null || true

# Start Termux:X11 display if available
if command -v termux-x11 >/dev/null 2>&1; then
    am start --user 0 -n com.termux.x11/com.termux.x11.MainActivity 2>/dev/null || true
    termux-x11 :0 >/dev/null 2>&1 &
    sleep 2
fi

echo "Booting $DISTRO ($DESKTOP) via PRoot on Display :0..."
proot \\
  --kill-on-exit \\
  --link2symlink \\
  -0 \\
  -r "$ROOTFS_DIR" \\
  -b /dev \\
  -b /proc \\
  -b /sys \\
  -b /tmp/.X11-unix:/tmp/.X11-unix \\
  -b /sdcard:/sdcard \\
  -w /root \\
  /usr/bin/env -i \\
  HOME=/root \\
  USER=root \\
  PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin \\
  TERM=xterm-256color \\
  LANG=C.UTF-8 \\
  DISPLAY=:0 \\
  PULSE_SERVER=127.0.0.1:4713 \\
  MOZ_FAKE_NO_SANDBOX=1 \\
  /bin/bash -c "$DESKTOP_EXEC"
EOF

chmod +x "$START_SCRIPT"

echo "[4/4] Installation completed successfully!"
echo ""
echo "============================================================"
echo "  To launch your Linux desktop:"
echo "  1. Open Termux:X11 app (or ensure Display :0 is active)"
echo "  2. Run: bash $START_SCRIPT"
echo "============================================================"

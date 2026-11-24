#!/bin/bash
# ==============================================================================
# Bootstrap Downloader for Linux on Android
# ==============================================================================
# Downloads rootfs archives for supported Linux distributions
# ==============================================================================

set -e

DISTRO="${1:-ubuntu}"
ARCH="${2:-arm64}"
TARGET_DIR="${3:-/storage/emulated/0/LinuxAndroid}"

echo "=== Linux on Android Bootstrap Downloader ==="
echo "Distro: $DISTRO"
echo "Architecture: $ARCH"
echo "Target Directory: $TARGET_DIR"

mkdir -p "$TARGET_DIR"

case "$DISTRO" in
    ubuntu)
        URL="https://github.com/termux/termux-packages/releases/latest/download/bootstrap-${ARCH}.tar.xz"
        ;;
    debian)
        URL="https://github.com/termux/termux-packages/releases/latest/download/bootstrap-${ARCH}.tar.xz"
        ;;
    alpine)
        URL="https://dl-cdn.alpinelinux.org/alpine/v3.20/releases/${ARCH}/alpine-minirootfs-3.20.0-${ARCH}.tar.gz"
        ;;
    arch)
        URL="http://os.archlinuxarm.org/os/ArchLinuxARM-aarch64-latest.tar.gz"
        ;;
    *)
        URL="https://github.com/termux/termux-packages/releases/latest/download/bootstrap-${ARCH}.tar.xz"
        ;;
esac

echo "Downloading from: $URL"
DOWNLOAD_TARGET="$TARGET_DIR/rootfs.tar.xz"

if command -v curl >/dev/null 2>&1; then
    curl -L -o "$DOWNLOAD_TARGET" "$URL"
elif command -v wget >/dev/null 2>&1; then
    wget -O "$DOWNLOAD_TARGET" "$URL"
else
    echo "Error: neither curl nor wget available" >&2
    exit 1
fi

echo "Extracting rootfs to $TARGET_DIR..."
tar -xf "$DOWNLOAD_TARGET" -C "$TARGET_DIR"
rm -f "$DOWNLOAD_TARGET"

echo "Bootstrap successfully installed!"

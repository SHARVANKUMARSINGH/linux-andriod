#!/bin/bash
# ==============================================================================
# Linux on Android Package Manager Helper
# ==============================================================================
# Manages package installations inside the chroot environment
# ==============================================================================

set -e

ACTION="${1:-update}"
PACKAGES="${@:2}"

case "$ACTION" in
    update)
        echo "Updating package lists..."
        if command -v apt-get >/dev/null 2>&1; then
            apt-get update
        elif command -v pacman >/dev/null 2>&1; then
            pacman -Sy
        elif command -v apk >/dev/null 2>&1; then
            apk update
        elif command -v dnf >/dev/null 2>&1; then
            dnf check-update || true
        fi
        ;;
    install)
        echo "Installing packages: $PACKAGES"
        if command -v apt-get >/dev/null 2>&1; then
            DEBIAN_FRONTEND=noninteractive apt-get install -y $PACKAGES
        elif command -v pacman >/dev/null 2>&1; then
            pacman -S --noconfirm $PACKAGES
        elif command -v apk >/dev/null 2>&1; then
            apk add $PACKAGES
        elif command -v dnf >/dev/null 2>&1; then
            dnf install -y $PACKAGES
        fi
        ;;
    clean)
        echo "Cleaning package caches..."
        if command -v apt-get >/dev/null 2>&1; then
            apt-get clean
        elif command -v pacman >/dev/null 2>&1; then
            pacman -Sc --noconfirm
        elif command -v apk >/dev/null 2>&1; then
            apk cache clean
        elif command -v dnf >/dev/null 2>&1; then
            dnf clean all
        fi
        ;;
    *)
        echo "Usage: $0 {update|install [pkgs...]|clean}"
        exit 1
        ;;
esac

#!/usr/bin/env bash
# ==============================================================================
# Gradle Wrapper Script
# ==============================================================================

set -e

APP_HOME="$(cd "$(dirname "$0")" && pwd -P)"
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

# Determine JVM
if [ -n "$JAVA_HOME" ] ; then
    JAVACMD="$JAVA_HOME/bin/java"
else
    JAVACMD="java"
fi

# Locate gradle wrapper properties
PROP_FILE="$APP_HOME/gradle/wrapper/gradle-wrapper.properties"
GRADLE_VERSION="8.11.1"
if [ -f "$PROP_FILE" ]; then
    VERSION_MATCH=$(grep "distributionUrl" "$PROP_FILE" | grep -o "gradle-[0-9.]*" | head -1 | sed 's/gradle-//')
    if [ -n "$VERSION_MATCH" ]; then
        GRADLE_VERSION="$VERSION_MATCH"
    fi
fi

# Cache directory for gradle
GRADLE_USER_HOME="${GRADLE_USER_HOME:-$HOME/.gradle}"
WRAPPER_DIR="$GRADLE_USER_HOME/wrapper/dists/gradle-$GRADLE_VERSION-bin"
GRADLE_BIN=""

# Check if gradle is already extracted
if [ -d "$WRAPPER_DIR" ]; then
    FOUND=$(find "$WRAPPER_DIR" -name "gradle" -type f -perm -111 2>/dev/null | head -1)
    if [ -x "$FOUND" ]; then
        GRADLE_BIN="$FOUND"
    fi
fi

# If system gradle exists and is sufficient, use it
if [ -z "$GRADLE_BIN" ] && command -v gradle >/dev/null 2>&1; then
    GRADLE_BIN="$(command -v gradle)"
fi

# If still not found, download distribution
if [ -z "$GRADLE_BIN" ]; then
    echo "Downloading Gradle $GRADLE_VERSION..."
    TMP_DIR="$(mktemp -d)"
    ZIP_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"
    ZIP_FILE="$TMP_DIR/gradle.zip"
    
    if command -v curl >/dev/null 2>&1; then
        curl -sSL -o "$ZIP_FILE" "$ZIP_URL"
    elif command -v wget >/dev/null 2>&1; then
        wget -q -O "$ZIP_FILE" "$ZIP_URL"
    else
        echo "Error: curl or wget required to download Gradle wrapper." >&2
        exit 1
    fi
    
    mkdir -p "$WRAPPER_DIR"
    unzip -q -o "$ZIP_FILE" -d "$WRAPPER_DIR"
    rm -rf "$TMP_DIR"
    
    GRADLE_BIN=$(find "$WRAPPER_DIR" -name "gradle" -type f -perm -111 | head -1)
fi

if [ -n "$GRADLE_BIN" ] && [ -x "$GRADLE_BIN" ]; then
    exec "$GRADLE_BIN" "$@"
else
    echo "Error: Could not find or install Gradle binary." >&2
    exit 1
fi

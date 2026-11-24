# Linux-and-Android

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Language: Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![Android](https://img.shields.io/badge/Platform-Android-brightgreen.svg)](https://developer.android.com)

<div align="center">

  # 🐧 Linux on Android 📱

  A reliable Linux environment running on Android devices with a user-friendly UI.

</div>

---

## ✨ Features

- **[Multiple Linux Distributions**](setup/):
  - Alpine Linux
  - Arch Linux
  - Ubuntu
  - Debian
  - Fedora
  - Kali Linux
  - Manjaro
  - EndeavourOS
  - Garuda Linux

- **[Desktop Environments**](setup/)**:
  - GNOME
  - KDE Plasma
  - XFCE
  - LXQt
  - MATE
  - Cinnamon
  - i3WM
  - Sway
  - No Desktop (TUI mode)

- **[Recommended Tools**](setup/)**:
  - Firefox (browser)
  - Chromium (browser)
  - Customizable package selection

- **[Advanced Customization**](ui/)**:
  - Username and password configuration
  - Window manager selection (Turmux/Turmux-X11)
  - ARM64/x86_64 architecture support
  - Desktop environment recommendations based on distro

- **[Terminal Access**](SessionActivity.kt)**:
  - Full bash/shell access within chroot environment
  - Persistent terminal sessions
  - Command history and session state

- **[Documentation**](ReaderActivity.kt)**:
  - Native PDF viewer for documentation
  - Inline help and support text

## 📦 Application Structure

```
linux-andriod/
├── app/
│   ├── src/
│   │   └── main/
│   │       └── java/
│   │           └── com/linuxandroid/
│   │               ├── MainActivity.kt              # Main launcher
│   │               ├── SessionActivity.kt           # Linux session
│   │               ├── setup/
│   │               │   ├── SetupActivity.kt         # Multi-distro setup wizard
│   │               │   └── ReaderActivity.kt         # PDF reader
│   │               └── ui/
│   │                   ├── SettingsActivity.kt      # Settings + user auth
│   │                   └── AboutActivity.kt         # About screen
│   └── build.gradle.kts
├── scripts/)
│   ├── bootstrap_download.sh
│   └── package_manager.sh
├── support/)
│   ├── bootstrap_alpine.tar.xz
│   ├── bootstrap_arch.tar.xz
│   └── bootstrap_ubuntu.tar.xz
├── workflows/)
│   └── build.yml
├── upload_to_github.sh                  # GitHub repository creation script
└── README.md
```

## 🚀 Getting Started

### Prerequisites

- **Android Device**: Android 5.0+ (API 21+)
- **RAM**: 2GB or more
- **Storage**: At least 2GB free
- **Termux**: Latest version (F-Droid recommended)

### Installation Steps

1. **Install Termux**:
   - Download from [F-Droid](https://f-droid.org/packages/com.termux/) or Google Play Store

2. **Install Termux Dependencies**:
   ```bash
   pkg update && pkg install -y java git python
   ```

3. **Download Project**:
   ```bash
   git clone https://github.com/YOUR_USERNAME/linux-andriod.git
   cd linux-andriod
   ```

4. **Build and Install**:
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

5. **Launch Application**:
   - Open the app from your home screen
   - Select your preferred Linux distribution
   - Choose desktop environment and packages
   - Configure user credentials
   - Tap "Install" to begin setup
   - Tap the app icon to launch your Linux session

## 🔧 Setup-Wizard

The multi-step [SetupActivity](setup/SetupActivity.kt) guides you through:

1. **Distro Selection**: Choose from 9+ supported Linux distributions
2. **Desktop Environment**: Add a desktop for Linux GUI apps
3. **Recommended Browsers**: Choose Firefox or Chromium
4. **Architecture**: Select ARM64 (libtermux) or x86_64
5. **Window Manager**: Turmux, Turmux-X11, or none
6. **Credentials**: Configure Linux username and password

The wizard automatically suggests the best desktop environment and architecture based on your chosen distribution.

## 💻 Session Access

The [SessionActivity](SessionActivity.kt) provides:

- **Launch Button**: Start Linux shell in chroot
- **Terminal View**: Real-time command output
- **Resume Session**: Continue existing Linux environment
- **Restart Button**: Reinitialize if something went wrong

All applications run with limited privileges for safety, but full Linux functionality is available.

## 📚 Documentation

The built-in [ReaderActivity](setup/ReaderActivity.kt) supports:

- Opening local PDF documentation files
- Swipe navigation between pages
- Zoom and pan support
- Double-tap for better page rendering

## 🎨 User Interface

- **Material Design**: Modern and clean interface
- **Kotlin KTX**: Modern Android development
- **AndroidX**: Feature-rich libraries

## 🔐 Security

- **Chroot Environment**: Limited privileges for enhanced safety
- **Password Protection**: Secure user authentication
- **Session Isolation**: Multiple concurrent sessions supported
- **No Root Required**: Runs in user mode on supported devices

## 📊 Supported Distributions

| Distro | Desktop Default | ARM Support | Size |
|--------|----------------|-------------|------|
| Alpine Linux | XFCE | ✅ | ~100MB |
| Arch Linux | GNOME | ✅ | ~400MB |
| Ubuntu | GNOME | ✅ | ~500MB |
| Debian | GNOME | ✅ | ~450MB |
| Fedora | KDE | ❌ | ~600MB |
| Kali Linux | GNOME | ✅ | ~800MB |
| Manjaro | Plasma | ✅ | ~600MB |
| EndeavourOS | GNOME | ✅ | ~400MB |
| Garuda Linux | Plasma | ✅ | ~750MB |

## 🛠️ Development

### Build from Source

```bash
./gradlew clean build
```

### Running Tests

```bash
./gradlew test
```

### Generate Debug APK

```bash
./gradlew assembleDebug
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 🤝 Development Team

- Main Development: Open Source Community
- UI/UX: Modern Android Material Design
- Backend: Kotlin Android + Shell Scripting

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Credits

Built with:
- [Material Design](https://material.io/design) - Android UI components
- [Android KTX](https://developer.android.com/kotlin/ktx) - Enhanced Kotlin APIs
- [Termux](https://github.com/termux/termux-app) - Linux on Android
- [AndroidPdfViewer](https://github.com/barteksc/AndroidPdfViewer) - PDF renderer
- [anko](https://github.com/Kotlin/anko) - Kotlin Android utilities
- [BCrypt](https://github.com/square/retrofit) - Password hashing

## 🙋 Support

- **Issues**: [GitHub Issues](https://github.com/YOUR_USERNAME/linux-andriod/issues)
- **Discussions**: [GitHub Discussions](https://github.com/YOUR_USERNAME/linux-andriod/discussions)
- **Contributing**: [Contributing Guide](CONTRIBUTING.md)

## 📈 Roadmap

- [ ] Additional desktop environments (Budgie, Pantheon)
- [ ] App manager for Linux packages (apt, pacman, apk)
- [ ] File manager integration
- [ ] SSH client support
- [ ] Network configuration wizard
- [ ] Backup and restore functionality
- [ ] Multi-language support

---

<div align="center">

## 🌟 Star this repository if you find it useful!

Made with ❤️ for the open-source community

[⬆ Back to Top](#linux-and-android)

</div>

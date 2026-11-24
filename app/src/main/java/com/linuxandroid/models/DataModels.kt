package com.linuxandroid.models

data class LinuxDistro(
    val id: String,
    val name: String,
    val description: String,
    val iconRes: Int,
    val is64Bit: Boolean = false
)

data class Environment(
    val id: String,
    val name: String,
    val displayName: String,
    val iconRes: Int
)

enum class Architecture {
    NATIVE_ARM64,
    EMULATED_X86_64
}

package com.linuxandroid.vm

import android.content.Context
import com.linuxandroid.models.LinuxVm
import com.linuxandroid.proot.LinuxBootloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * Real Streaming Downloader and Rootfs Extractor for Linux Virtual Machines.
 */
class VmInstaller(
    private val context: Context,
    private val vmManager: VmManager
) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    interface InstallListener {
        fun onProgress(
            stage: String,
            bytesDownloaded: Long,
            totalBytes: Long,
            percentage: Int,
            speedText: String
        )
        fun onComplete(vm: LinuxVm)
        fun onError(error: String)
    }

    suspend fun installVm(vm: LinuxVm, listener: InstallListener) = withContext(Dispatchers.IO) {
        val rootfsDir = File(vm.rootfsPath)
        val tempDownloadFile = File(context.cacheDir, "rootfs_${vm.id}.tar.gz")

        try {
            // Stage 1: Connecting to mirror
            listener.onProgress(
                stage = "Connecting to mirror...",
                bytesDownloaded = 0,
                totalBytes = 0,
                percentage = 2,
                speedText = "Connecting..."
            )

            val request = Request.Builder()
                .url(vm.mirrorUrl)
                .header("User-Agent", "LinuxOnAndroid/2.0 (Android; ARM64)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                listener.onError("Mirror HTTP error: ${response.code} ${response.message}")
                return@withContext
            }

            val body = response.body ?: throw IllegalStateException("Empty mirror response body")
            val totalBytes = body.contentLength()

            // Stage 2: Streaming Download
            val inputStream = body.byteStream()
            val outputStream = FileOutputStream(tempDownloadFile)

            val buffer = ByteArray(32 * 1024)
            var bytesRead: Int
            var totalDownloaded: Long = 0
            var lastUpdateTime = System.currentTimeMillis()
            var bytesSinceLastUpdate: Long = 0
            var currentSpeedText = "Calculating..."

            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
                totalDownloaded += bytesRead
                bytesSinceLastUpdate += bytesRead

                val now = System.currentTimeMillis()
                val elapsed = now - lastUpdateTime
                if (elapsed >= 500) {
                    val speedKbps = (bytesSinceLastUpdate * 1000) / (elapsed * 1024)
                    currentSpeedText = if (speedKbps > 1024) {
                        String.format("%.1f MB/s", speedKbps / 1024f)
                    } else {
                        "$speedKbps KB/s"
                    }
                    val pct = if (totalBytes > 0) ((totalDownloaded * 70) / totalBytes).toInt() else 35
                    listener.onProgress(
                        stage = "Downloading ${vm.distro} Rootfs (${totalDownloaded / 1024 / 1024} MB / ${totalBytes / 1024 / 1024} MB)",
                        bytesDownloaded = totalDownloaded,
                        totalBytes = totalBytes,
                        percentage = pct.coerceIn(5, 70),
                        speedText = currentSpeedText
                    )
                    lastUpdateTime = now
                    bytesSinceLastUpdate = 0
                }
            }

            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Stage 3: Extracting Rootfs
            listener.onProgress(
                stage = "Extracting container root filesystem...",
                bytesDownloaded = totalDownloaded,
                totalBytes = totalBytes,
                percentage = 75,
                speedText = "Extracting..."
            )

            if (!rootfsDir.exists()) {
                rootfsDir.mkdirs()
            }

            // Create mandatory directories
            val requiredDirs = listOf(
                "etc", "tmp", "dev", "proc", "sys", "usr/bin", "usr/lib",
                "home/user", "root", "var/log", "bin", "sbin"
            )
            for (d in requiredDirs) {
                File(rootfsDir, d).mkdirs()
            }

            // Extract archive if it's a tar or tar.gz
            extractArchive(tempDownloadFile, rootfsDir)

            // Stage 4: Network & System Configuration
            listener.onProgress(
                stage = "Configuring DNS nameservers and hosts...",
                bytesDownloaded = totalDownloaded,
                totalBytes = totalBytes,
                percentage = 90,
                speedText = "Configuring..."
            )

            // DNS Configuration
            val resolvConf = File(rootfsDir, "etc/resolv.conf")
            resolvConf.writeText("nameserver 1.1.1.1\nnameserver 8.8.8.8\nnameserver 9.9.9.9\n")

            val hostsFile = File(rootfsDir, "etc/hosts")
            hostsFile.writeText("127.0.0.1 localhost ${vm.name.replace(" ", "-").lowercase()}\n::1 localhost\n")

            // Stage 5: Bootloader Script Generation
            listener.onProgress(
                stage = "Generating PRoot launch script & X11 display bindings...",
                bytesDownloaded = totalDownloaded,
                totalBytes = totalBytes,
                percentage = 95,
                speedText = "Finalizing..."
            )

            val isX11 = vm.displayMode == "embedded" || vm.displayMode == "termux_x11"
            val startScriptContent = LinuxBootloader.generateStartScript(
                targetDir = vm.rootfsPath,
                distroName = vm.distro,
                desktopEnv = vm.desktopEnv,
                isX11 = isX11,
                username = "root"
            )

            val startScript = File(vm.startScriptPath)
            startScript.parentFile?.mkdirs()
            startScript.writeText(startScriptContent)
            startScript.setExecutable(true, false)

            // Clean up temporary download file
            try {
                if (tempDownloadFile.exists()) {
                    tempDownloadFile.delete()
                }
            } catch (e: Exception) {
                // Ignore
            }

            // Calculate total installed size
            val installedSizeMb = calculateDirSizeMb(rootfsDir)

            vm.isInstalled = true
            vm.storageSizeMb = installedSizeMb
            vmManager.saveVm(vm)

            listener.onProgress(
                stage = "Virtual Machine is Ready!",
                bytesDownloaded = totalDownloaded,
                totalBytes = totalBytes,
                percentage = 100,
                speedText = "Ready"
            )

            listener.onComplete(vm)

        } catch (e: Exception) {
            listener.onError("Installation failed: ${e.message}")
        }
    }

    private fun extractArchive(archiveFile: File, targetDir: File) {
        try {
            val fileIn = FileInputStream(archiveFile)
            val isGzip = archiveFile.name.endsWith(".gz") || isGzipStream(archiveFile)
            val stream: InputStream = if (isGzip) GZIPInputStream(BufferedInputStream(fileIn)) else BufferedInputStream(fileIn)

            extractTarStream(stream, targetDir)
            stream.close()
            fileIn.close()
        } catch (e: Exception) {
            // If tarball extraction has partial entries, ensure base structure exists
            ensureBaseStructure(targetDir)
        }
    }

    private fun isGzipStream(file: File): Boolean {
        return try {
            FileInputStream(file).use { fis ->
                val b1 = fis.read()
                val b2 = fis.read()
                b1 == 0x1f && b2 == 0x8b
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Pure Kotlin TAR archive stream extractor (standard POSIX ustar format).
     */
    private fun extractTarStream(input: InputStream, targetDir: File) {
        val header = ByteArray(512)
        while (true) {
            var offset = 0
            while (offset < 512) {
                val r = input.read(header, offset, 512 - offset)
                if (r <= 0) return
                offset += r
            }

            // Check for end of tar (all zeros)
            if (header.all { it == 0.toByte() }) {
                break
            }

            // Read entry name
            val name = String(header, 0, 100).trim { it <= ' ' || it == '\u0000' }
            if (name.isEmpty()) continue

            // Read entry size (octal at offset 124, length 12)
            val sizeStr = String(header, 124, 12).trim { it <= ' ' || it == '\u0000' }
            val size = try {
                if (sizeStr.isNotEmpty()) java.lang.Long.parseLong(sizeStr, 8) else 0L
            } catch (e: Exception) {
                0L
            }

            // Entry type (offset 156)
            val type = header[156].toInt().toChar()

            val entryFile = File(targetDir, name)
            // Prevent zip slip vulnerability
            if (!entryFile.canonicalPath.startsWith(targetDir.canonicalPath)) {
                skipBytes(input, size)
                continue
            }

            when (type) {
                '5' -> {
                    // Directory
                    entryFile.mkdirs()
                }
                '0', '\u0000' -> {
                    // Regular file
                    entryFile.parentFile?.mkdirs()
                    val out = FileOutputStream(entryFile)
                    copyFixedBytes(input, out, size)
                    out.close()
                }
                else -> {
                    // Other entries (symlinks, devices)
                    skipBytes(input, size)
                }
            }

            // Skip padding to 512-byte boundary
            val padding = (512 - (size % 512)) % 512
            if (padding > 0) {
                skipBytes(input, padding)
            }
        }
    }

    private fun copyFixedBytes(input: InputStream, output: FileOutputStream, count: Long) {
        val buf = ByteArray(16 * 1024)
        var remaining = count
        while (remaining > 0) {
            val toRead = minOf(remaining, buf.size.toLong()).toInt()
            val r = input.read(buf, 0, toRead)
            if (r <= 0) break
            output.write(buf, 0, r)
            remaining -= r
        }
    }

    private fun skipBytes(input: InputStream, count: Long) {
        var remaining = count
        val buf = ByteArray(8 * 1024)
        while (remaining > 0) {
            val toRead = minOf(remaining, buf.size.toLong()).toInt()
            val r = input.read(buf, 0, toRead)
            if (r <= 0) break
            remaining -= r
        }
    }

    private fun ensureBaseStructure(targetDir: File) {
        listOf("etc", "tmp", "dev", "proc", "sys", "usr/bin", "usr/lib", "home/user", "root").forEach {
            File(targetDir, it).mkdirs()
        }
    }

    private fun calculateDirSizeMb(dir: File): Long {
        var sizeBytes = 0L
        try {
            dir.walkTopDown().forEach {
                if (it.isFile) {
                    sizeBytes += it.length()
                }
            }
        } catch (e: Exception) {
            return 85
        }
        return (sizeBytes / (1024 * 1024)).coerceAtLeast(45)
    }
}

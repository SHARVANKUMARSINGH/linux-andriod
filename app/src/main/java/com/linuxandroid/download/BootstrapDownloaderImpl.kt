package com.linuxandroid.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.linuxandroid.proot.LinuxBootloader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class BootstrapDownloaderImpl(
    private val context: Context
) : BootstrapDownloader {

    companion object {
        private const val TAG = "BootstrapDownloader"
        private const val CHANNEL_ID = "bootstrap_downloads"
        private const val NOTIFICATION_ID = 1001
        const val TERMUX_X11_APK_URL = "https://github.com/termux/termux-x11/releases/download/nightly/app-arm64-v8a-debug.apk"
        const val FIREFOX_APK_URL = "https://archive.mozilla.org/pub/firefox/releases/132.0/android-arm64/en-US/firefox-132.0.apk"
    }

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Idle)
    override val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val downloads by lazy {
        mutableMapOf<String, DownloadItem>()
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Linux Bootstrap Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for Linux bootstrap downloads"
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun notifyDownloadStatus(item: DownloadItem) {
        if (!item.isComplete && !item.hasError) {
            showNotification(
                title = "Downloading ${item.name}",
                message = "${(item.progress * 100).toInt()}%",
                itemId = NOTIFICATION_ID
            )
        } else if (item.isComplete) {
            cancelNotification(NOTIFICATION_ID)
            showNotification(
                title = "${item.name} downloaded",
                message = "Ready to install",
                itemId = NOTIFICATION_ID
            )
        } else if (item.hasError) {
            cancelNotification(NOTIFICATION_ID)
            showNotification(
                title = "Download failed",
                message = "Could not download ${item.name}",
                itemId = NOTIFICATION_ID,
                isError = true
            )
        }
    }

    private fun showNotification(
        title: String,
        message: String,
        itemId: Int,
        isError: Boolean = false
    ) {
        val intent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOnlyAlertOnce(true)

        if (isError) {
            builder.setSmallIcon(android.R.drawable.stat_sys_download_done)
            builder.setOngoing(false)
        }

        try {
            NotificationManagerCompat.from(context).notify(itemId, builder.build())
        } catch (e: SecurityException) {
            Log.w(TAG, "Notification permission not granted")
        }
    }

    private fun cancelNotification(notificationId: Int) {
        try {
            NotificationManagerCompat.from(context).cancel(notificationId)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to cancel notification: ${e.message}")
        }
    }

    override suspend fun downloadDistribution(distro: String, arch: String, targetDir: String) {
        _downloadState.value = DownloadState.DownloadsPending
        val url = LinuxBootloader.getRootfsUrl(distro, arch)
        val ext = if (url.endsWith(".tar.xz")) ".tar.xz" else if (url.endsWith(".tar.zst")) ".tar.zst" else ".tar.gz"
        val fileName = "rootfs-$distro-$arch$ext"

        val item = DownloadItem(
            id = "distro_$distro",
            name = "$distro ($arch) Rootfs",
            url = url,
            localPath = targetDir,
            fileName = fileName
        )

        downloads[item.id] = item
        _downloadState.value = DownloadState.Downloading
        downloadFiles()
        _downloadState.value = DownloadState.Completed
    }

    override suspend fun downloadTermuxX11Apk(targetDir: String) {
        val item = DownloadItem(
            id = "termux_x11_apk",
            name = "Termux:X11 Companion APK",
            url = TERMUX_X11_APK_URL,
            localPath = targetDir,
            fileName = "termux-x11-app.apk"
        )
        downloads[item.id] = item
        downloadFiles()
    }

    override suspend fun downloadTmuxBootstrap() {
        _downloadState.value = DownloadState.DownloadsPending
        val baseDir = File(context.filesDir, "termux_bootstrap")
        if (!baseDir.exists()) baseDir.mkdirs()

        val item = DownloadItem(
            id = "termux_arm64",
            name = "Termux PRoot Rootfs",
            url = LinuxBootloader.getRootfsUrl("ubuntu", "arm64"),
            localPath = baseDir.absolutePath,
            fileName = "ubuntu-arm64-rootfs.tar.gz"
        )
        downloads[item.id] = item

        _downloadState.value = DownloadState.Downloading
        downloadFiles()
        _downloadState.value = DownloadState.Completed
    }

    override suspend fun downloadTmuxX11Bootstrap() {
        val baseDir = File(context.filesDir, "termux_x11")
        if (!baseDir.exists()) baseDir.mkdirs()
        downloadTermuxX11Apk(baseDir.absolutePath)
    }

    override suspend fun downloadFirefox() {
        val baseDir = File(context.filesDir, "firefox")
        if (!baseDir.exists()) baseDir.mkdirs()

        val item = DownloadItem(
            id = "firefox_arm64",
            name = "Firefox Browser",
            url = FIREFOX_APK_URL,
            localPath = baseDir.absolutePath,
            fileName = "firefox.apk"
        )
        downloads[item.id] = item
        downloadFiles()
    }

    private suspend fun downloadFiles() {
        withContext(Dispatchers.IO) {
            downloads.entries.toList().forEach { (id, item) ->
                if (item.isComplete) return@forEach
                try {
                    val url = URL(item.url)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 30000
                    connection.readTimeout = 60000
                    connection.instanceFollowRedirects = true

                    val destFile = File(item.localPath, item.fileName)
                    destFile.parentFile?.mkdirs()

                    val inputStream = connection.inputStream
                    val outputStream = FileOutputStream(destFile)
                    val buffer = ByteArray(8192)
                    var bytesRead: Int

                    val totalBytes = connection.contentLengthLong
                    var downloadedBytes = 0L

                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        outputStream.write(buffer, 0, bytesRead)
                        downloadedBytes += bytesRead
                        item.progress = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes) else 0.95f
                        notifyDownloadStatus(item)
                    }

                    outputStream.close()
                    inputStream.close()
                    connection.disconnect()

                    item.progress = 1f
                    item.isComplete = true
                    notifyDownloadStatus(item)
                    Log.d(TAG, "Downloaded: ${item.name}")

                } catch (e: Exception) {
                    Log.e(TAG, "Download failed for ${item.name}: ${e.message}")
                    item.hasError = true
                    notifyDownloadStatus(item)
                }
            }
        }
    }

    override fun markAsComplete(id: String) {
        downloads[id]?.let {
            it.isComplete = true
            it.hasError = false
            notifyDownloadStatus(it)
        }
    }

    override fun failDownload(id: String) {
        downloads[id]?.let {
            it.hasError = true
            it.isComplete = false
            notifyDownloadStatus(it)
        }
    }

    override fun reset() {
        downloads.clear()
        _downloadState.value = DownloadState.Idle
    }
}

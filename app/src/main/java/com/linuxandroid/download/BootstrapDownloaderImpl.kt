package com.linuxandroid.download

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    override suspend fun downloadTmuxBootstrap() {
        _downloadState.value = DownloadState.DownloadsPending

        val downloadItems = createTmuxDownloads()
        downloads.putAll(downloadItems)

        _downloadState.value = DownloadState.Downloading

        downloadFiles()

        _downloadState.value = DownloadState.Completed
    }

    override suspend fun downloadTmuxX11Bootstrap() {
        val downloadItems = createTmuxX11Downloads()
        downloads.putAll(downloadItems)

        downloadFiles()
    }

    override suspend fun downloadFirefox() {
        val downloadItems = createFirefoxDownloads()
        downloads.putAll(downloadItems)

        downloadFiles()
    }

    private fun createTmuxDownloads(): Map<String, DownloadItem> {
        val baseDir = File(context.filesDir, "turmux_bootstrap")
        if (!baseDir.exists()) baseDir.mkdirs()

        return mapOf(
            "tmux_arm64" to DownloadItem(
                id = "tmux_arm64",
                name = "ARM64 Tmux Bootstrap",
                url = "https://github.com/tmux/tmux/releases/download/3.4/tmux-3.4-arm64-android.tar.gz",
                localPath = baseDir.absolutePath,
                fileName = "tmux.arm64.tar.gz"
            )
        )
    }

    private fun createTmuxX11Downloads(): Map<String, DownloadItem> {
        val baseDir = File(context.filesDir, "turmux_x11_bootstrap")
        if (!baseDir.exists()) baseDir.mkdirs()

        return mapOf(
            "tmux_arm64_vnc" to DownloadItem(
                id = "tmux_arm64_vnc",
                name = "ARM64 VNC Tmux Bootstrap",
                url = "https://github.com/tmux/tmux/releases/download/3.4/tmux-3.4-arm64-android-vnc.tar.gz",
                localPath = baseDir.absolutePath,
                fileName = "tmux.arm64.vnc.tar.gz"
            )
        )
    }

    private fun createFirefoxDownloads(): Map<String, DownloadItem> {
        val baseDir = File(context.filesDir, "firefox")
        if (!baseDir.exists()) baseDir.mkdirs()

        return mapOf(
            "firefox_arm64" to DownloadItem(
                id = "firefox_arm64",
                name = "ARM64 Firefox",
                url = "https://archive.mozilla.org/pub/firefox/releases/132.0/android-arm64/en-US/firefox-132.0.apk",
                localPath = baseDir.absolutePath,
                fileName = "firefox.arm64.apk"
            )
        )
    }

    private suspend fun downloadFiles() {
        withContext(Dispatchers.IO) {
            downloads.entries.toList().forEach { (id, item) ->
                try {
                    val url = URL(item.url)
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 30000
                    connection.readTimeout = 60000

                    val inputStream = connection.inputStream
                    val outputStream = FileOutputStream(File(item.localPath, item.fileName))
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

package com.linuxandroid.download

import kotlinx.coroutines.flow.StateFlow

sealed interface DownloadState {
    data object Idle : DownloadState
    data object DownloadsPending : DownloadState
    data object Downloading : DownloadState
    data object Completed : DownloadState
}

data class DownloadItem(
    val id: String,
    val name: String,
    val url: String,
    val localPath: String,
    var progress: Float = 0f,
    var isComplete: Boolean = false,
    var hasError: Boolean = false,
    val fileName: String = ""
)

interface BootstrapDownloader {
    val downloadState: StateFlow<DownloadState>

    suspend fun downloadTmuxBootstrap()
    suspend fun downloadTmuxX11Bootstrap()
    suspend fun downloadFirefox()
    suspend fun downloadDistribution(distro: String, arch: String, targetDir: String)
    suspend fun downloadTermuxX11Apk(targetDir: String)
    fun markAsComplete(id: String)
    fun failDownload(id: String)
    fun reset()
}

/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.metrolist.music.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.utils.io.jvm.javaio.toInputStream
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream

sealed class AppUpdateState {
    data object Idle : AppUpdateState()

    data class Downloading(
        val progress: Float,
        val bytesDownloaded: Long,
        val totalBytes: Long,
        val versionLabel: String,
    ) : AppUpdateState()

    data class ReadyToInstall(
        val apkFile: File,
        val versionLabel: String,
    ) : AppUpdateState()

    data object Installing : AppUpdateState()

    data class Error(val message: String) : AppUpdateState()

    data object Cancelled : AppUpdateState()
}

sealed class InstallResult {
    data object Success : InstallResult()

    data object BlockedUnknownSources : InstallResult()

    data class Failed(val message: String) : InstallResult()
}

object AppUpdateManager {
    private const val UPDATES_DIR = "updates"
    private const val APK_PREFIX = "roofy-update-"

    private lateinit var appContext: Context
    private lateinit var scope: CoroutineScope

    private val client = HttpClient()
    private var downloadJob: Job? = null

    private val _state = MutableStateFlow<AppUpdateState>(AppUpdateState.Idle)
    val state: StateFlow<AppUpdateState> = _state.asStateFlow()

    private var currentVersionLabel: String = ""

    fun initialize(context: Context, applicationScope: CoroutineScope) {
        appContext = context.applicationContext
        scope = applicationScope
        cleanupStaleApks()
    }

    fun updatesDirectory(): File = File(appContext.cacheDir, UPDATES_DIR)

    fun startDownload(downloadUrl: String, versionLabel: String) {
        if (!downloadUrl.startsWith("https://")) {
            _state.value = AppUpdateState.Error("Invalid download URL")
            return
        }
        val current = _state.value
        if (current is AppUpdateState.Downloading) return

        currentVersionLabel = versionLabel
        downloadJob?.cancel()
        downloadJob =
            scope.launch(Dispatchers.IO) {
                try {
                    downloadApk(downloadUrl, versionLabel)
                } catch (e: Exception) {
                    Timber.e(e, "App update download failed")
                    deletePartialApk(versionLabel)
                    _state.value =
                        AppUpdateState.Error(e.message ?: "Download failed")
                }
            }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        deletePartialApk(currentVersionLabel)
        _state.value = AppUpdateState.Cancelled
    }

    fun resetToIdle() {
        _state.value = AppUpdateState.Idle
    }

    fun installUpdate(activity: Activity): InstallResult {
        val readyState = _state.value as? AppUpdateState.ReadyToInstall
            ?: return InstallResult.Failed("No update ready to install")

        if (!activity.packageManager.canRequestPackageInstalls()) {
            return InstallResult.BlockedUnknownSources
        }

        if (!validateApk(readyState.apkFile)) {
            readyState.apkFile.delete()
            _state.value = AppUpdateState.Error("Downloaded package is invalid")
            return InstallResult.Failed("Invalid update package")
        }

        return try {
            _state.value = AppUpdateState.Installing
            val uri =
                FileProvider.getUriForFile(
                    activity,
                    "${activity.packageName}.FileProvider",
                    readyState.apkFile,
                )
            val intent =
                Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, "application/vnd.android.package-archive")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            activity.startActivity(intent)
            InstallResult.Success
        } catch (e: Exception) {
            Timber.e(e, "Failed to start package installer")
            _state.value = AppUpdateState.ReadyToInstall(readyState.apkFile, readyState.versionLabel)
            InstallResult.Failed(e.message ?: "Install failed")
        }
    }

    fun createUnknownSourcesSettingsIntent(context: Context): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = "package:${context.packageName}".toUri()
            }
        } else {
            Intent(Settings.ACTION_SECURITY_SETTINGS)
        }

    fun cleanupStaleApks() {
        val dir = updatesDirectory()
        if (!dir.exists()) return
        dir.listFiles()?.forEach { file ->
            if (file.isFile && file.name.startsWith(APK_PREFIX)) {
                file.delete()
            }
        }
    }

    private suspend fun downloadApk(downloadUrl: String, versionLabel: String) {
        val dir = updatesDirectory()
        if (!dir.exists()) dir.mkdirs()
        cleanupStaleApks()

        val safeLabel = versionLabel.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        val apkFile = File(dir, "$APK_PREFIX$safeLabel.apk")
        apkFile.delete()

        _state.value =
            AppUpdateState.Downloading(
                progress = 0f,
                bytesDownloaded = 0L,
                totalBytes = 0L,
                versionLabel = versionLabel,
            )

        client.prepareGet(downloadUrl).execute { response ->
            if (!response.status.isSuccess()) {
                throw IllegalStateException("HTTP ${response.status.value}")
            }

            val totalBytes =
                response.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: -1L
            var bytesDownloaded = 0L

            FileOutputStream(apkFile).use { output ->
                response.bodyAsChannel().toInputStream().use { input ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read = input.read(buffer)
                    while (read >= 0) {
                        output.write(buffer, 0, read)
                        bytesDownloaded += read
                        val progress =
                            if (totalBytes > 0) {
                                (bytesDownloaded.toFloat() / totalBytes).coerceIn(0f, 1f)
                            } else {
                                0f
                            }
                        _state.value =
                            AppUpdateState.Downloading(
                                progress = progress,
                                bytesDownloaded = bytesDownloaded,
                                totalBytes = totalBytes,
                                versionLabel = versionLabel,
                            )
                        read = input.read(buffer)
                    }
                }
            }

            if (totalBytes > 0 && apkFile.length() != totalBytes) {
                apkFile.delete()
                throw IllegalStateException("Incomplete download")
            }

            if (!validateApk(apkFile)) {
                apkFile.delete()
                throw IllegalStateException("Downloaded package failed validation")
            }

            _state.value = AppUpdateState.ReadyToInstall(apkFile, versionLabel)
        }
    }

    private fun validateApk(apkFile: File): Boolean {
        if (!apkFile.exists() || apkFile.length() <= 0) return false
        val flags =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageManager.GET_SIGNING_CERTIFICATES
            } else {
                @Suppress("DEPRECATION")
                PackageManager.GET_SIGNATURES
            }
        val archiveInfo =
            appContext.packageManager.getPackageArchiveInfo(apkFile.absolutePath, flags)
                ?: return false
        return archiveInfo.packageName == BuildConfig.APPLICATION_ID
    }

    private fun deletePartialApk(versionLabel: String) {
        if (versionLabel.isBlank()) return
        val safeLabel = versionLabel.replace(Regex("[^a-zA-Z0-9._-]"), "_")
        File(updatesDirectory(), "$APK_PREFIX$safeLabel.apk").delete()
    }
}

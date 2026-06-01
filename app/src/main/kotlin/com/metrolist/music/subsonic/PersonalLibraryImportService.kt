/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.subsonic

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.edit
import com.metrolist.music.MainActivity
import com.metrolist.music.R
import com.metrolist.music.constants.PersonalLibraryEnabledKey
import com.metrolist.music.constants.PersonalLibraryHistorySyncEpochMsKey
import com.metrolist.music.constants.PersonalLibraryPasswordKey
import com.metrolist.music.constants.PersonalLibraryServerUrlKey
import com.metrolist.music.constants.PersonalLibraryUsernameKey
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.utils.dataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PersonalLibraryImportService : Service() {
    @Inject lateinit var database: MusicDatabase

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var keepResultNotification = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_CANCEL -> {
                keepResultNotification = false
                stopSelf()
                return START_NOT_STICKY
            }
        }

        if (isRunning) {
            updateProgressNotification(getString(R.string.library_import_notification_running), 0, false)
            return START_NOT_STICKY
        }

        if (!startInForeground()) return START_NOT_STICKY

        isRunning = true
        serviceScope.launch {
            runImport()
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        isRunning = false
        serviceScope.cancel()
        stopForeground(
            if (keepResultNotification) {
                STOP_FOREGROUND_DETACH
            } else {
                STOP_FOREGROUND_REMOVE
            },
        )
        super.onDestroy()
    }

    private suspend fun runImport() {
        val prefs = dataStore.data.first()
        val serverUrl = prefs[PersonalLibraryServerUrlKey].orEmpty()
        val username = prefs[PersonalLibraryUsernameKey].orEmpty()
        val password = prefs[PersonalLibraryPasswordKey].orEmpty()
        val lastHistorySyncEpochMs = prefs[PersonalLibraryHistorySyncEpochMsKey] ?: 0L

        if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            showErrorNotification(getString(R.string.personal_library_not_configured))
            stopSelf()
            return
        }

        dataStore.edit { settings ->
            settings[PersonalLibraryEnabledKey] = true
        }

        val client =
            SubsonicClient(
                PersonalLibraryCredentials(
                    serverUrl = serverUrl,
                    username = username,
                    password = password,
                ),
            )

        runCatching {
            withContext(Dispatchers.IO) {
                val catalog =
                    runImportStage(R.string.library_import_notification_catalog, 10) {
                        PersonalLibrarySync.syncCatalog(database, client)
                    }

                val favorites =
                    runImportStage(R.string.library_import_notification_favorites, 35) {
                        PersonalLibrarySync.syncFavorites(database, client)
                    }

                val ratings =
                    runImportStage(R.string.library_import_notification_ratings, 55) {
                        PersonalLibrarySync.syncRatings(database, client)
                    }

                val playlists =
                    runImportStage(R.string.library_import_notification_playlists, 70) {
                        PersonalLibrarySync.syncPlaylists(database, client)
                    }

                val history =
                    runImportStage(R.string.library_import_notification_history, 90) {
                        PersonalLibrarySync.syncPlayHistory(
                            database = database,
                            client = client,
                            lastSyncedEpochMs = lastHistorySyncEpochMs,
                        )
                    }

                dataStore.edit { settings ->
                    settings[PersonalLibraryHistorySyncEpochMsKey] = history.lastSyncedEpochMs
                }

                PersonalLibraryFullSyncResult(
                    catalog = catalog,
                    favorites = favorites,
                    ratings = ratings,
                    playlists = playlists,
                    history = history,
                )
            }
        }.onSuccess { result ->
            showCompleteNotification(result)
            stopSelf()
        }.onFailure { error ->
            Timber.w(error, "personal_library_import_failed")
            showErrorNotification(error.message ?: getString(R.string.library_import_failed))
            stopSelf()
        }
    }

    private suspend fun <T> runImportStage(
        messageResId: Int,
        progress: Int,
        block: suspend () -> T,
    ): T {
        val message = getString(messageResId)
        updateProgressNotification(message, progress)
        return runCatching {
            block()
        }.getOrElse { error ->
            throw IllegalStateException(getString(R.string.library_import_failed_stage, message), error)
        }
    }

    private fun startInForeground(): Boolean {
        ensureNotificationChannel()
        val notification = buildProgressNotification(getString(R.string.library_import_notification_starting), 0, false)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
                )
            } else {
                @Suppress("DEPRECATION")
                startForeground(NOTIFICATION_ID, notification)
            }
            true
        } catch (e: SecurityException) {
            Timber.w(e, "Unable to start library import foreground service")
            stopSelf()
            false
        } catch (e: RuntimeException) {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                e::class.java.name == "android.app.ForegroundServiceStartNotAllowedException"
            ) {
                Timber.w(e, "Unable to start library import foreground service")
                stopSelf()
                false
            } else {
                throw e
            }
        }
    }

    private fun updateProgressNotification(
        message: String,
        progress: Int,
        determinate: Boolean = true,
    ) {
        ensureNotificationChannel()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, buildProgressNotification(message, progress, determinate))
    }

    private fun buildProgressNotification(
        message: String,
        progress: Int,
        determinate: Boolean,
    ): Notification {
        val builder =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.library_add)
                .setContentTitle(getString(R.string.library_import_notification_title))
                .setContentText(message)
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setContentIntent(openAppPendingIntent())
                .addAction(
                    R.drawable.close,
                    getString(android.R.string.cancel),
                    cancelPendingIntent(),
                )

        if (determinate) {
            builder.setProgress(100, progress.coerceIn(0, 100), false)
        } else {
            builder.setProgress(0, 0, true)
        }

        return builder.build()
    }

    private fun showCompleteNotification(result: PersonalLibraryFullSyncResult) {
        keepResultNotification = true
        val changedSongs = result.catalog.importedSongs + result.catalog.updatedSongs
        val changedPlaylists = result.playlists.importedPlaylists + result.playlists.updatedPlaylists
        val content =
            if (changedSongs == 0 && changedPlaylists == 0) {
                getString(R.string.library_import_notification_up_to_date)
            } else {
                getString(R.string.library_import_success_summary, changedSongs, changedPlaylists)
            }
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.library_add_check)
                .setContentTitle(getString(R.string.library_import_success_title))
                .setContentText(content)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent())
                .build()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun showErrorNotification(message: String) {
        keepResultNotification = true
        val notification =
            NotificationCompat
                .Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.library_add)
                .setContentTitle(getString(R.string.library_import_failed_title))
                .setContentText(message)
                .setAutoCancel(true)
                .setContentIntent(openAppPendingIntent())
                .build()
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)
    }

    private fun openAppPendingIntent(): PendingIntent {
        val intent =
            Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
        return PendingIntent.getActivity(
            this,
            REQUEST_OPEN_APP,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun cancelPendingIntent(): PendingIntent {
        val intent =
            Intent(this, PersonalLibraryImportService::class.java).apply {
                action = ACTION_CANCEL
            }
        return PendingIntent.getService(
            this,
            REQUEST_CANCEL,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)
        if (nm.getNotificationChannel(CHANNEL_ID) != null) return
        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.library_import_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.library_import_channel_desc)
            }
        nm.createNotificationChannel(channel)
    }

    companion object {
        private const val ACTION_CANCEL = "com.metrolist.music.subsonic.CANCEL_PERSONAL_LIBRARY_IMPORT"
        private const val CHANNEL_ID = "personal_library_import"
        private const val NOTIFICATION_ID = 2407
        private const val REQUEST_CANCEL = 2408
        private const val REQUEST_OPEN_APP = 2409

        @Volatile private var isRunning = false

        fun start(context: Context) {
            val intent = Intent(context, PersonalLibraryImportService::class.java)
            ContextCompat.startForegroundService(context, intent)
        }
    }
}

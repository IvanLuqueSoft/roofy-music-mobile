/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.device

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.metrolist.music.constants.DevicePlaybackTargetKey
import com.metrolist.music.desktopimport.DesktopConnect
import com.metrolist.music.desktopimport.DesktopRemoteClient
import com.metrolist.music.desktopimport.DesktopRemoteState
import com.metrolist.music.pairing.DevicePairingParams
import com.metrolist.music.utils.dataStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant

object DeviceSessionManager {
    private const val TAG = "RoofyDevice"

    private lateinit var appContext: Context
    private lateinit var dataStore: DataStore<Preferences>
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val mutableUiState = MutableStateFlow(DeviceSessionUiState())
    val uiState: StateFlow<DeviceSessionUiState> = mutableUiState.asStateFlow()

    val remoteState: StateFlow<DesktopRemoteState>
        get() = DesktopRemoteClient.state

    private var healthJob: Job? = null
    private var consecutiveHealthFailures = 0

    fun init(context: Context) {
        if (::appContext.isInitialized) return
        appContext = context.applicationContext
        dataStore = appContext.dataStore
        scope.launch {
            loadSession()
            startHealthLoopIfNeeded()
        }
        observeLifecycle()
    }

    fun isComputerOutputSelected(): Boolean = mutableUiState.value.isComputerOutputSelected

    /** Remote transport is usable (WebSocket up or connecting). */
    fun isComputerRemoteActive(): Boolean {
        if (!isComputerOutputSelected()) return false
        val remote = DesktopRemoteClient.state.value
        return remote.connected || remote.connecting
    }

    private fun observeLifecycle() {
        ProcessLifecycleOwner.get().lifecycle.addObserver(
            object : androidx.lifecycle.DefaultLifecycleObserver {
                override fun onStart(owner: androidx.lifecycle.LifecycleOwner) {
                    scope.launch {
                        if (mutableUiState.value.session != null) {
                            refreshHealth()
                        }
                    }
                }
            },
        )
    }

    suspend fun applyDevicePair(params: DevicePairingParams): Boolean {
        val session = DeviceSessionStore.fromDevicePairingParams(params) ?: return false
        DeviceSessionStore.write(dataStore, session)
        Timber.tag(TAG).i("device_session_paired computer=%s", session.computerName)
        mutableUiState.update {
            it.copy(
                session = session,
                connectionState = DeviceSessionConnectionState.Ready,
                lastError = null,
                bridgeReachable = true,
            )
        }
        notifyDesktopHealth(session)
        scope.launch { refreshHealth() }
        return true
    }

    suspend fun forgetSession() {
        healthJob?.cancel()
        healthJob = null
        consecutiveHealthFailures = 0
        DesktopRemoteClient.disconnect()
        withContext(NonCancellable) {
            DeviceSessionStore.clear(dataStore)
        }
        mutableUiState.value = DeviceSessionUiState()
        Timber.tag(TAG).i("device_session_cleared")
    }

    suspend fun setPlaybackTarget(target: String) {
        val session = mutableUiState.value.session
        val normalized =
            when {
                target == "computer" && session == null -> "phone"
                target == "computer" -> "computer"
                else -> "phone"
            }

        withContext(NonCancellable) {
            dataStore.edit { it[DevicePlaybackTargetKey] = normalized }
        }

        if (normalized == "computer" && session != null) {
            mutableUiState.update {
                it.copy(
                    playbackTarget = normalized,
                    connectionState = DeviceSessionConnectionState.ActiveComputer,
                    lastError = null,
                )
            }
            DesktopRemoteClient.connect(session.remote.wsUrl, session.remote.token)
            refreshHealth()
        } else {
            DesktopRemoteClient.disconnect()
            mutableUiState.update {
                it.copy(
                    playbackTarget = "phone",
                    connectionState =
                        if (session != null) {
                            DeviceSessionConnectionState.Ready
                        } else {
                            DeviceSessionConnectionState.Unpaired
                        },
                    lastError = null,
                )
            }
        }
    }

    fun shouldPlayOnDesktop(): Boolean = mutableUiState.value.shouldPlayOnDesktop

    fun bridgeEndpoint(): Pair<String, String>? {
        val session = mutableUiState.value.session ?: return null
        return session.bridge.baseUrl to session.bridge.token
    }

    fun computerName(): String = mutableUiState.value.computerName.ifBlank { "Your computer" }

    suspend fun refreshHealth() {
        val session = mutableUiState.value.session ?: return
        val controllingComputer = mutableUiState.value.playbackTarget == "computer"

        val result =
            runCatching {
                DesktopConnect.resolveLiveEndpoint(session.bridge.baseUrl, session.bridge.token).getOrThrow()
            }

        result
            .onSuccess { liveEndpoint ->
                consecutiveHealthFailures = 0
                val normalizedBase = session.bridge.baseUrl.trimEnd('/')
                if (liveEndpoint != normalizedBase) {
                    val updated =
                        session.copy(
                            bridge = session.bridge.copy(baseUrl = liveEndpoint),
                            lastHealthyAt = Instant.now().toString(),
                            lastError = null,
                        )
                    DeviceSessionStore.write(dataStore, updated)
                    mutableUiState.update {
                        it.copy(session = updated, bridgeReachable = true, lastError = null)
                    }
                } else {
                    mutableUiState.update {
                        it.copy(
                            bridgeReachable = true,
                            lastError = null,
                            connectionState =
                                when {
                                    it.playbackTarget == "computer" ->
                                        DeviceSessionConnectionState.ActiveComputer
                                    else -> DeviceSessionConnectionState.Ready
                                },
                        )
                    }
                }

                if (mutableUiState.value.playbackTarget == "computer") {
                    val activeSession = mutableUiState.value.session ?: return
                    val remote = DesktopRemoteClient.state.value
                    if (!remote.connected && !remote.connecting) {
                        DesktopRemoteClient.connect(activeSession.remote.wsUrl, activeSession.remote.token)
                    }
                }
            }.onFailure { error ->
                if (error is CancellationException) throw error
                consecutiveHealthFailures += 1
                val message = error.message ?: "Could not connect to your computer"
                Timber.tag(TAG).w(error, "health_check_failed")
                if (controllingComputer) {
                    if (consecutiveHealthFailures >= 3) {
                        DesktopRemoteClient.markUnavailable(message)
                        mutableUiState.update {
                            it.copy(
                                bridgeReachable = false,
                                lastError = message,
                                connectionState = DeviceSessionConnectionState.Unavailable,
                            )
                        }
                    } else {
                        DesktopRemoteClient.scheduleReconnectIfNeeded()
                        mutableUiState.update {
                            it.copy(
                                bridgeReachable = false,
                                lastError = null,
                                connectionState = DeviceSessionConnectionState.ActiveComputer,
                            )
                        }
                    }
                } else {
                    mutableUiState.update {
                        it.copy(
                            bridgeReachable = false,
                            lastError = message,
                            connectionState = DeviceSessionConnectionState.Ready,
                        )
                    }
                }
            }
    }

    suspend fun retryConnection() {
        val session = mutableUiState.value.session
        if (session == null) {
            setPlaybackTarget("phone")
            return
        }
        mutableUiState.update {
            it.copy(connectionState = DeviceSessionConnectionState.Pairing, lastError = null)
        }
        DesktopRemoteClient.connect(session.remote.wsUrl, session.remote.token)
        refreshHealth()
    }

    private suspend fun notifyDesktopHealth(session: DeviceSessionV1) {
        runCatching {
            DesktopConnect.resolveLiveEndpoint(session.bridge.baseUrl, session.bridge.token).getOrThrow()
        }
    }

    private suspend fun persistPlaybackTarget(target: String) {
        withContext(NonCancellable) {
            dataStore.edit { it[DevicePlaybackTargetKey] = target }
        }
    }

    private suspend fun revertComputerTargetIfUnreachable() {
        val session = mutableUiState.value.session ?: return
        val reachable =
            runCatching {
                DesktopConnect.resolveLiveEndpoint(session.bridge.baseUrl, session.bridge.token).getOrThrow()
            }.isSuccess
        if (!reachable) {
            Timber.tag(TAG).i("revert_computer_target_desktop_unreachable")
            DesktopRemoteClient.disconnect()
            persistPlaybackTarget("phone")
            mutableUiState.update {
                it.copy(
                    playbackTarget = "phone",
                    connectionState = DeviceSessionConnectionState.Ready,
                    bridgeReachable = false,
                    lastError = null,
                )
            }
        }
    }

    private fun startHealthLoopIfNeeded() {
        if (healthJob?.isActive == true) return
        healthJob =
            scope.launch(Dispatchers.IO) {
                while (isActive) {
                    if (mutableUiState.value.session != null) {
                        try {
                            refreshHealth()
                        } catch (error: CancellationException) {
                            throw error
                        } catch (error: Exception) {
                            Timber.tag(TAG).w(error, "health_loop_error")
                        }
                    }
                    val interval =
                        if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                            12_000L
                        } else {
                            45_000L
                        }
                    delay(interval)
                }
            }
    }

    suspend fun loadSession() {
        DesktopRemoteClient.disconnect()

        val session = DeviceSessionStore.read(dataStore)
        var playbackTarget = dataStore.data.first()[DevicePlaybackTargetKey] ?: "phone"

        if (session == null) {
            DeviceSessionStore.clearOrphanLegacyCredentials(dataStore)
            DesktopRemoteClient.disconnect()
            if (playbackTarget == "computer") {
                persistPlaybackTarget("phone")
                playbackTarget = "phone"
            }
            mutableUiState.value = DeviceSessionUiState(playbackTarget = playbackTarget)
            return
        }

        if (playbackTarget == "computer") {
            Timber.tag(TAG).i("reset_stale_computer_target_on_launch")
            persistPlaybackTarget("phone")
            playbackTarget = "phone"
            DesktopRemoteClient.disconnect()
        }

        mutableUiState.update {
            it.copy(
                session = session,
                playbackTarget = playbackTarget,
                connectionState = DeviceSessionConnectionState.Ready,
                bridgeReachable = false,
            )
        }
        refreshHealth()
        startHealthLoopIfNeeded()
    }
}

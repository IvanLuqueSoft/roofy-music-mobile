/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.desktopimport

import android.net.Uri
import androidx.core.net.toUri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DesktopRemoteTrack(
    val id: String? = null,
    val title: String = "",
    val artist: String = "",
    val album: String? = null,
    val imageUrl: String? = null,
    val durationMs: Long = 0L,
)

data class DesktopRemoteState(
    val connected: Boolean = false,
    val connecting: Boolean = false,
    val errorMessage: String? = null,
    val track: DesktopRemoteTrack? = null,
    val playbackStatus: String = "paused",
    val positionMs: Long = 0L,
    val volume: Int? = null,
    val lastConnectedAtMs: Long = 0L,
    val lastMessageAtMs: Long = 0L,
) {
    val isPlaying: Boolean
        get() = playbackStatus.equals("playing", ignoreCase = true)

    val isRecoveringConnection: Boolean
        get() =
            !connected &&
                connecting &&
                lastConnectedAtMs > 0L &&
                System.currentTimeMillis() - lastConnectedAtMs <= RECENT_CONNECTION_GRACE_MS

    companion object {
        const val RECENT_CONNECTION_GRACE_MS = 30_000L
    }
}

object DesktopRemoteClient {
    private val client =
        OkHttpClient
            .Builder()
            .pingInterval(20, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()

    private val mutableState = MutableStateFlow(DesktopRemoteState())
    val state = mutableState.asStateFlow()

    private var webSocket: WebSocket? = null
    private var connectionKey: String? = null
    private var lastRemoteUrl: String? = null
    private var lastRemoteToken: String? = null
    private var reconnectJob: Job? = null
    private var connectionTimeoutJob: Job? = null
    private var reconnectFailures = 0
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    fun connect(
        remoteUrl: String,
        token: String,
    ) {
        val endpointChanged = lastRemoteUrl != remoteUrl || lastRemoteToken != token
        if (endpointChanged) {
            reconnectFailures = 0
        }
        lastRemoteUrl = remoteUrl
        lastRemoteToken = token
        val wsUrl = buildWebSocketUrl(remoteUrl, token) ?: run {
            disconnect()
            mutableState.update {
                it.copy(
                    connected = false,
                    connecting = false,
                    errorMessage = "Remote link is missing",
                )
            }
            return
        }

        if (connectionKey == wsUrl && (mutableState.value.connected || mutableState.value.connecting)) {
            return
        }

        webSocket?.close(1000, "Reconnecting")
        connectionTimeoutJob?.cancel()
        connectionKey = wsUrl
        mutableState.update {
            it.copy(
                connecting = true,
                errorMessage = null,
                connected = false,
            )
        }

        val request = Request.Builder().url(wsUrl).build()
        webSocket =
            client.newWebSocket(
                request,
                object : WebSocketListener() {
                    override fun onOpen(
                        webSocket: WebSocket,
                        response: Response,
                    ) {
                        if (webSocket !== this@DesktopRemoteClient.webSocket) return
                        mutableState.update {
                            it.copy(
                                connected = true,
                                connecting = false,
                                errorMessage = null,
                                lastConnectedAtMs = System.currentTimeMillis(),
                            )
                        }
                        connectionTimeoutJob?.cancel()
                        reconnectFailures = 0
                    }

                    override fun onMessage(
                        webSocket: WebSocket,
                        text: String,
                    ) {
                        if (webSocket !== this@DesktopRemoteClient.webSocket) return
                        handleMessage(text)
                    }

                    override fun onClosing(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String,
                    ) {
                        webSocket.close(code, reason)
                    }

                    override fun onClosed(
                        webSocket: WebSocket,
                        code: Int,
                        reason: String,
                    ) {
                        if (webSocket !== this@DesktopRemoteClient.webSocket) return
                        handleSocketLoss("Connection closed")
                    }

                    override fun onFailure(
                        webSocket: WebSocket,
                        t: Throwable,
                        response: Response?,
                    ) {
                        if (webSocket !== this@DesktopRemoteClient.webSocket) return
                        handleSocketLoss(t.message ?: "Could not connect to your computer", delayMs = 2_000L)
                    }
                },
            )
        scheduleConnectionTimeout(wsUrl)
    }

    fun disconnect() {
        reconnectJob?.cancel()
        connectionTimeoutJob?.cancel()
        reconnectJob = null
        connectionTimeoutJob = null
        lastRemoteUrl = null
        lastRemoteToken = null
        reconnectFailures = 0
        webSocket?.close(1000, "Closed")
        webSocket = null
        connectionKey = null
        mutableState.update { DesktopRemoteState() }
    }

    fun scheduleReconnectIfNeeded(delayMs: Long = 1_500L) {
        val remoteUrl = lastRemoteUrl ?: return
        val token = lastRemoteToken ?: return
        reconnectJob?.cancel()
        reconnectJob =
            scope.launch {
                delay(delayMs)
                if (!mutableState.value.connected) {
                    connect(remoteUrl, token)
                }
            }
    }

    /** Update UI immediately after handoff, before the WebSocket echoes state. */
    fun applyHandoffSnapshot(snapshot: HandoffSnapshot) {
        val nextTrack = snapshot.nowPlaying?.toRemoteTrack()
        mutableState.update {
            it.copy(
                track = nextTrack,
                playbackStatus = snapshot.playbackStatus,
                positionMs = snapshot.positionMs.coerceAtLeast(0L),
                connecting = it.connecting,
                connected = it.connected,
                errorMessage = null,
                lastMessageAtMs = System.currentTimeMillis(),
            )
        }
        maybeRequestArtworkProxy(nextTrack)
    }

    private fun HandoffTrack.toRemoteTrack(): DesktopRemoteTrack =
        DesktopRemoteTrack(
            id = id,
            title = title,
            artist = artist,
            album = album,
            imageUrl = artworkUrl,
            durationMs = durationMs?.coerceAtLeast(0L) ?: 0L,
        )

    fun markUnavailable(message: String = "Could not connect to your computer") {
        reconnectJob?.cancel()
        connectionTimeoutJob?.cancel()
        webSocket?.close(1000, "Unavailable")
        webSocket = null
        connectionKey = null
        mutableState.update {
            it.copy(
                connected = false,
                connecting = false,
                errorMessage = message,
            )
        }
    }

    private fun handleSocketLoss(
        message: String,
        delayMs: Long = 1_500L,
    ) {
        webSocket = null
        connectionKey = null
        connectionTimeoutJob?.cancel()
        reconnectFailures += 1

        if (reconnectFailures >= 2) {
            markUnavailable(message)
            return
        }

        mutableState.update {
            it.copy(connected = false, connecting = true, errorMessage = null)
        }
        scheduleReconnectIfNeeded(delayMs = delayMs)
    }

    private fun scheduleConnectionTimeout(wsUrl: String) {
        connectionTimeoutJob?.cancel()
        connectionTimeoutJob =
            scope.launch {
                delay(8_000L)
                if (connectionKey == wsUrl && mutableState.value.connecting && !mutableState.value.connected) {
                    markUnavailable("Could not connect to your computer")
                }
            }
    }

    fun play() = sendSimple("play")

    fun pause() = sendSimple("pause")

    fun togglePlayPause() {
        if (mutableState.value.isPlaying) pause() else play()
    }

    fun next() = sendSimple("next")

    fun previous() = sendSimple("previous")

    fun seekTo(positionMs: Long) {
        send(JSONObject().put("event", "position").put("position", positionMs / 1000.0))
        mutableState.update { it.copy(positionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun setVolume(volume: Int) {
        send(JSONObject().put("event", "volume").put("volume", volume.coerceIn(0, 100)))
    }

    /** Ask the desktop to fetch artwork and return it as a base64 data URI (same as the web remote). */
    fun requestArtworkProxy() {
        if (!mutableState.value.connected) return
        sendSimple("proxy")
    }

    internal fun isDirectlyLoadableArtworkUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        if (url.startsWith("data:", ignoreCase = true)) return true
        return runCatching {
            val host = url.toUri().host ?: return@runCatching false
            !host.equals("localhost", ignoreCase = true) &&
                host != "127.0.0.1" &&
                !host.startsWith("127.")
        }.getOrDefault(false)
    }

    internal fun buildWebSocketUrl(
        remoteUrl: String,
        token: String,
    ): String? =
        runCatching {
            val parsed = Uri.parse(remoteUrl.trim())
            if (parsed.host.isNullOrBlank()) return@runCatching null

            val scheme =
                when (parsed.scheme?.lowercase()) {
                    "https", "wss" -> "wss"
                    else -> "ws"
                }
            val builder = parsed.buildUpon().scheme(scheme)
            if (parsed.getQueryParameter("token").isNullOrBlank() && token.isNotBlank()) {
                builder.appendQueryParameter("token", token)
            }
            builder.build().toString()
        }.getOrNull()

    private fun sendSimple(event: String) {
        send(JSONObject().put("event", event))
    }

    private fun send(payload: JSONObject) {
        webSocket?.send(payload.toString())
    }

    private fun handleMessage(text: String) {
        val message = runCatching { JSONObject(text) }.getOrNull() ?: return
        val event = message.optString("event")
        val data = message.opt("data")

        when (event) {
            "state" -> {
                val stateObject = data as? JSONObject ?: return
                val nextTrack = stateObject.optJSONObject("song")?.toRemoteTrack()
                mutableState.update {
                    it.copy(
                        track = nextTrack,
                        playbackStatus = stateObject.optString("status", it.playbackStatus),
                        positionMs = secondsToMs(stateObject.optDouble("position", it.positionMs / 1000.0)),
                        volume = stateObject.optIntOrNull("volume") ?: it.volume,
                        lastMessageAtMs = System.currentTimeMillis(),
                    )
                }
                maybeRequestArtworkProxy(nextTrack)
            }
            "song" -> {
                val nextTrack = (data as? JSONObject)?.toRemoteTrack()
                mutableState.update {
                    it.copy(
                        track = nextTrack,
                        lastMessageAtMs = System.currentTimeMillis(),
                    )
                }
                maybeRequestArtworkProxy(nextTrack)
            }
            "proxy" -> {
                val base64 = data?.toString()?.takeIf { it.isNotBlank() } ?: return
                val dataUri = "data:image/jpeg;base64,$base64"
                mutableState.update { state ->
                    state.copy(
                        track = state.track?.copy(imageUrl = dataUri) ?: state.track,
                        lastMessageAtMs = System.currentTimeMillis(),
                    )
                }
            }
            "playback" -> {
                mutableState.update {
                    it.copy(
                        playbackStatus = data?.toString() ?: it.playbackStatus,
                        lastMessageAtMs = System.currentTimeMillis(),
                    )
                }
            }
            "position" -> {
                val position = (data as? Number)?.toDouble() ?: data?.toString()?.toDoubleOrNull()
                if (position != null) {
                    mutableState.update {
                        it.copy(
                            positionMs = secondsToMs(position),
                            lastMessageAtMs = System.currentTimeMillis(),
                        )
                    }
                }
            }
            "volume" -> {
                val volume = (data as? Number)?.toInt() ?: data?.toString()?.toIntOrNull()
                if (volume != null) {
                    mutableState.update {
                        it.copy(
                            volume = volume,
                            lastMessageAtMs = System.currentTimeMillis(),
                        )
                    }
                }
            }
            "error" -> {
                mutableState.update {
                    it.copy(errorMessage = data?.toString())
                }
            }
        }
    }

    private fun JSONObject.toRemoteTrack(): DesktopRemoteTrack {
        val duration = optDouble("duration", 0.0)
        val imageUrl =
            optString("imageUrl")
                .ifBlank { optString("artworkUrl") }
                .ifBlank { null }
        return DesktopRemoteTrack(
            id = optString("id").ifBlank { null },
            title = optString("name").ifBlank { optString("title") },
            artist = optString("artistName").ifBlank { optString("artist") },
            album = optString("album").ifBlank { null },
            imageUrl = imageUrl,
            durationMs = normalizeDurationToMs(duration),
        )
    }

    private fun maybeRequestArtworkProxy(track: DesktopRemoteTrack?) {
        if (!isDirectlyLoadableArtworkUrl(track?.imageUrl)) {
            requestArtworkProxy()
        }
    }

    private fun JSONObject.optIntOrNull(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null

    private fun secondsToMs(seconds: Double): Long =
        (seconds.coerceAtLeast(0.0) * 1000.0).toLong()

    internal fun normalizeDurationToMs(duration: Double): Long {
        if (duration <= 0.0) return 0L
        return if (duration > 86_400.0) {
            duration.toLong()
        } else {
            secondsToMs(duration)
        }
    }
}

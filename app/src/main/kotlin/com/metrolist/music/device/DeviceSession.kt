/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.device

import kotlinx.serialization.Serializable

@Serializable
data class DeviceSessionLibrary(
    val serverUrl: String,
    val username: String,
    val password: String,
)

@Serializable
data class DeviceSessionBridge(
    val baseUrl: String,
    val token: String,
)

@Serializable
data class DeviceSessionRemote(
    val wsUrl: String,
    val token: String,
)

@Serializable
data class DeviceSessionCapabilities(
    val library: Boolean = true,
    val import: Boolean = true,
    val handoff: Boolean = true,
    val remote: Boolean = true,
)

@Serializable
data class DeviceSessionV1(
    val sessionId: String,
    val computerName: String,
    val pairedAt: String,
    val mode: String,
    val library: DeviceSessionLibrary,
    val bridge: DeviceSessionBridge,
    val remote: DeviceSessionRemote,
    val webControlUrl: String? = null,
    val capabilities: DeviceSessionCapabilities = DeviceSessionCapabilities(),
    val lastHealthyAt: String? = null,
    val lastError: String? = null,
)

enum class DeviceSessionConnectionState {
    Unpaired,
    Pairing,
    Ready,
    Degraded,
    ActiveComputer,
    Unavailable,
}

data class DeviceSessionUiState(
    val session: DeviceSessionV1? = null,
    val connectionState: DeviceSessionConnectionState = DeviceSessionConnectionState.Unpaired,
    val playbackTarget: String = "phone",
    val bridgeReachable: Boolean = false,
    val lastError: String? = null,
) {
    val isPaired: Boolean
        get() = session != null

    val computerName: String
        get() = session?.computerName.orEmpty()

    val shouldPlayOnDesktop: Boolean
        get() =
            playbackTarget == "computer" &&
                session != null &&
                connectionState != DeviceSessionConnectionState.Unavailable &&
                (connectionState == DeviceSessionConnectionState.Ready ||
                    connectionState == DeviceSessionConnectionState.ActiveComputer ||
                    connectionState == DeviceSessionConnectionState.Degraded)

    /** User chose this computer as the active output (stable; does not flicker with WebSocket). */
    val isComputerOutputSelected: Boolean
        get() = playbackTarget == "computer" && session != null

    /** True when the Now Playing UI should show the desktop remote surface (not merely paired). */
    val showComputerRemoteUi: Boolean
        get() = isComputerOutputSelected
}

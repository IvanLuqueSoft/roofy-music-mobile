/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.device

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.metrolist.music.constants.DesktopComputerNameKey
import com.metrolist.music.constants.DesktopImportEndpointUrlKey
import com.metrolist.music.constants.DesktopImportTokenKey
import com.metrolist.music.constants.DesktopRemoteControlTokenKey
import com.metrolist.music.constants.DesktopRemoteControlUrlKey
import com.metrolist.music.constants.DesktopWebControlUrlKey
import com.metrolist.music.constants.DevicePlaybackTargetKey
import com.metrolist.music.constants.PersonalLibraryEnabledKey
import com.metrolist.music.constants.PersonalLibraryPasswordKey
import com.metrolist.music.constants.PersonalLibraryServerUrlKey
import com.metrolist.music.constants.PersonalLibraryUsernameKey
import com.metrolist.music.pairing.DevicePairingParams
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

object DeviceSessionStore {
    val SessionJsonKey = stringPreferencesKey("deviceSessionV1")

    private val json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }

    suspend fun read(dataStore: DataStore<Preferences>): DeviceSessionV1? {
        val raw = dataStore.data.map { it[SessionJsonKey] }.first()?.trim().orEmpty()
        if (raw.isNotBlank()) {
            return runCatching { json.decodeFromString<DeviceSessionV1>(raw) }.getOrNull()
        }
        return migrateLegacySession(dataStore)
    }

    suspend fun write(
        dataStore: DataStore<Preferences>,
        session: DeviceSessionV1,
    ) {
        dataStore.edit { settings ->
            settings[SessionJsonKey] = json.encodeToString(session)
            applyLegacyKeys(settings, session)
        }
    }

    suspend fun clear(dataStore: DataStore<Preferences>) {
        dataStore.edit { settings ->
            removeLegacyDeviceKeys(settings)
            settings[DevicePlaybackTargetKey] = "phone"
        }
    }

    /** Drops leftover prefs when the v1 session blob is missing (e.g. partial clear / reinstall). */
    suspend fun clearOrphanLegacyCredentials(dataStore: DataStore<Preferences>) {
        val hasSession = dataStore.data.map { it[SessionJsonKey]?.isNotBlank() == true }.first()
        if (hasSession) return
        dataStore.edit { settings -> removeLegacyDeviceKeys(settings) }
    }

    private fun removeLegacyDeviceKeys(settings: androidx.datastore.preferences.core.MutablePreferences) {
        settings.remove(SessionJsonKey)
        settings.remove(PersonalLibraryEnabledKey)
        settings.remove(PersonalLibraryServerUrlKey)
        settings.remove(PersonalLibraryUsernameKey)
        settings.remove(PersonalLibraryPasswordKey)
        settings.remove(DesktopImportEndpointUrlKey)
        settings.remove(DesktopImportTokenKey)
        settings.remove(DesktopRemoteControlUrlKey)
        settings.remove(DesktopRemoteControlTokenKey)
        settings.remove(DesktopWebControlUrlKey)
        settings.remove(DesktopComputerNameKey)
    }

    fun fromDevicePairingParams(params: DevicePairingParams): DeviceSessionV1? {
        val remoteUrl = params.remoteControlUrl?.trim().orEmpty()
        val remoteToken = params.remoteControlToken?.trim().orEmpty()
        if (remoteUrl.isBlank() || remoteToken.isBlank()) {
            return null
        }

        return DeviceSessionV1(
            sessionId = UUID.randomUUID().toString(),
            computerName = params.computerName?.trim().orEmpty().ifBlank { "Your computer" },
            pairedAt = Instant.now().toString(),
            mode = "lan",
            library =
                DeviceSessionLibrary(
                    serverUrl = params.serverUrl,
                    username = params.username,
                    password = params.password,
                ),
            bridge =
                DeviceSessionBridge(
                    baseUrl = params.endpointUrl,
                    token = params.token,
                ),
            remote =
                DeviceSessionRemote(
                    wsUrl = remoteUrl,
                    token = remoteToken,
                ),
            webControlUrl = params.webControlUrl,
        )
    }

    private suspend fun migrateLegacySession(dataStore: DataStore<Preferences>): DeviceSessionV1? {
        val prefs = dataStore.data.first()
        val endpoint = prefs[DesktopImportEndpointUrlKey].orEmpty().trim()
        val token = prefs[DesktopImportTokenKey].orEmpty().trim()
        val serverUrl = prefs[PersonalLibraryServerUrlKey].orEmpty().trim()
        val username = prefs[PersonalLibraryUsernameKey].orEmpty().trim()
        val password = prefs[PersonalLibraryPasswordKey].orEmpty()
        val remoteUrl = prefs[DesktopRemoteControlUrlKey].orEmpty().trim()
        val remoteToken = prefs[DesktopRemoteControlTokenKey].orEmpty().trim()

        if (endpoint.isBlank() || token.isBlank() || serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
            return null
        }

        if (remoteUrl.isBlank() || remoteToken.isBlank()) {
            return null
        }

        val session =
            DeviceSessionV1(
                sessionId = UUID.randomUUID().toString(),
                computerName = prefs[DesktopComputerNameKey].orEmpty().ifBlank { "Your computer" },
                pairedAt = Instant.now().toString(),
                mode = "lan",
                library = DeviceSessionLibrary(serverUrl, username, password),
                bridge = DeviceSessionBridge(endpoint, token),
                remote = DeviceSessionRemote(remoteUrl, remoteToken),
                webControlUrl = prefs[DesktopWebControlUrlKey],
            )
        write(dataStore, session)
        return session
    }

    private fun applyLegacyKeys(
        settings: androidx.datastore.preferences.core.MutablePreferences,
        session: DeviceSessionV1,
    ) {
        settings[PersonalLibraryEnabledKey] = true
        settings[PersonalLibraryServerUrlKey] = session.library.serverUrl
        settings[PersonalLibraryUsernameKey] = session.library.username
        settings[PersonalLibraryPasswordKey] = session.library.password
        settings[DesktopImportEndpointUrlKey] = session.bridge.baseUrl
        settings[DesktopImportTokenKey] = session.bridge.token
        settings[DesktopRemoteControlUrlKey] = session.remote.wsUrl
        settings[DesktopRemoteControlTokenKey] = session.remote.token
        settings[DesktopComputerNameKey] = session.computerName
        session.webControlUrl?.let { settings[DesktopWebControlUrlKey] = it }
    }
}

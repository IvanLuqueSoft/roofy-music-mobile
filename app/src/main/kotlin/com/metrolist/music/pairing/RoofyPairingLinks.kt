/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.pairing

import android.net.Uri

data class SubsonicPairingParams(
    val serverUrl: String,
    val username: String,
    val password: String,
)

data class ImportPairingParams(
    val endpointUrl: String,
    val token: String,
)

data class DevicePairingParams(
    val serverUrl: String,
    val username: String,
    val password: String,
    val endpointUrl: String,
    val token: String,
    val computerName: String? = null,
    val remoteControlUrl: String? = null,
    val remoteControlToken: String? = null,
    val webControlUrl: String? = null,
)

object RoofyPairingLinks {
    fun isSubsonicPairLink(uri: Uri): Boolean =
        uri.scheme.equals("roofymusic", ignoreCase = true) &&
            uri.host.equals("pair", ignoreCase = true) &&
            uri.pathSegments.firstOrNull().equals("subsonic", ignoreCase = true)

    fun isImportPairLink(uri: Uri): Boolean =
        uri.scheme.equals("roofymusic", ignoreCase = true) &&
            uri.host.equals("pair", ignoreCase = true) &&
            uri.pathSegments.firstOrNull().equals("import", ignoreCase = true)

    fun isDevicePairLink(uri: Uri): Boolean =
        uri.scheme.equals("roofymusic", ignoreCase = true) &&
            uri.host.equals("pair", ignoreCase = true) &&
            uri.pathSegments.firstOrNull().equals("device", ignoreCase = true)

    fun parseSubsonicPair(uri: Uri): SubsonicPairingParams? {
        if (!isSubsonicPairLink(uri)) return null
        return parseSubsonicPairQuery(
            serverUrl = uri.getQueryParameter("serverUrl"),
            username = uri.getQueryParameter("username"),
            password = uri.getQueryParameter("password"),
        )
    }

    fun parseSubsonicPairQuery(
        serverUrl: String?,
        username: String?,
        password: String?,
    ): SubsonicPairingParams? {
        val normalizedServerUrl = serverUrl?.trim().orEmpty().trimEnd('/')
        val normalizedUsername = username?.trim().orEmpty()
        val normalizedPassword = password.orEmpty()
        if (normalizedServerUrl.isBlank() || normalizedUsername.isBlank() || normalizedPassword.isBlank()) {
            return null
        }
        return SubsonicPairingParams(normalizedServerUrl, normalizedUsername, normalizedPassword)
    }

    fun parseImportPair(uri: Uri): ImportPairingParams? {
        if (!isImportPairLink(uri)) return null
        return parseImportPairQuery(
            endpointUrl = uri.getQueryParameter("endpointUrl"),
            token = uri.getQueryParameter("token"),
        )
    }

    fun parseDevicePair(uri: Uri): DevicePairingParams? {
        if (!isDevicePairLink(uri)) return null
        val subsonic =
            parseSubsonicPairQuery(
                serverUrl = uri.getQueryParameter("serverUrl"),
                username = uri.getQueryParameter("username"),
                password = uri.getQueryParameter("password"),
            ) ?: return null
        val import =
            parseImportPairQuery(
                endpointUrl = uri.getQueryParameter("endpointUrl"),
                token = uri.getQueryParameter("token"),
            ) ?: return null
        val webControlUrl = uri.getQueryParameter("webControlUrl")?.trim().orEmpty().ifBlank { null }
        val computerName = uri.getQueryParameter("computerName")?.trim().orEmpty().ifBlank { null }
        val remoteControlUrl =
            uri
                .getQueryParameter("remoteControlUrl")
                ?.trim()
                .orEmpty()
                .ifBlank { webControlUrl }
        val remoteControlToken =
            uri
                .getQueryParameter("remoteControlToken")
                ?.trim()
                .orEmpty()
                .ifBlank { tokenFromUrl(remoteControlUrl) }
        return DevicePairingParams(
            serverUrl = subsonic.serverUrl,
            username = subsonic.username,
            password = subsonic.password,
            endpointUrl = import.endpointUrl,
            token = import.token,
            computerName = computerName,
            remoteControlUrl = remoteControlUrl,
            remoteControlToken = remoteControlToken,
            webControlUrl = webControlUrl,
        )
    }

    fun parseImportPairQuery(
        endpointUrl: String?,
        token: String?,
    ): ImportPairingParams? {
        val normalizedEndpoint = endpointUrl?.trim().orEmpty().trimEnd('/')
        val normalizedToken = token.orEmpty()
        if (normalizedEndpoint.isBlank() || normalizedToken.isBlank()) return null
        return ImportPairingParams(normalizedEndpoint, normalizedToken)
    }

    private fun tokenFromUrl(remoteUrl: String?): String? {
        if (remoteUrl.isNullOrBlank()) return null
        return runCatching {
            Uri.parse(remoteUrl).getQueryParameter("token")?.trim().orEmpty().ifBlank { null }
        }.getOrNull()
    }
}

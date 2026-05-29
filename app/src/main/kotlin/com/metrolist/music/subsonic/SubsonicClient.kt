/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.subsonic

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.URLBuilder
import io.ktor.http.encodedPath
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.security.MessageDigest
import java.util.UUID

class SubsonicClient(
    private val credentials: PersonalLibraryCredentials,
    private val clientName: String = "RoofyMusicMobile",
    private val apiVersion: String = "1.16.1",
    httpClientOverride: HttpClient? = null,
) {
    private val httpClient = httpClientOverride ?: sharedHttpClient

    suspend fun ping(): SubsonicResponse =
        request("ping.view").response.ensureOk()

    suspend fun search3(
        query: String,
        songCount: Int = 50,
        albumCount: Int = 20,
        artistCount: Int = 20,
    ): SubsonicSearchResult =
        request("search3.view") {
            parameters.append("query", query)
            parameters.append("songCount", songCount.toString())
            parameters.append("albumCount", albumCount.toString())
            parameters.append("artistCount", artistCount.toString())
        }.response.ensureOk().searchResult3 ?: SubsonicSearchResult()

    suspend fun getSong(id: String): SubsonicSong? =
        request("getSong.view") {
            parameters.append("id", id)
        }.response.ensureOk().song

    suspend fun getStarred2(): SubsonicStarred =
        request("getStarred2.view").response.ensureOk().starred2

    suspend fun starSong(id: String): SubsonicResponse =
        request("star.view") {
            parameters.append("id", id)
        }.response.ensureOk()

    suspend fun unstarSong(id: String): SubsonicResponse =
        request("unstar.view") {
            parameters.append("id", id)
        }.response.ensureOk()

    suspend fun getPlaylists(): List<SubsonicPlaylist> =
        request("getPlaylists.view").response.ensureOk().playlists?.playlist ?: emptyList()

    suspend fun getPlaylist(id: String): SubsonicPlaylist =
        request("getPlaylist.view") {
            parameters.append("id", id)
        }.response.ensureOk().playlist ?: throw IllegalStateException("Playlist not found")

    suspend fun createPlaylist(
        name: String,
        songIds: List<String>,
    ): SubsonicResponse =
        request("createPlaylist.view") {
            parameters.append("name", name)
            songIds.forEach { parameters.append("songId", it) }
        }.response.ensureOk()

    suspend fun updatePlaylist(
        playlistId: String,
        songIdsToAdd: List<String>,
    ): SubsonicResponse =
        request("updatePlaylist.view") {
            parameters.append("playlistId", playlistId)
            songIdsToAdd.forEach { parameters.append("songIdToAdd", it) }
        }.response.ensureOk()

    fun streamUrl(id: String): String =
        endpoint("stream.view") {
            parameters.append("id", id)
        }

    fun coverArtUrl(id: String): String =
        endpoint("getCoverArt.view") {
            parameters.append("id", id)
        }

    private suspend fun request(
        path: String,
        block: URLBuilder.() -> Unit = {},
    ): SubsonicEnvelope =
        httpClient.get(endpoint(path, block)).body()

    private fun endpoint(
        path: String,
        block: URLBuilder.() -> Unit = {},
    ): String {
        val trimmedBaseUrl = credentials.serverUrl.trim().trimEnd('/')
        val baseUrl =
            if (trimmedBaseUrl.startsWith("http://") || trimmedBaseUrl.startsWith("https://")) {
                trimmedBaseUrl
            } else {
                "http://$trimmedBaseUrl"
            }
        return URLBuilder().apply {
            takeFrom(baseUrl)
            encodedPath = "${encodedPath.trimEnd('/')}/rest/$path"
            authParameters().forEach { (key, value) -> parameters.append(key, value) }
            block()
        }.buildString()
    }

    private fun authParameters(): Map<String, String> {
        val salt = UUID.randomUUID().toString().replace("-", "")
        return mapOf(
            "u" to credentials.username,
            "t" to md5(credentials.password + salt),
            "s" to salt,
            "v" to apiVersion,
            "c" to clientName,
            "f" to "json",
        )
    }

    private fun SubsonicResponse.ensureOk(): SubsonicResponse {
        if (status.equals("ok", ignoreCase = true)) return this
        throw IllegalStateException(error?.message ?: "Subsonic request failed")
    }

    companion object {
        private val sharedHttpClient: HttpClient by lazy { defaultHttpClient() }

        @OptIn(ExperimentalSerializationApi::class)
        fun defaultHttpClient() =
            HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(
                        Json {
                            ignoreUnknownKeys = true
                            explicitNulls = false
                        }
                    )
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = 30_000
                    connectTimeoutMillis = 15_000
                    socketTimeoutMillis = 30_000
                }
            }

        fun localIdFromMediaId(mediaId: String): String? =
            mediaId.takeIf { it.startsWith(SUBSONIC_MEDIA_ID_PREFIX) }?.removePrefix(SUBSONIC_MEDIA_ID_PREFIX)

        fun mediaId(id: String): String = "$SUBSONIC_MEDIA_ID_PREFIX$id"

        private fun md5(value: String): String =
            MessageDigest
                .getInstance("MD5")
                .digest(value.toByteArray())
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }
    }
}

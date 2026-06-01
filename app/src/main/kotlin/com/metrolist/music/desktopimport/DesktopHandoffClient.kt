/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.desktopimport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

@Serializable
data class HandoffTrack(
    val source: String,
    val id: String,
    val title: String,
    val artist: String,
    val album: String? = null,
    val durationMs: Long? = null,
    val artworkUrl: String? = null,
)

@Serializable
data class HandoffSnapshot(
    val positionMs: Long = 0,
    val playbackStatus: String = "paused",
    @SerialName("nowPlaying")
    val nowPlaying: HandoffTrack? = null,
    val queue: List<HandoffTrack> = emptyList(),
)

object DesktopHandoffClient {
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    private val json = "application/json; charset=utf-8".toMediaType()
    private val parser =
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }

    suspend fun fetchState(
        endpointUrl: String,
        token: String,
    ): Result<HandoffSnapshot> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request =
                    Request
                        .Builder()
                        .url("${endpointUrl.normalizedBaseUrl()}/handoff/state")
                        .addHeader("Authorization", "Bearer ${token.trim()}")
                        .addHeader("X-Roofy-Device-Name", DesktopConnect.deviceName)
                        .get()
                        .build()

                client.newCall(request).execute().use { response ->
                    val body = response.body?.string().orEmpty()
                    if (!response.isSuccessful) {
                        val message =
                            runCatching {
                                JSONObject(body).optString("error")
                            }.getOrNull()
                                ?.takeIf { it.isNotBlank() }
                                ?: "Desktop handoff returned ${response.code}"
                        throw IllegalStateException(message)
                    }
                    parser.decodeFromString(HandoffSnapshot.serializer(), body)
                }
            }.recoverCatching { error ->
                throw DesktopConnect.mapConnectionError(error, endpointUrl)
            }
        }

    suspend fun pushState(
        endpointUrl: String,
        token: String,
        snapshot: HandoffSnapshot,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val payload = parser.encodeToString(HandoffSnapshot.serializer(), snapshot)
                val request =
                    Request
                        .Builder()
                        .url("${endpointUrl.normalizedBaseUrl()}/handoff/play")
                        .addHeader("Authorization", "Bearer ${token.trim()}")
                        .addHeader("Content-Type", "application/json")
                        .addHeader("X-Roofy-Device-Name", DesktopConnect.deviceName)
                        .post(payload.toRequestBody(json))
                        .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val responseBody = response.body?.string().orEmpty()
                        throw IllegalStateException(
                            responseBody.ifBlank { "Desktop handoff failed with ${response.code}" },
                        )
                    }
                }
            }.recoverCatching { error ->
                throw DesktopConnect.mapConnectionError(error, endpointUrl)
            }
        }

    private fun String.normalizedBaseUrl() = trim().trimEnd('/')
}

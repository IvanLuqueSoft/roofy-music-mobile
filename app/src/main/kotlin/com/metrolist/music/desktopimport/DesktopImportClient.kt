/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.desktopimport

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class DesktopImportTrack(
    val artist: String? = null,
    val artists: List<String> = emptyList(),
    val thumbnailUrl: String? = null,
    val title: String,
    val url: String,
    val videoId: String,
)

object DesktopImportClient {
    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    private val json = "application/json; charset=utf-8".toMediaType()

    suspend fun health(
        endpointUrl: String,
        token: String,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request =
                    Request
                        .Builder()
                        .url("${endpointUrl.normalizedBaseUrl()}/health")
                        .addHeader("Authorization", "Bearer ${token.trim()}")
                        .get()
                        .build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        throw IllegalStateException("Desktop import endpoint returned ${response.code}")
                    }
                }
            }
        }

    suspend fun sendImport(
        endpointUrl: String,
        token: String,
        track: DesktopImportTrack,
    ): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val body =
                    JSONObject()
                        .put("url", track.url)
                        .put("title", track.title)
                        .put("artist", track.artist)
                        .put("artists", JSONArray(track.artists))
                        .put("thumbnailUrl", track.thumbnailUrl)
                        .put("videoId", track.videoId)

                val request =
                    Request
                        .Builder()
                        .url("${endpointUrl.normalizedBaseUrl()}/import")
                        .addHeader("Authorization", "Bearer ${token.trim()}")
                        .addHeader("Content-Type", "application/json")
                        .post(body.toString().toRequestBody(json))
                        .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val responseBody = response.body?.string().orEmpty()
                        throw IllegalStateException(responseBody.ifBlank { "Desktop import failed with ${response.code}" })
                    }
                }
            }
        }

    private fun String.normalizedBaseUrl() = trim().trimEnd('/')
}

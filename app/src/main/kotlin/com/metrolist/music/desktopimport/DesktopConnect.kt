/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.desktopimport

import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Keeps the mobile ↔ desktop bridge reachable: pings /health, refreshes the public
 * endpoint when the desktop tunnel rotates, and surfaces friendly connection errors.
 */
object DesktopConnect {
    val deviceName: String
        get() = listOf(Build.MANUFACTURER, Build.MODEL)
            .filter { it.isNotBlank() }
            .joinToString(" ")
            .replaceFirstChar { it.titlecase() }

    private val client =
        OkHttpClient
            .Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()

    suspend fun resolveLiveEndpoint(
        endpointUrl: String,
        token: String,
    ): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val normalized = endpointUrl.normalizedBaseUrl()
                val request =
                    Request
                        .Builder()
                        .url("$normalized/health")
                        .addHeader("Authorization", "Bearer ${token.trim()}")
                        .addHeader("X-Roofy-Device-Name", deviceName)
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
                                ?: when (response.code) {
                                    503 -> "Desktop connection is not ready. Open Devices on your computer and try again."
                                    else -> "Desktop returned ${response.code}"
                                }
                        throw IllegalStateException(message)
                    }

                    val refreshed =
                        runCatching {
                            JSONObject(body).optString("endpointUrl")
                        }.getOrNull()
                            ?.trim()
                            ?.takeIf { it.isNotBlank() }

                    refreshed?.normalizedBaseUrl() ?: normalized
                }
            }.recoverCatching { error ->
                throw mapConnectionError(error, endpointUrl)
            }
        }

    fun mapConnectionError(error: Throwable, endpointUrl: String): IllegalStateException {
        if (error is IllegalStateException && error !is IOException) {
            return error
        }
        val cause = generateSequence(error) { it.cause }.firstOrNull { it !== error } ?: error
        val message =
            when (cause) {
                is UnknownHostException,
                is ConnectException,
                is SocketTimeoutException,
                is IOException,
                -> {
                    val hostHint =
                        runCatching {
                            val host = endpointUrl.substringAfter("://").substringBefore('/').substringBefore(':')
                            host
                        }.getOrDefault("")
                    if (hostHint.startsWith("192.168.") || hostHint.startsWith("10.") || hostHint.startsWith("172.")) {
                        "Could not reach your computer. Make sure your phone is on the same Wi‑Fi, or open Devices on the desktop app to refresh the connection."
                    } else {
                        "Could not reach your computer. Open Roofy on your desktop (Devices in the player) and try again."
                    }
                }
                else -> cause.message?.takeIf { it.isNotBlank() }
                    ?: "Could not connect to your computer."
            }
        return IllegalStateException(message, cause)
    }

    private fun String.normalizedBaseUrl() = trim().trimEnd('/')
}

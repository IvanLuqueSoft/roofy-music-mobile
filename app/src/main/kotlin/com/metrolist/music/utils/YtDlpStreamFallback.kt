/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.utils

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.response.PlayerResponse
import com.metrolist.music.constants.AudioQuality
import com.yausername.youtubedl_android.YoutubeDL
import com.yausername.youtubedl_android.YoutubeDLException
import com.yausername.youtubedl_android.YoutubeDLRequest
import com.yausername.youtubedl_android.mapper.VideoFormat
import com.yausername.youtubedl_android.mapper.VideoInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit

object YtDlpStreamFallback {
    private const val TAG = "YtDlpFallback"
    private const val DEFAULT_EXPIRES_IN_SECONDS = 6 * 60 * 60
    private const val DEFAULT_CONTENT_LENGTH = 10_000_000L

    @Volatile
    private var appContext: Context? = null

    @Volatile
    private var initialized = false

    private val httpClient =
        OkHttpClient
            .Builder()
            .proxy(YouTube.proxy)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

    data class ResultData(
        val format: PlayerResponse.StreamingData.Format,
        val streamUrl: String,
        val streamExpiresInSeconds: Int,
    )

    fun setContext(context: Context) {
        appContext = context.applicationContext
    }

    fun initialize(context: Context) {
        setContext(context)
        tryInitialize()
    }

    suspend fun resolve(
        videoId: String,
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): Result<ResultData> =
        withContext(Dispatchers.IO) {
            runCatching {
                ensureInitialized()

                val request = YoutubeDLRequest("https://www.youtube.com/watch?v=$videoId")
                    .addOption("--no-playlist")
                    .addOption("--no-warnings")
                    .addOption("--extractor-args", "youtube:player_client=web_music,web_embedded,tv,android_vr,ios,web")
                    .addOption("-f", formatSelector(audioQuality, connectivityManager))

                YouTube.cookie?.takeIf { it.isNotBlank() }?.let { cookie ->
                    request.addOption("--add-header", "Cookie:$cookie")
                }

                Timber.tag(TAG).i("Resolving stream with yt-dlp fallback: videoId=$videoId")
                val info = YoutubeDL.getInstance().getInfo(request)
                val streamUrl = info.url?.takeIf { it.isNotBlank() }
                    ?: selectedFormat(info)?.url?.takeIf { it.isNotBlank() }
                    ?: throw YoutubeDLException("yt-dlp did not return a playable stream URL")

                if (!validateStreamUrl(streamUrl)) {
                    throw YoutubeDLException("yt-dlp stream URL failed validation")
                }

                val selected = selectedFormat(info)
                val contentLength = contentLength(info, selected, streamUrl)
                val format = buildFormat(info, selected, streamUrl, contentLength)
                val expiresInSeconds = expiresInSeconds(streamUrl)

                Timber.tag(TAG).i(
                    "yt-dlp fallback resolved stream: videoId=$videoId, " +
                        "format=${format.itag}, mime=${format.mimeType}, expires=${expiresInSeconds}s",
                )

                ResultData(
                    format = format,
                    streamUrl = streamUrl,
                    streamExpiresInSeconds = expiresInSeconds,
                )
            }.onFailure { error ->
                Timber.tag(TAG).w(error, "yt-dlp fallback failed")
            }
        }

    private fun tryInitialize() {
        val context = appContext ?: return
        if (initialized) return

        synchronized(this) {
            if (initialized) return
            try {
                YoutubeDL.getInstance().init(context)
                initialized = true
                Timber.tag(TAG).i("yt-dlp initialized")
            } catch (e: Exception) {
                Timber.tag(TAG).w(e, "Failed to initialize yt-dlp")
            }
        }
    }

    private fun ensureInitialized() {
        if (!initialized) {
            tryInitialize()
        }
        check(initialized) { "yt-dlp fallback is not initialized" }
    }

    private fun formatSelector(
        audioQuality: AudioQuality,
        connectivityManager: ConnectivityManager,
    ): String =
        when (audioQuality) {
            AudioQuality.LOW -> "bestaudio[abr<=128]/bestaudio/best"
            AudioQuality.HIGH -> "bestaudio[abr<=256]/bestaudio/best"
            AudioQuality.VERY_HIGH -> "bestaudio/best"
            AudioQuality.AUTO ->
                if (connectivityManager.isActiveNetworkMetered) {
                    "bestaudio[abr<=128]/bestaudio/best"
                } else {
                    "bestaudio/best"
                }
        }

    private fun selectedFormat(info: VideoInfo): VideoFormat? {
        val selectedFormatId = info.formatId
        val selected = info.formats
            ?.firstOrNull { it.formatId == selectedFormatId }

        return selected
            ?: info.requestedFormats?.firstOrNull { it.isAudioOnly() }
            ?: info.formats
                ?.filter { it.isAudioOnly() && !it.url.isNullOrBlank() }
                ?.maxByOrNull { maxOf(it.abr, it.tbr) }
    }

    private fun VideoFormat.isAudioOnly(): Boolean =
        (vcodec == null || vcodec == "none") &&
            acodec != null &&
            acodec != "none"

    private fun buildFormat(
        info: VideoInfo,
        selected: VideoFormat?,
        streamUrl: String,
        contentLength: Long,
    ): PlayerResponse.StreamingData.Format {
        val bitrateKbps = selected?.abr?.takeIf { it > 0 }
            ?: selected?.tbr?.takeIf { it > 0 }
            ?: 128
        val ext = selected?.ext ?: info.ext ?: streamUrl.substringBefore("?").substringAfterLast('.', "webm")
        val acodec = selected?.acodec?.takeUnless { it == "none" } ?: when (ext) {
            "m4a", "mp4" -> "mp4a.40.2"
            "opus", "webm" -> "opus"
            else -> "unknown"
        }
        val mimeType = when (ext) {
            "m4a", "mp4" -> "audio/mp4; codecs=\"$acodec\""
            "opus", "webm" -> "audio/webm; codecs=\"$acodec\""
            else -> "audio/$ext; codecs=\"$acodec\""
        }
        val itag = selected?.formatId?.toIntOrNull() ?: 0

        return PlayerResponse.StreamingData.Format(
            itag = itag,
            url = streamUrl,
            mimeType = mimeType,
            bitrate = bitrateKbps * 1000,
            width = null,
            height = null,
            contentLength = contentLength,
            quality = selected?.formatNote ?: "yt-dlp",
            fps = null,
            qualityLabel = null,
            averageBitrate = bitrateKbps * 1000,
            audioQuality = "AUDIO_QUALITY_MEDIUM",
            approxDurationMs = info.duration.takeIf { it > 0 }?.let { (it * 1000L).toString() },
            audioSampleRate = selected?.asr?.takeIf { it > 0 },
            audioChannels = null,
            loudnessDb = null,
            lastModified = null,
            signatureCipher = null,
            cipher = null,
            audioTrack = null,
        )
    }

    private fun contentLength(
        info: VideoInfo,
        selected: VideoFormat?,
        streamUrl: String,
    ): Long {
        val fromInfo = selected?.fileSize?.takeIf { it > 0 }
            ?: selected?.fileSizeApproximate?.takeIf { it > 0 }
            ?: info.fileSize.takeIf { it > 0 }
            ?: info.fileSizeApproximate.takeIf { it > 0 }
        if (fromInfo != null) return fromInfo

        return fetchContentLength(streamUrl) ?: DEFAULT_CONTENT_LENGTH
    }

    private fun expiresInSeconds(streamUrl: String): Int {
        val expireEpochSeconds = Uri.parse(streamUrl)
            .getQueryParameter("expire")
            ?.toLongOrNull()
        val nowEpochSeconds = System.currentTimeMillis() / 1000L

        return expireEpochSeconds
            ?.let { (it - nowEpochSeconds).coerceAtLeast(60L).toInt() }
            ?: DEFAULT_EXPIRES_IN_SECONDS
    }

    private fun validateStreamUrl(streamUrl: String): Boolean {
        val headRequest = Request.Builder().head().url(streamUrl).build()
        try {
            httpClient.newCall(headRequest).execute().use { response ->
                if (response.isSuccessful) return true
                if (response.code != 405) return false
            }
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "yt-dlp HEAD validation failed")
        }

        val rangeRequest =
            Request.Builder()
                .url(streamUrl)
                .header("Range", "bytes=0-0")
                .build()
        return try {
            httpClient.newCall(rangeRequest).execute().use { response ->
                response.isSuccessful
            }
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "yt-dlp range validation failed")
            false
        }
    }

    private fun fetchContentLength(streamUrl: String): Long? {
        val request = Request.Builder().head().url(streamUrl).build()
        return try {
            httpClient.newCall(request).execute().use { response ->
                response.header("Content-Length")?.toLongOrNull()
            }
        } catch (e: Exception) {
            Timber.tag(TAG).d(e, "Could not read yt-dlp stream Content-Length")
            null
        }
    }
}

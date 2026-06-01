/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.desktopimport

import androidx.media3.common.MediaItem
import com.metrolist.music.db.MusicDatabase
import com.metrolist.music.extensions.metadata
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.Queue
import com.metrolist.music.subsonic.PersonalLibraryCredentials
import com.metrolist.music.subsonic.SUBSONIC_MEDIA_ID_PREFIX
import com.metrolist.music.subsonic.SubsonicClient
import com.metrolist.music.subsonic.toMediaItem
import com.metrolist.music.subsonic.toRoofyMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object HandoffPlayback {
    private val YOUTUBE_VIDEO_ID_REGEX = Regex("^[A-Za-z0-9_-]{11}$")

    fun buildSnapshot(
        status: Queue.Status,
        playbackStatus: String = "playing",
    ): HandoffSnapshot {
        val nowPlaying = status.items.getOrNull(status.mediaItemIndex)?.metadata?.toHandoffTrack()
        val queueTracks =
            status.items
                .mapIndexedNotNull { index, item ->
                    if (index == status.mediaItemIndex) return@mapIndexedNotNull null
                    item.metadata?.toHandoffTrack()
                }.take(50)

        return HandoffSnapshot(
            positionMs = status.position.coerceAtLeast(0),
            playbackStatus = playbackStatus,
            nowPlaying = nowPlaying,
            queue = queueTracks,
        )
    }

    fun buildSnapshot(playerConnection: PlayerConnection): HandoffSnapshot {
        val player = playerConnection.player
        val currentMetadata = player.currentMediaItem?.metadata
        val nowPlaying = currentMetadata?.toHandoffTrack()
        val queueTracks = mutableListOf<HandoffTrack>()

        for (index in 0 until player.mediaItemCount) {
            if (index == player.currentMediaItemIndex) continue
            val metadata = player.getMediaItemAt(index).metadata ?: continue
            metadata.toHandoffTrack()?.let(queueTracks::add)
        }

        val playbackStatus =
            if (player.isPlaying) {
                "playing"
            } else {
                "paused"
            }

        return HandoffSnapshot(
            positionMs = player.currentPosition.coerceAtLeast(0),
            playbackStatus = playbackStatus,
            nowPlaying = nowPlaying,
            queue = queueTracks.take(50),
        )
    }

    suspend fun continueOnDesktop(
        playerConnection: PlayerConnection,
        endpointUrl: String,
        token: String,
    ): String {
        val snapshot =
            withContext(Dispatchers.Main) {
                buildSnapshot(playerConnection)
            }
        if (snapshot.nowPlaying == null) {
            throw IllegalStateException("Nothing is playing on this device.")
        }
        val liveEndpoint =
            withContext(Dispatchers.IO) {
                val resolved = DesktopConnect.resolveLiveEndpoint(endpointUrl, token).getOrThrow()
                DesktopHandoffClient.pushState(resolved, token, snapshot).getOrThrow()
                resolved
            }
        return liveEndpoint
    }

    suspend fun continueFromDesktop(
        database: MusicDatabase,
        playerConnection: PlayerConnection,
        endpointUrl: String,
        token: String,
        personalLibraryCredentials: PersonalLibraryCredentials?,
    ): String {
        val liveEndpoint =
            withContext(Dispatchers.IO) {
                DesktopConnect.resolveLiveEndpoint(endpointUrl, token).getOrThrow()
            }
        val snapshot =
            withContext(Dispatchers.IO) {
                DesktopHandoffClient.fetchState(liveEndpoint, token).getOrThrow()
            }
        val tracks = listOfNotNull(snapshot.nowPlaying) + snapshot.queue
        if (tracks.isEmpty()) {
            throw IllegalStateException("Desktop is not playing anything.")
        }

        val subsonicClient =
            personalLibraryCredentials?.takeIf { it.isConfigured }?.let { SubsonicClient(it) }

        val mediaItems =
            withContext(Dispatchers.IO) {
                val resolved = mutableListOf<MediaItem>()
                tracks.forEach { track ->
                    resolveTrack(database, track, subsonicClient)?.let(resolved::add)
                }
                resolved
            }

        if (mediaItems.isEmpty()) {
            throw IllegalStateException("No handoff tracks could be resolved on this device.")
        }

        val shouldPlay = snapshot.playbackStatus.equals("playing", ignoreCase = true)
        val positionMs = snapshot.positionMs.coerceAtLeast(0)
        withContext(Dispatchers.Main) {
            playerConnection.playQueue(
                ListQueue(
                    title = "Roofy Connect",
                    items = mediaItems,
                    startIndex = 0,
                    position = positionMs,
                ),
            )
            if (!shouldPlay) {
                playerConnection.pause()
            }
        }
        return liveEndpoint
    }

    private suspend fun resolveTrack(
        database: MusicDatabase,
        track: HandoffTrack,
        subsonicClient: SubsonicClient?,
    ): MediaItem? =
        when (track.source.lowercase()) {
            "subsonic" -> {
                val client = subsonicClient ?: return null
                val song = client.getSong(track.id) ?: return null
                database.withTransaction {
                    insert(song.toRoofyMetadata(client))
                }
                song.toMediaItem(client)
            }
            "youtube" -> {
                val metadata =
                    MediaMetadata(
                        id = track.id,
                        title = track.title,
                        artists =
                            listOf(
                                MediaMetadata.Artist(
                                    id = null,
                                    name = track.artist,
                                ),
                            ),
                        duration = ((track.durationMs ?: 0) / 1000).toInt().takeIf { it > 0 } ?: -1,
                        thumbnailUrl = track.artworkUrl,
                    )
                database.withTransaction {
                    insert(metadata)
                }
                metadata.toMediaItem()
            }
            else -> null
        }

    private fun MediaMetadata.toHandoffTrack(): HandoffTrack? {
        if (id.startsWith(SUBSONIC_MEDIA_ID_PREFIX)) {
            val rawId = SubsonicClient.localIdFromMediaId(id) ?: return null
            return HandoffTrack(
                source = "subsonic",
                id = rawId,
                title = title,
                artist = artists.joinToString { it.name }.ifBlank { "Unknown artist" },
                album = album?.title,
                durationMs = duration.takeIf { it > 0 }?.times(1000L),
                artworkUrl = thumbnailUrl,
            )
        }

        val youtubeVideoId = resolveYoutubeVideoId()
        if (youtubeVideoId != null) {
            return HandoffTrack(
                source = "youtube",
                id = youtubeVideoId,
                title = title,
                artist = artists.joinToString { it.name }.ifBlank { "Unknown artist" },
                album = album?.title,
                durationMs = duration.takeIf { it > 0 }?.times(1000L),
                artworkUrl = thumbnailUrl,
            )
        }

        return null
    }

    /** Playable watch id — often in [setVideoId] when [id] is a browse/navigation token. */
    internal fun MediaMetadata.resolveYoutubeVideoId(): String? {
        val candidates =
            listOfNotNull(
                setVideoId?.trim()?.takeIf { it.isNotEmpty() },
                id.removePrefix("ytm:").trim().takeIf { it.isNotEmpty() },
            )
        return candidates.firstOrNull { YOUTUBE_VIDEO_ID_REGEX.matches(it) }
    }
}

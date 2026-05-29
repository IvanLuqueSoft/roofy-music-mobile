/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.subsonic

import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata.MEDIA_TYPE_MUSIC
import com.metrolist.music.models.MediaMetadata

fun SubsonicSong.toRoofyMetadata(client: SubsonicClient): MediaMetadata =
    MediaMetadata(
        id = SubsonicClient.mediaId(id),
        title = title,
        artists = listOf(
            MediaMetadata.Artist(
                id = artistId,
                name = artist ?: "Unknown artist",
            )
        ),
        duration = duration ?: -1,
        thumbnailUrl = coverArt?.let(client::coverArtUrl),
        album = albumId?.let {
            MediaMetadata.Album(
                id = it,
                title = album.orEmpty(),
            )
        },
    )

fun SubsonicSong.toMediaItem(client: SubsonicClient): MediaItem {
    val metadata = toRoofyMetadata(client)
    return MediaItem.Builder()
        .setMediaId(metadata.id)
        .setUri(metadata.id)
        .setCustomCacheKey(metadata.id)
        .setTag(metadata)
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle(metadata.title)
                .setSubtitle(metadata.artists.joinToString { it.name })
                .setArtist(metadata.artists.joinToString { it.name })
                .setArtworkUri(metadata.thumbnailUrl?.toUri())
                .setAlbumTitle(metadata.album?.title)
                .setAlbumArtist(metadata.artists.firstOrNull()?.name)
                .setDisplayTitle(metadata.title)
                .setMediaType(MEDIA_TYPE_MUSIC)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .build()
        )
        .build()
}

/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

internal object AndroidAutoMediaIds {
    val BROWSER_SOURCES = setOf(
        MusicService.SONG,
        MusicService.ARTIST,
        MusicService.ALBUM,
        MusicService.PLAYLIST,
        MusicService.YOUTUBE_PLAYLIST,
        MusicService.SEARCH,
    )

    data class Parsed(
        val source: String,
        val collectionId: String? = null,
        val itemId: String? = null,
    ) {
        val isShuffle: Boolean
            get() = itemId == MusicService.SHUFFLE_ACTION
    }

    fun child(
        parentId: String,
        itemId: String,
    ): String = "$parentId/$itemId"

    fun search(
        query: String,
        itemId: String,
    ): String = "${MusicService.SEARCH}/$query/$itemId"

    fun parse(mediaId: String?): Parsed? {
        if (mediaId.isNullOrBlank()) return null

        val parts = mediaId.split("/")
        return when (parts.firstOrNull()) {
            MusicService.ROOT -> Parsed(source = MusicService.ROOT)
            MusicService.SONG -> Parsed(
                source = MusicService.SONG,
                itemId = parts.getOrNull(1),
            )
            MusicService.ARTIST,
            MusicService.ALBUM,
            MusicService.PLAYLIST,
            MusicService.YOUTUBE_PLAYLIST,
            -> Parsed(
                source = parts[0],
                collectionId = parts.getOrNull(1),
                itemId = parts.getOrNull(2),
            )
            MusicService.SEARCH -> {
                if (parts.size == 1) {
                    Parsed(source = MusicService.SEARCH)
                } else {
                    Parsed(
                        source = MusicService.SEARCH,
                        collectionId = parts.drop(1).dropLast(1).joinToString("/").ifBlank { parts.getOrNull(1) },
                        itemId = parts.takeIf { it.size >= 3 }?.last(),
                    )
                }
            }
            else -> Parsed(source = mediaId, itemId = mediaId)
        }
    }
}

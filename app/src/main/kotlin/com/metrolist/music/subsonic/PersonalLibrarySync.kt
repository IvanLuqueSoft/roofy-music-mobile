/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.subsonic

import com.metrolist.music.db.MusicDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

data class PersonalLibraryFavoriteSyncResult(
    val importedFavorites: Int,
    val pushedFavorites: Int,
    val remoteFavorites: Int,
    val updatedFavorites: Int,
)

object PersonalLibrarySync {
    suspend fun syncFavorites(
        database: MusicDatabase,
        client: SubsonicClient,
    ): PersonalLibraryFavoriteSyncResult =
        withContext(Dispatchers.IO) {
            val remoteSongs = client.getStarred2().song
            val remoteRawIds = remoteSongs.map { it.id }.toSet()
            val localLikedSongs = database.likedSubsonicSongs()
            val localOnlyRawIds =
                localLikedSongs
                    .mapNotNull { SubsonicClient.localIdFromMediaId(it.id) }
                    .filterNot(remoteRawIds::contains)
                    .distinct()

            localOnlyRawIds.forEach { client.starSong(it) }

            var imported = 0
            var updated = 0
            val now = LocalDateTime.now()

            database.withTransaction {
                remoteSongs.forEach { song ->
                    val metadata = song.toRoofyMetadata(client)
                    val existing = getSongByIdBlocking(metadata.id)
                    if (existing == null) {
                        insert(metadata) {
                            it.copy(
                                inLibrary = now,
                                liked = true,
                                likedDate = now,
                            )
                        }
                        imported += 1
                    } else if (!existing.song.liked || existing.song.inLibrary == null) {
                        update(
                            existing.song.copy(
                                inLibrary = existing.song.inLibrary ?: now,
                                liked = true,
                                likedDate = existing.song.likedDate ?: now,
                            )
                        )
                        updated += 1
                    }
                }
            }

            PersonalLibraryFavoriteSyncResult(
                importedFavorites = imported,
                pushedFavorites = localOnlyRawIds.size,
                remoteFavorites = remoteSongs.size,
                updatedFavorites = updated,
            )
        }
}

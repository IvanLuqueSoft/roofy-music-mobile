/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.subsonic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

const val SUBSONIC_MEDIA_ID_PREFIX = "subsonic:"
const val SUBSONIC_PLAYLIST_BROWSE_PREFIX = "subsonic:playlist:"
const val SUBSONIC_PENDING_PLAYLIST_BROWSE_PREFIX = "subsonic:pending:"

fun subsonicPlaylistLocalId(remoteId: String) = "PL_SUB_$remoteId"

fun subsonicPlaylistBrowseId(remoteId: String) = "$SUBSONIC_PLAYLIST_BROWSE_PREFIX$remoteId"

fun subsonicRemoteIdFromBrowseId(browseId: String?): String? =
    browseId
        ?.takeIf { it.startsWith(SUBSONIC_PLAYLIST_BROWSE_PREFIX) }
        ?.removePrefix(SUBSONIC_PLAYLIST_BROWSE_PREFIX)

@Serializable
data class SubsonicEnvelope(
    @SerialName("subsonic-response")
    val response: SubsonicResponse,
)

@Serializable
data class SubsonicResponse(
    val status: String,
    val version: String? = null,
    val type: String? = null,
    val serverVersion: String? = null,
    val error: SubsonicError? = null,
    val searchResult3: SubsonicSearchResult? = null,
    val song: SubsonicSong? = null,
    val starred2: SubsonicStarred = SubsonicStarred(),
    val playlists: SubsonicPlaylists? = null,
    val playlist: SubsonicPlaylist? = null,
)

@Serializable
data class SubsonicPlaylists(
    val playlist: List<SubsonicPlaylist> = emptyList(),
)

@Serializable
data class SubsonicPlaylist(
    val id: String,
    val name: String,
    val owner: String? = null,
    val `public`: Boolean? = null,
    val songCount: Int? = null,
    val created: String? = null,
    val changed: String? = null,
    val coverArt: String? = null,
    val entry: List<SubsonicSong> = emptyList(),
)

@Serializable
data class SubsonicError(
    val code: Int,
    val message: String,
)

@Serializable
data class SubsonicSearchResult(
    val song: List<SubsonicSong> = emptyList(),
    val album: List<SubsonicAlbum> = emptyList(),
    val artist: List<SubsonicArtist> = emptyList(),
)

@Serializable
data class SubsonicStarred(
    val song: List<SubsonicSong> = emptyList(),
    val album: List<SubsonicAlbum> = emptyList(),
    val artist: List<SubsonicArtist> = emptyList(),
)

@Serializable
data class SubsonicSong(
    val id: String,
    val parent: String? = null,
    val title: String,
    val album: String? = null,
    val albumId: String? = null,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val duration: Int? = null,
    val bitRate: Int? = null,
    val contentType: String? = null,
    val suffix: String? = null,
    val path: String? = null,
    val starred: String? = null,
)

@Serializable
data class SubsonicAlbum(
    val id: String,
    val name: String,
    val artist: String? = null,
    val artistId: String? = null,
    val coverArt: String? = null,
    val songCount: Int? = null,
    val duration: Int? = null,
    val year: Int? = null,
)

@Serializable
data class SubsonicArtist(
    val id: String,
    val name: String,
    val albumCount: Int? = null,
    val coverArt: String? = null,
)

data class PersonalLibraryCredentials(
    val serverUrl: String,
    val username: String,
    val password: String,
) {
    val isConfigured: Boolean
        get() = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
}

/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidAutoMediaIdsTest {
    @Test
    fun `parses top-level song id`() {
        val parsed = AndroidAutoMediaIds.parse("song/video123")

        assertEquals(MusicService.SONG, parsed?.source)
        assertNull(parsed?.collectionId)
        assertEquals("video123", parsed?.itemId)
        assertFalse(parsed?.isShuffle ?: true)
    }

    @Test
    fun `parses collection child id`() {
        val parsed = AndroidAutoMediaIds.parse("playlist/liked/video123")

        assertEquals(MusicService.PLAYLIST, parsed?.source)
        assertEquals("liked", parsed?.collectionId)
        assertEquals("video123", parsed?.itemId)
    }

    @Test
    fun `parses shuffle child id`() {
        val parsed = AndroidAutoMediaIds.parse("album/album123/${MusicService.SHUFFLE_ACTION}")

        assertEquals(MusicService.ALBUM, parsed?.source)
        assertEquals("album123", parsed?.collectionId)
        assertEquals(MusicService.SHUFFLE_ACTION, parsed?.itemId)
        assertTrue(parsed?.isShuffle ?: false)
    }

    @Test
    fun `preserves slash characters in search query`() {
        val parsed = AndroidAutoMediaIds.parse("search/acdc/live/video123")

        assertEquals(MusicService.SEARCH, parsed?.source)
        assertEquals("acdc/live", parsed?.collectionId)
        assertEquals("video123", parsed?.itemId)
    }

    @Test
    fun `parses search query without selected item`() {
        val parsed = AndroidAutoMediaIds.parse("search/acdc")

        assertEquals(MusicService.SEARCH, parsed?.source)
        assertEquals("acdc", parsed?.collectionId)
        assertNull(parsed?.itemId)
    }

    @Test
    fun `builds child and search ids`() {
        assertEquals("artist/artist123/video123", AndroidAutoMediaIds.child("artist/artist123", "video123"))
        assertEquals("search/hello world/video123", AndroidAutoMediaIds.search("hello world", "video123"))
    }
}

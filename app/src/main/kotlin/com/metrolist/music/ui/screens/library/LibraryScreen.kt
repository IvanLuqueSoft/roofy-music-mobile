/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.R
import com.metrolist.music.constants.ChipSortTypeKey
import com.metrolist.music.constants.LibraryFilter
import com.metrolist.music.ui.theme.RetroTokens
import com.metrolist.music.utils.rememberEnumPreference

@Composable
fun LibraryScreen(navController: NavController) {
    var filterType by rememberEnumPreference(ChipSortTypeKey, LibraryFilter.LIBRARY)

    val tabs = listOf(
        LibraryFilter.LIBRARY to "MIX",
        LibraryFilter.PLAYLISTS to stringResource(R.string.filter_playlists).uppercase(),
        LibraryFilter.SONGS to stringResource(R.string.filter_songs).uppercase(),
        LibraryFilter.ALBUMS to stringResource(R.string.filter_albums).uppercase(),
        LibraryFilter.ARTISTS to stringResource(R.string.filter_artists).uppercase(),
        LibraryFilter.PODCASTS to stringResource(R.string.filter_podcasts).uppercase(),
    )

    val filterContent = @Composable {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(36.dp)
                .background(RetroTokens.Background2)
                .border(1.dp, RetroTokens.Border),
        ) {
            tabs.forEachIndexed { index, (value, label) ->
                val isActive = filterType == value
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(if (isActive) RetroTokens.Panel2 else Color.Transparent)
                        .border(
                            width = 1.dp,
                            color = if (isActive) RetroTokens.BorderBright else Color.Transparent,
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {
                                filterType = if (filterType == value) LibraryFilter.LIBRARY else value
                            },
                        )
                        .padding(horizontal = 4.dp),
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isActive) RetroTokens.TextHot else RetroTokens.TextSoft,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                    )
                }
                if (index < tabs.lastIndex) {
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(RetroTokens.Border),
                    )
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (filterType) {
            LibraryFilter.LIBRARY -> LibraryMixScreen(navController, filterContent)
            LibraryFilter.PLAYLISTS -> LibraryPlaylistsScreen(navController, filterContent)
            LibraryFilter.SONGS -> LibrarySongsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
            )
            LibraryFilter.ALBUMS -> LibraryAlbumsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
            )
            LibraryFilter.ARTISTS -> LibraryArtistsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
            )
            LibraryFilter.PODCASTS -> LibraryPodcastsScreen(
                navController,
                { filterType = LibraryFilter.LIBRARY },
            )
        }
    }
}

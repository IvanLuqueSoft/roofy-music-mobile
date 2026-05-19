/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.theme.RetroArtwork
import com.metrolist.music.ui.theme.RetroGridItem
import com.metrolist.music.ui.theme.RetroIconButton
import com.metrolist.music.ui.theme.RetroListItem
import com.metrolist.music.ui.theme.RetroPanel
import com.metrolist.music.ui.theme.RetroSectionHeader
import com.metrolist.music.ui.theme.RetroTextButton
import com.metrolist.music.ui.theme.RetroTokens
import com.metrolist.music.viewmodels.ChartsViewModel
import com.metrolist.music.viewmodels.ExploreViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ExploreScreen(
    navController: NavController,
    exploreViewModel: ExploreViewModel = hiltViewModel(),
    chartsViewModel: ChartsViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val explorePage by exploreViewModel.explorePage.collectAsStateWithLifecycle()
    val chartsPage by chartsViewModel.chartsPage.collectAsStateWithLifecycle()
    val isChartsLoading by chartsViewModel.isLoading.collectAsStateWithLifecycle()

    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop by backStackEntry
        ?.savedStateHandle
        ?.getStateFlow("scrollToTop", false)
        ?.collectAsStateWithLifecycle() ?: return

    LaunchedEffect(Unit) {
        if (chartsPage == null) {
            chartsViewModel.loadCharts()
        }
    }

    LaunchedEffect(scrollToTop) {
        if (scrollToTop) {
            scrollState.animateScrollTo(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        Column(
            modifier = Modifier.verticalScroll(scrollState),
        ) {
            Spacer(
                Modifier.height(
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateTopPadding(),
                ),
            )

            if (isChartsLoading || chartsPage == null || explorePage == null) {
                ShimmerHost {
                    TextPlaceholder(
                        height = 36.dp,
                        modifier =
                            Modifier
                                .padding(12.dp)
                                .fillMaxWidth(0.5f),
                    )
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
                        val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor

                        LazyHorizontalGrid(
                            rows = GridCells.Fixed(4),
                            contentPadding = PaddingValues(start = 4.dp),
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(ListItemHeight * 4),
                        ) {
                            items(4) {
                                Row(
                                    modifier =
                                        Modifier
                                            .width(horizontalLazyGridItemWidth)
                                            .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Box(
                                        modifier =
                                            Modifier
                                                .size(ListItemHeight - 16.dp)
                                                .background(RetroTokens.Background)
                                                .border(1.dp, RetroTokens.BorderMuted),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column(
                                        modifier = Modifier.fillMaxHeight(),
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .height(16.dp)
                                                    .width(120.dp)
                                                    .background(RetroTokens.Background),
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Box(
                                            modifier =
                                                Modifier
                                                    .height(12.dp)
                                                    .width(80.dp)
                                                    .background(RetroTokens.Background),
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TextPlaceholder(
                        height = 36.dp,
                        modifier =
                            Modifier
                                .padding(vertical = 12.dp, horizontal = 12.dp)
                                .width(250.dp),
                    )
                    Row {
                        repeat(2) {
                            GridItemPlaceHolder()
                        }
                    }

                    TextPlaceholder(
                        height = 36.dp,
                        modifier =
                            Modifier
                                .padding(vertical = 12.dp, horizontal = 12.dp)
                                .width(250.dp),
                    )
                    Row {
                        repeat(2) {
                            GridItemPlaceHolder()
                        }
                    }

                    TextPlaceholder(
                        height = 36.dp,
                        modifier =
                            Modifier
                                .padding(vertical = 12.dp, horizontal = 12.dp)
                                .width(250.dp),
                    )
                    repeat(4) {
                        Row {
                            repeat(2) {
                                TextPlaceholder(
                                    height = 48.dp,
                                    modifier =
                                        Modifier
                                            .padding(horizontal = 6.dp)
                                            .width(200.dp),
                                )
                            }
                        }
                    }
                }
            } else {
                // Charts panel
                RetroPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                ) {
                    Column {
                        chartsPage?.sections?.filter { it.title != "Top music videos" }?.forEach { section ->
                            RetroSectionHeader(
                                title = when (section.title) {
                                    "Trending" -> stringResource(R.string.trending)
                                    else -> section.title.ifEmpty { stringResource(R.string.charts) }
                                },
                            )
                            LazyRow(
                                contentPadding =
                                    WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                            ) {
                                items(
                                    items = section.items.filterIsInstance<SongItem>().distinctBy { it.id },
                                    key = { "explore_song_${it.id}" },
                                ) { song ->
                                    RetroListItem(
                                        index = null,
                                        title = song.title,
                                        subtitle = song.artists.joinToString { it.name },
                                        onClick = {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        endpoint = WatchEndpoint(videoId = song.id),
                                                        preloadItem = song.toMediaMetadata(),
                                                    ),
                                                )
                                            }
                                        },
                                        trailing = {
                                            RetroIconButton(
                                                onClick = {
                                                    menuState.show {
                                                        YouTubeSongMenu(
                                                            song = song,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp),
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.more_vert),
                                                    contentDescription = null,
                                                    tint = RetroTokens.TextSoft,
                                                    modifier = Modifier.size(18.dp),
                                                )
                                            }
                                        },
                                        modifier = Modifier.width(280.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                // New releases panel
                explorePage?.newReleaseAlbums?.let { newReleaseAlbums ->
                    RetroPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Column {
                            RetroSectionHeader(
                                title = stringResource(R.string.new_release_albums),
                                action = {
                                    RetroTextButton(
                                        text = "MORE",
                                        onClick = { navController.navigate("new_release") },
                                    )
                                },
                            )
                            LazyRow(
                                contentPadding =
                                    WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                            ) {
                                items(
                                    items = newReleaseAlbums.distinctBy { it.id },
                                    key = { "explore_album_${it.id}" },
                                ) { album ->
                                    RetroGridItem(
                                        title = album.title,
                                        subtitle = album.artists?.joinToString { it.name },
                                        onClick = { navController.navigate("album/${album.id}") },
                                        modifier = Modifier
                                            .width(120.dp)
                                            .padding(4.dp),
                                    ) {
                                        RetroArtwork(modifier = Modifier.fillMaxSize()) {
                                            AsyncImage(
                                                model = album.thumbnail,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Top music videos panel
                chartsPage?.sections?.find { it.title == "Top music videos" }?.let { topVideosSection ->
                    RetroPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Column {
                            RetroSectionHeader(
                                title = stringResource(R.string.top_music_videos),
                            )
                            LazyRow(
                                contentPadding =
                                    WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                            ) {
                                items(
                                    items = topVideosSection.items.filterIsInstance<SongItem>().distinctBy { it.id },
                                    key = { "explore_video_${it.id}" },
                                ) { video ->
                                    RetroGridItem(
                                        title = video.title,
                                        subtitle = video.artists.joinToString { it.name },
                                        onClick = {
                                            if (video.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                playerConnection.playQueue(
                                                    YouTubeQueue(
                                                        endpoint = WatchEndpoint(videoId = video.id),
                                                        preloadItem = video.toMediaMetadata(),
                                                    ),
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .width(120.dp)
                                            .padding(4.dp),
                                    ) {
                                        RetroArtwork(modifier = Modifier.fillMaxSize()) {
                                            AsyncImage(
                                                model = video.thumbnail,
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Mood and genres panel
                explorePage?.moodAndGenres?.let { moodAndGenres ->
                    RetroPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Column {
                            RetroSectionHeader(
                                title = stringResource(R.string.mood_and_genres),
                                action = {
                                    RetroTextButton(
                                        text = "MORE",
                                        onClick = { navController.navigate("mood_and_genres") },
                                    )
                                },
                            )
                            val itemsPerRow = 2
                            moodAndGenres.chunked(itemsPerRow).forEach { row ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 8.dp),
                                ) {
                                    row.forEach {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .weight(1f)
                                                .padding(4.dp)
                                                .aspectRatio(1f)
                                                .background(RetroTokens.Panel)
                                                .border(1.dp, RetroTokens.Border, RoundedCornerShape(0.dp))
                                                .clickable {
                                                    navController.navigate("youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}")
                                                }
                                                .padding(4.dp),
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    painter = painterResource(R.drawable.music_note),
                                                    contentDescription = null,
                                                    tint = RetroTokens.TextSoft,
                                                    modifier = Modifier.size(24.dp),
                                                )
                                                Spacer(modifier = Modifier.padding(vertical = 2.dp))
                                                Text(
                                                    text = it.title.uppercase(),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = RetroTokens.Text,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                    textAlign = TextAlign.Center,
                                                )
                                            }
                                        }
                                    }
                                    repeat(itemsPerRow - row.size) {
                                        Spacer(Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(
                Modifier.height(
                    LocalPlayerAwareWindowInsets.current.asPaddingValues().calculateBottomPadding(),
                ),
            )
        }
    }
}

/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.artist

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.HideExplicitKey
import com.metrolist.music.constants.ShowArtistDescriptionKey
import com.metrolist.music.constants.ShowArtistSubscriberCountKey
import com.metrolist.music.constants.ShowMonthlyListenersKey
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.NavigationTitle
import com.metrolist.music.ui.component.shimmer.ButtonPlaceholder
import com.metrolist.music.ui.component.shimmer.ListItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.theme.RetroArtwork
import com.metrolist.music.ui.theme.RetroCommandBar
import com.metrolist.music.ui.theme.RetroGridItem
import com.metrolist.music.ui.theme.RetroIconButton
import com.metrolist.music.ui.theme.RetroListItem
import com.metrolist.music.ui.theme.RetroPanel
import com.metrolist.music.ui.theme.RetroSectionHeader
import com.metrolist.music.ui.theme.RetroTextButton
import com.metrolist.music.ui.theme.RetroTokens
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.ArtistViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ArtistScreen(
    navController: NavController,
    viewModel: ArtistViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val playerConnection = LocalPlayerConnection.current ?: return
    val listenTogetherManager = LocalListenTogetherManager.current
    val isGuest = listenTogetherManager?.isInRoom == true && !listenTogetherManager.isHost
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val artistPage = viewModel.artistPage
    val libraryArtist by viewModel.libraryArtist.collectAsStateWithLifecycle()
    val librarySongs by viewModel.librarySongs.collectAsStateWithLifecycle()
    val libraryAlbums by viewModel.libraryAlbums.collectAsStateWithLifecycle()
    val isChannelSubscribed by viewModel.isChannelSubscribed.collectAsStateWithLifecycle()
    val hideExplicit by rememberPreference(key = HideExplicitKey, defaultValue = false)
    val showArtistDescription by rememberPreference(key = ShowArtistDescriptionKey, defaultValue = true)
    val showArtistSubscriberCount by rememberPreference(key = ShowArtistSubscriberCountKey, defaultValue = true)
    val showMonthlyListeners by rememberPreference(key = ShowMonthlyListenersKey, defaultValue = true)

    var showLocal by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(libraryArtist) {
        showLocal = libraryArtist?.artist?.isLocal == true
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Box(
        modifier = Modifier.fillMaxSize(),
    ) {
        LazyColumn(
            contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
        ) {
            if (artistPage == null && !showLocal) {
                item(key = "shimmer") {
                    ShimmerHost(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        // Bordered panel placeholder
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .background(RetroTokens.Panel)
                                    .padding(16.dp),
                        ) {
                            Column {
                                TextPlaceholder(
                                    height = 36.dp,
                                    modifier =
                                        Modifier
                                            .fillMaxWidth(0.7f)
                                            .padding(bottom = 16.dp),
                                )
                                Row {
                                    ButtonPlaceholder(
                                        modifier =
                                            Modifier
                                                .width(100.dp)
                                                .height(32.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ButtonPlaceholder(
                                        modifier =
                                            Modifier
                                                .width(100.dp)
                                                .height(32.dp),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    ButtonPlaceholder(
                                        modifier =
                                            Modifier
                                                .width(100.dp)
                                                .height(32.dp),
                                    )
                                }
                            }
                        }
                        repeat(6) {
                            ListItemPlaceHolder()
                        }
                    }
                }
            } else {
                item(key = "header") {
                    val thumbnail = artistPage?.artist?.thumbnail ?: libraryArtist?.artist?.thumbnailUrl
                    val artistName = artistPage?.artist?.title ?: libraryArtist?.artist?.name

                    RetroPanel(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                        ) {
                            if (thumbnail != null) {
                                RetroArtwork(
                                    modifier = Modifier.size(80.dp),
                                ) {
                                    AsyncImage(
                                        model = thumbnail.resize(400, 400),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize(),
                                    )
                                }
                            }

                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = artistName ?: "Unknown",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )

                                val subscriberCount = artistPage?.subscriberCountText
                                val monthlyListeners = artistPage?.monthlyListenerCount
                                if (showArtistSubscriberCount && !subscriberCount.isNullOrEmpty()) {
                                    Text(
                                        text = subscriberCount,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RetroTokens.TextMuted,
                                    )
                                }
                                if (showMonthlyListeners && !monthlyListeners.isNullOrEmpty()) {
                                    Text(
                                        text = monthlyListeners,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = RetroTokens.TextMuted,
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                RetroCommandBar {
                                    if (!showLocal && !isGuest) {
                                        artistPage?.artist?.radioEndpoint?.let { radioEndpoint ->
                                            RetroTextButton(
                                                text = stringResource(R.string.radio),
                                                onClick = {
                                                    playerConnection.playQueue(YouTubeQueue(radioEndpoint))
                                                },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                    if (!showLocal && !isGuest) {
                                        artistPage?.artist?.shuffleEndpoint?.let { shuffleEndpoint ->
                                            RetroTextButton(
                                                text = stringResource(R.string.shuffle),
                                                onClick = {
                                                    playerConnection.playQueue(YouTubeQueue(shuffleEndpoint))
                                                },
                                                modifier = Modifier.weight(1f),
                                            )
                                        }
                                    }
                                    RetroTextButton(
                                        text = stringResource(if (isChannelSubscribed) R.string.subscribed else R.string.subscribe),
                                        onClick = {
                                            viewModel.toggleChannelSubscription()
                                        },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }

                // About Artist Section
                if (!showLocal && (showArtistDescription || showArtistSubscriberCount || showMonthlyListeners)) {
                    val description = artistPage?.description
                    val descriptionRuns = artistPage?.descriptionRuns
                    val subscriberCount = artistPage?.subscriberCountText
                    val monthlyListeners = artistPage?.monthlyListenerCount

                    if ((showArtistDescription && !description.isNullOrEmpty()) ||
                        (showArtistSubscriberCount && !subscriberCount.isNullOrEmpty()) ||
                        (showMonthlyListeners && !monthlyListeners.isNullOrEmpty())
                    ) {
                        item(key = "about_artist") {
                            var expanded by remember { mutableStateOf(false) }
                            RetroPanel(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(bottom = 16.dp)
                                        .animateItem(),
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                ) {
                                    if (showArtistSubscriberCount && !subscriberCount.isNullOrEmpty()) {
                                        Text(
                                            text = subscriberCount,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = RetroTokens.TextSoft,
                                            modifier = Modifier.padding(bottom = 4.dp),
                                        )
                                    }

                                    if (showMonthlyListeners && !monthlyListeners.isNullOrEmpty()) {
                                        Text(
                                            text = monthlyListeners,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = RetroTokens.TextSoft,
                                            modifier = Modifier.padding(bottom = 4.dp),
                                        )
                                    }

                                    if (showArtistDescription && !description.isNullOrEmpty()) {
                                        Text(
                                            text = description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = RetroTokens.TextSoft,
                                            maxLines = if (expanded) Int.MAX_VALUE else 3,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    RetroTextButton(
                                        text = if (expanded) stringResource(R.string.show_less) else stringResource(R.string.show_more),
                                        onClick = { expanded = !expanded },
                                    )
                                }
                            }
                        }
                    }
                }

                if (showLocal) {
                    if (librarySongs.isNotEmpty()) {
                        item(key = "local_songs_title") {
                            RetroSectionHeader(
                                title = stringResource(R.string.songs),
                                modifier = Modifier.animateItem(),
                                action = {
                                    RetroTextButton(
                                        text = stringResource(R.string.show_more),
                                        onClick = {
                                            navController.navigate("artist/${viewModel.artistId}/songs")
                                        },
                                    )
                                },
                            )
                        }

                        val filteredLibrarySongs =
                            if (hideExplicit) {
                                librarySongs.filter { !it.song.explicit }
                            } else {
                                librarySongs
                            }
                        itemsIndexed(
                            items = filteredLibrarySongs,
                            key = { index, item -> "local_song_${item.id}_$index" },
                        ) { index, song ->
                            val isActive = song.id == mediaMetadata?.id
                            RetroListItem(
                                index = index + 1,
                                title = song.song.title,
                                subtitle = joinByBullet(
                                    song.orderedArtists.joinToString { it.name },
                                    makeTimeString(song.song.duration * 1000L),
                                ),
                                selected = isActive,
                                onClick = null,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .combinedClickable(
                                        onClick = {
                                            if (!isGuest) {
                                                if (song.id == mediaMetadata?.id) {
                                                    playerConnection.togglePlayPause()
                                                } else {
                                                    playerConnection.playQueue(
                                                        ListQueue(
                                                            title = libraryArtist?.artist?.name ?: "Unknown Artist",
                                                            items = librarySongs.map { it.toMediaItem() },
                                                            startIndex = index,
                                                        ),
                                                    )
                                                }
                                            }
                                        },
                                        onLongClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ).animateItem(),
                                trailing = {
                                    RetroIconButton(
                                        onClick = {
                                            menuState.show {
                                                SongMenu(
                                                    originalSong = song,
                                                    navController = navController,
                                                    onDismiss = menuState::dismiss,
                                                )
                                            }
                                        },
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.more_vert),
                                            contentDescription = null,
                                        )
                                    }
                                },
                            )
                        }
                    }

                    if (libraryAlbums.isNotEmpty()) {
                        item(key = "local_albums_title") {
                            RetroSectionHeader(
                                title = stringResource(R.string.albums),
                                modifier = Modifier.animateItem(),
                                action = {
                                    RetroTextButton(
                                        text = "More",
                                        onClick = {
                                            navController.navigate("artist/${viewModel.artistId}/albums")
                                        },
                                    )
                                },
                            )
                        }

                        item(key = "local_albums_list") {
                            val filteredLibraryAlbums =
                                if (hideExplicit) {
                                    libraryAlbums.filter { !it.album.explicit }
                                } else {
                                    libraryAlbums
                                }
                            LazyRow(
                                contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                            ) {
                                items(
                                    items = filteredLibraryAlbums,
                                    key = { "local_album_${it.id}_${filteredLibraryAlbums.indexOf(it)}" },
                                ) { album ->
                                    RetroGridItem(
                                        title = album.album.title,
                                        subtitle = album.album.year?.toString(),
                                        onClick = { navController.navigate("album/${album.id}") },
                                        modifier =
                                            Modifier
                                                .size(140.dp)
                                                .combinedClickable(
                                                    onClick = {
                                                        navController.navigate("album/${album.id}")
                                                    },
                                                    onLongClick = {
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                        menuState.show {
                                                            AlbumMenu(
                                                                originalAlbum = album,
                                                                navController = navController,
                                                                onDismiss = menuState::dismiss,
                                                            )
                                                        }
                                                    },
                                                ).animateItem(),
                                    ) {
                                        AsyncImage(
                                            model = album.album.thumbnailUrl?.resize(400, 400),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize(),
                                        )
                                    }
                                }
                            }
                        }
                    }
                } else {
                    artistPage?.sections?.fastForEach { section ->
                        if (section.items.isNotEmpty()) {
                            item(key = "section_${section.title}") {
                                RetroSectionHeader(
                                    title = section.title,
                                    modifier = Modifier.animateItem(),
                                    action =
                                        section.moreEndpoint?.let {
                                            {
                                                RetroTextButton(
                                                    text = "More",
                                                    onClick = {
                                                        navController.navigate(
                                                            "artist/${viewModel.artistId}/items?browseId=${it.browseId}?params=${it.params}",
                                                        )
                                                    },
                                                )
                                            }
                                        },
                                )
                            }
                        }

                        if ((section.items.firstOrNull() as? SongItem)?.album != null) {
                            items(
                                items = section.items.distinctBy { it.id },
                                key = { "youtube_song_${it.id}" },
                            ) { item ->
                                val song = item as SongItem
                                val isActive = mediaMetadata?.id == song.id
                                RetroListItem(
                                    title = song.title,
                                    subtitle = joinByBullet(
                                        song.artists.joinToString { it.name },
                                        makeTimeString(song.duration?.times(1000L)),
                                    ),
                                    selected = isActive,
                                    onClick = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .combinedClickable(
                                            onClick = {
                                                if (!isGuest) {
                                                    if (song.id == mediaMetadata?.id) {
                                                        playerConnection.togglePlayPause()
                                                    } else {
                                                        playerConnection.playQueue(
                                                            YouTubeQueue(
                                                                WatchEndpoint(videoId = song.id),
                                                                song.toMediaMetadata(),
                                                            ),
                                                        )
                                                    }
                                                }
                                            },
                                            onLongClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                menuState.show {
                                                    YouTubeSongMenu(
                                                        song = song,
                                                        navController = navController,
                                                        onDismiss = menuState::dismiss,
                                                    )
                                                }
                                            },
                                        ).animateItem(),
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
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.more_vert),
                                                contentDescription = null,
                                            )
                                        }
                                    },
                                )
                            }
                        } else {
                            item(key = "section_list_${section.title}") {
                                LazyRow(
                                    contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Horizontal).asPaddingValues(),
                                ) {
                                    items(
                                        items = section.items.distinctBy { it.id },
                                        key = { "youtube_album_${it.id}" },
                                    ) { item ->
                                        RetroGridItem(
                                            title = item.title,
                                            subtitle =
                                                when (item) {
                                                    is SongItem -> item.artists.joinToString { it.name }
                                                    is AlbumItem -> item.year?.toString()
                                                    is ArtistItem -> null
                                                    is PlaylistItem -> item.author?.name
                                                    is PodcastItem -> item.author?.name
                                                    is EpisodeItem -> item.author?.name
                                                    else -> null
                                                },
                                            onClick = {
                                                when (item) {
                                                    is SongItem -> {
                                                        if (!isGuest) {
                                                            playerConnection.playQueue(
                                                                YouTubeQueue(
                                                                    WatchEndpoint(videoId = item.id),
                                                                    item.toMediaMetadata(),
                                                                ),
                                                            )
                                                        }
                                                    }

                                                    is AlbumItem -> {
                                                        navController.navigate("album/${item.id}")
                                                    }

                                                    is ArtistItem -> {
                                                        navController.navigate("artist/${item.id}")
                                                    }

                                                    is PlaylistItem -> {
                                                        navController.navigate("online_playlist/${item.id}")
                                                    }

                                                    is PodcastItem -> {
                                                        navController.navigate("online_podcast/${item.id}")
                                                    }

                                                    is EpisodeItem -> {
                                                        if (!isGuest) {
                                                            playerConnection.playQueue(
                                                                YouTubeQueue(
                                                                    WatchEndpoint(videoId = item.id),
                                                                    item.toMediaMetadata(),
                                                                ),
                                                            )
                                                        }
                                                    }
                                                }
                                            },
                                            modifier =
                                                Modifier
                                                    .size(140.dp)
                                                    .combinedClickable(
                                                        onClick = {
                                                            when (item) {
                                                                is SongItem -> {
                                                                    if (!isGuest) {
                                                                        playerConnection.playQueue(
                                                                            YouTubeQueue(
                                                                                WatchEndpoint(videoId = item.id),
                                                                                item.toMediaMetadata(),
                                                                            ),
                                                                        )
                                                                    }
                                                                }

                                                                is AlbumItem -> {
                                                                    navController.navigate("album/${item.id}")
                                                                }

                                                                is ArtistItem -> {
                                                                    navController.navigate("artist/${item.id}")
                                                                }

                                                                is PlaylistItem -> {
                                                                    navController.navigate("online_playlist/${item.id}")
                                                                }

                                                                is PodcastItem -> {
                                                                    navController.navigate("online_podcast/${item.id}")
                                                                }

                                                                is EpisodeItem -> {
                                                                    if (!isGuest) {
                                                                        playerConnection.playQueue(
                                                                            YouTubeQueue(
                                                                                WatchEndpoint(videoId = item.id),
                                                                                item.toMediaMetadata(),
                                                                            ),
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        },
                                                        onLongClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            menuState.show {
                                                                when (item) {
                                                                    is SongItem -> {
                                                                        YouTubeSongMenu(
                                                                            song = item,
                                                                            navController = navController,
                                                                            onDismiss = menuState::dismiss,
                                                                        )
                                                                    }

                                                                    is AlbumItem -> {
                                                                        YouTubeAlbumMenu(
                                                                            albumItem = item,
                                                                            navController = navController,
                                                                            onDismiss = menuState::dismiss,
                                                                        )
                                                                    }

                                                                    is ArtistItem -> {
                                                                        YouTubeArtistMenu(
                                                                            artist = item,
                                                                            onDismiss = menuState::dismiss,
                                                                        )
                                                                    }

                                                                    is PlaylistItem -> {
                                                                        YouTubePlaylistMenu(
                                                                            playlist = item,
                                                                            coroutineScope = coroutineScope,
                                                                            onDismiss = menuState::dismiss,
                                                                        )
                                                                    }

                                                                    is PodcastItem -> {
                                                                        YouTubePlaylistMenu(
                                                                            playlist = item.asPlaylistItem(),
                                                                            coroutineScope = coroutineScope,
                                                                            onDismiss = menuState::dismiss,
                                                                        )
                                                                    }

                                                                    is EpisodeItem -> {
                                                                        YouTubeSongMenu(
                                                                            song = item.asSongItem(),
                                                                            navController = navController,
                                                                            onDismiss = menuState::dismiss,
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                        },
                                                    ).animateItem(),
                                        ) {
                                            AsyncImage(
                                                model =
                                                    when (item) {
                                                        is SongItem -> item.thumbnail?.resize(400, 400)
                                                        is AlbumItem -> item.thumbnail?.resize(400, 400)
                                                        is ArtistItem -> item.thumbnail?.resize(400, 400)
                                                        is PlaylistItem -> item.thumbnail?.resize(400, 400)
                                                        is PodcastItem -> item.thumbnail?.resize(400, 400)
                                                        is EpisodeItem -> item.thumbnail?.resize(400, 400)
                                                        else -> null
                                                    },
                                                contentDescription = null,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier =
                Modifier
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues())
                    .align(Alignment.BottomCenter),
        )
    }

    TopAppBar(
        title = { Text(artistPage?.artist?.title.orEmpty()) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painterResource(R.drawable.arrow_back),
                    contentDescription = null,
                )
            }
        },
        actions = {
            RetroIconButton(
                onClick = {
                    viewModel.artistPage?.artist?.shareLink?.let { link ->
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        val clip = ClipData.newPlainText("Artist Link", link)
                        clipboard.setPrimaryClip(clip)
                        Toast.makeText(context, R.string.link_copied, Toast.LENGTH_SHORT).show()
                    }
                },
            ) {
                Icon(
                    painterResource(R.drawable.link),
                    contentDescription = null,
                )
            }
        },
    )
}

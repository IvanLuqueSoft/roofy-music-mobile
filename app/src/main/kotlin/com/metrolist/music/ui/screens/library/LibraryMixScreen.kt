/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.library

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.AlbumViewTypeKey
import com.metrolist.music.constants.CONTENT_TYPE_HEADER
import com.metrolist.music.constants.CONTENT_TYPE_PLAYLIST
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.LibraryViewType
import com.metrolist.music.constants.MixSortDescendingKey
import com.metrolist.music.constants.MixSortType
import com.metrolist.music.constants.MixSortTypeKey
import com.metrolist.music.constants.PersonalLibraryPasswordKey
import com.metrolist.music.constants.PersonalLibraryServerUrlKey
import com.metrolist.music.constants.PersonalLibraryUsernameKey
import com.metrolist.music.constants.ShowCachedPlaylistKey
import com.metrolist.music.constants.ShowDownloadedPlaylistKey
import com.metrolist.music.constants.ShowLikedPlaylistKey
import com.metrolist.music.constants.ShowTopPlaylistKey
import com.metrolist.music.constants.ShowUploadedPlaylistKey
import com.metrolist.music.constants.YtmSyncKey
import com.metrolist.music.device.DeviceSessionManager
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.matchesNormalizedQuery
import com.metrolist.music.extensions.normalizeForSearch
import com.metrolist.music.extensions.reversed
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.LocalAlbumRadio
import com.metrolist.music.subsonic.PersonalLibraryImportService
import com.metrolist.music.ui.component.AlbumListItem
import com.metrolist.music.ui.component.ArtistListItem
import com.metrolist.music.ui.component.CreatePlaylistDialog
import com.metrolist.music.ui.component.LibrarySearchEmptyPlaceholder
import com.metrolist.music.ui.component.LibrarySearchHeader
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PlaylistListItem
import com.metrolist.music.ui.component.SongListItem
import com.metrolist.music.ui.component.SortHeader
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.component.MenuState
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.PlaylistMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.theme.RetroButton
import com.metrolist.music.ui.theme.RetroIconButton
import com.metrolist.music.ui.theme.RetroTextButton
import com.metrolist.music.ui.theme.RetroTokens
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.LibraryMixViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.Collator
import java.time.LocalDateTime
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LibraryMixScreen(
    navController: NavController,
    filterContent: @Composable () -> Unit,
    viewModel: LibraryMixViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val queueSearchedSongsStr = stringResource(R.string.queue_searched_songs)
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    var viewType by rememberEnumPreference(AlbumViewTypeKey, LibraryViewType.GRID)
    val (sortType, onSortTypeChange) =
        rememberEnumPreference(
            MixSortTypeKey,
            MixSortType.CREATE_DATE,
        )
    val (sortDescending, onSortDescendingChange) = rememberPreference(MixSortDescendingKey, true)
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)

    val (ytmSync) = rememberPreference(YtmSyncKey, true)

    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val debouncedSearchQuery by viewModel.debouncedSearchQuery.collectAsStateWithLifecycle()
    var showCreatePlaylistDialog by rememberSaveable { mutableStateOf(false) }
    
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onPlaylistCreated = { playlistId ->
                showCreatePlaylistDialog = false
                navController.navigate("local_playlist/$playlistId")
            }
        )
    }
    
    val normalizedQuery = remember(isSearchActive, searchQuery, debouncedSearchQuery) {
        if (isSearchActive) {
            searchQuery.normalizeForSearch()
        } else {
            debouncedSearchQuery.normalizeForSearch()
        }
    }

    val topSize by viewModel.topValue.collectAsStateWithLifecycle(initialValue = 50)
    val likedPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.liked),
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val downloadPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.offline),
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val topPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.my_top) + " $topSize",
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val cachedPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.cached_playlist),
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val uploadedPlaylist =
        Playlist(
            playlist =
                PlaylistEntity(
                    id = UUID.randomUUID().toString(),
                    name = stringResource(R.string.uploaded_playlist),
                ),
            songCount = 0,
            songThumbnails = emptyList(),
        )

    val (showLiked) = rememberPreference(ShowLikedPlaylistKey, true)
    val (showDownloaded) = rememberPreference(ShowDownloadedPlaylistKey, true)
    val (showTop) = rememberPreference(ShowTopPlaylistKey, true)
    val (showCached) = rememberPreference(ShowCachedPlaylistKey, false)
    val (showUploaded) = rememberPreference(ShowUploadedPlaylistKey, true)
    
    val showLikedPlaylist = showLiked && matchesNormalizedQuery(normalizedQuery, likedPlaylist.playlist.name)
    val showDownloadedPlaylist =
        showDownloaded && matchesNormalizedQuery(normalizedQuery, downloadPlaylist.playlist.name)
    val showTopPlaylists = showTop && matchesNormalizedQuery(normalizedQuery, topPlaylist.playlist.name)
    val showUploadedPlaylists =
        showUploaded && matchesNormalizedQuery(normalizedQuery, uploadedPlaylist.playlist.name)
    val showCachedPlaylists = showCached && matchesNormalizedQuery(normalizedQuery, cachedPlaylist.playlist.name)


    val albums = viewModel.albums.collectAsStateWithLifecycle()
    val artist = viewModel.artists.collectAsStateWithLifecycle()
    val songs = viewModel.songs.collectAsStateWithLifecycle()
    val playlist = viewModel.playlists.collectAsStateWithLifecycle()

    var allItems = albums.value + artist.value + playlist.value
    val locale = LocalLocale.current.platformLocale
    val collator = remember(locale) {
        Collator.getInstance(locale).apply {
            strength = Collator.PRIMARY
        }
    }
    allItems =
        when (sortType) {
            MixSortType.CREATE_DATE -> {
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.bookmarkedAt
                        is Artist -> item.artist.bookmarkedAt
                        is Playlist -> item.playlist.createdAt
                        else -> LocalDateTime.now()
                    }
                }
            }

            MixSortType.NAME -> {
                allItems.sortedWith(
                    compareBy(collator) { item ->
                        when (item) {
                            is Album -> item.album.title
                            is Artist -> item.artist.name
                            is Playlist -> item.playlist.name
                            else -> ""
                        }
                    },
                )
            }

            MixSortType.LAST_UPDATED -> {
                allItems.sortedBy { item ->
                    when (item) {
                        is Album -> item.album.lastUpdateTime
                        is Artist -> item.artist.lastUpdateTime
                        is Playlist -> item.playlist.lastUpdateTime
                        else -> LocalDateTime.now()
                    }
                }
            }
        }.reversed(sortDescending)

    val searchableItems = if (normalizedQuery.isBlank()) allItems else allItems + songs.value

    val filteredItems = remember(searchableItems, normalizedQuery, collator) {
        val matchedItems =
            searchableItems.filter { item ->
                when (item) {
                    is Song -> {
                        val artistNames = item.orderedArtists.map { it.name }.toTypedArray()
                        matchesNormalizedQuery(normalizedQuery, item.song.title, item.song.albumName, *artistNames)
                    }

                    is Album -> {
                        val artistNames = item.artists.map { it.name }.toTypedArray()
                        matchesNormalizedQuery(normalizedQuery, item.album.title, *artistNames)
                    }

                    is Artist -> matchesNormalizedQuery(normalizedQuery, item.artist.name)
                    is Playlist -> matchesNormalizedQuery(normalizedQuery, item.playlist.name)
                    else -> true
                }
            }

        if (normalizedQuery.isBlank()) {
            matchedItems.distinctBy { it.id }
        } else {
            matchedItems
                .sortedWith { first, second ->
                    val firstPriority =
                        when (first) {
                            is Playlist -> 0
                            is Song -> 1
                            is Artist -> 2
                            is Album -> 3
                            else -> 4
                        }
                    val secondPriority =
                        when (second) {
                            is Playlist -> 0
                            is Song -> 1
                            is Artist -> 2
                            is Album -> 3
                            else -> 4
                        }

                    if (firstPriority != secondPriority) {
                        firstPriority.compareTo(secondPriority)
                    } else {
                        val firstName =
                            when (first) {
                                is Playlist -> first.playlist.name
                                is Song -> first.song.title
                                is Artist -> first.artist.name
                                is Album -> first.album.title
                                else -> ""
                            }
                        val secondName =
                            when (second) {
                                is Playlist -> second.playlist.name
                                is Song -> second.song.title
                                is Artist -> second.artist.name
                                is Album -> second.album.title
                                else -> ""
                            }
                        collator.compare(firstName, secondName)
                    }
                }
                .distinctBy { it.id }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()
    val lazyGridState = rememberLazyGridState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsStateWithLifecycle()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            when (viewType) {
                LibraryViewType.LIST -> lazyListState.animateScrollToItem(0)
                LibraryViewType.GRID -> lazyGridState.animateScrollToItem(0)
            }
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        if (ytmSync) {
            withContext(Dispatchers.IO) {
                viewModel.syncAllLibrary()
            }
        }
    }

    val headerContent = @Composable {
        LibrarySearchHeader(
            isSearchActive = isSearchActive,
            searchQuery = searchQuery,
            onSearchQueryChange = viewModel::updateSearchQuery,
            onBack = {
                isSearchActive = false
                viewModel.updateSearchQuery("")
            },
            keyboardController = keyboardController,
            modifier = Modifier.padding(start = 16.dp),
        ) {
            SortHeader(
                sortType = sortType,
                sortDescending = sortDescending,
                onSortTypeChange = onSortTypeChange,
                onSortDescendingChange = onSortDescendingChange,
                sortTypeText = { sortType ->
                    when (sortType) {
                        MixSortType.CREATE_DATE -> R.string.sort_by_create_date
                        MixSortType.LAST_UPDATED -> R.string.sort_by_last_updated
                        MixSortType.NAME -> R.string.sort_by_name
                    }
                },
            )

            Spacer(Modifier.weight(1f))

            IconButton(
                onClick = { isSearchActive = true },
                modifier = Modifier.padding(start = 8.dp).size(40.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.search),
                    contentDescription = stringResource(R.string.search),
                )
            }

            IconButton(
                onClick = {
                    viewType = viewType.toggle()
                },
                modifier = Modifier.padding(end = 8.dp).size(40.dp),
            ) {
                Icon(
                    painter =
                    painterResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.drawable.list
                            LibraryViewType.GRID -> R.drawable.grid_view
                        },
                    ),
                    contentDescription = stringResource(
                        when (viewType) {
                            LibraryViewType.LIST -> R.string.switch_to_grid_view
                            LibraryViewType.GRID -> R.string.switch_to_list_view
                        },
                    ),
                )
            }
        }
    }

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .pullToRefresh(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing,
                    onRefresh = viewModel::refresh,
                ),
    ) {
        when (viewType) {
            LibraryViewType.LIST -> {
                LazyColumn(
                    state = lazyListState,
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    if (showLikedPlaylist) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = likedPlaylist,
                                autoPlaylist = true,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("auto_playlist/liked")
                                        }.animateItem(),
                            )
                        }
                    }

                    if (showDownloadedPlaylist) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = downloadPlaylist,
                                autoPlaylist = true,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("auto_playlist/downloaded")
                                        }
                                        .animateItem(),
                            )
                        }
                    }

                    if (showCachedPlaylists) {
                        item(
                            key = "cachedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = cachedPlaylist,
                                autoPlaylist = true,
                                modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        navController.navigate("cache_playlist/cached")
                                    }
                                    .animateItem(),
                            )
                        }
                    }

                    if (showTopPlaylists) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = topPlaylist,
                                autoPlaylist = true,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("top_playlist/$topSize")
                                        }.animateItem(),
                            )
                        }
                    }

                    if (showUploadedPlaylists) {
                        item(
                            key = "uploadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            PlaylistListItem(
                                playlist = uploadedPlaylist,
                                autoPlaylist = true,
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("auto_playlist/uploaded")
                                        }.animateItem(),
                            )
                        }
                    }

                    items(
                        items = filteredItems,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                PlaylistListItem(
                                    playlist = item,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    PlaylistMenu(
                                                        playlist = item,
                                                        coroutineScope = coroutineScope,
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
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (!item.playlist.isEditable && item.songCount == 0 &&
                                                        item.playlist.browseId != null
                                                    ) {
                                                        navController.navigate("online_playlist/${item.playlist.browseId}")
                                                    } else {
                                                        navController.navigate("local_playlist/${item.id}")
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        PlaylistMenu(
                                                            playlist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItem(),
                                )
                            }

                            is Song -> {
                                SongListItem(
                                    song = item,
                                    showInLibraryIcon = true,
                                    isActive = item.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    SongMenu(
                                                        originalSong = item,
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
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    if (item.id == mediaMetadata?.id) {
                                                        playerConnection.togglePlayPause()
                                                    } else {
                                                        val filteredSongs = filteredItems.filterIsInstance<Song>()
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = queueSearchedSongsStr,
                                                                items = filteredSongs.map { it.toMediaItem() },
                                                                startIndex = filteredSongs.indexOfFirst { it.id == item.id },
                                                            ),
                                                        )
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            )
                                            .animateItem(),
                                )
                            }

                            is Artist -> {
                                ArtistListItem(
                                    artist = item,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    ArtistMenu(
                                                        originalArtist = item,
                                                        coroutineScope = coroutineScope,
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
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("artist/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        ArtistMenu(
                                                            originalArtist = item,
                                                            coroutineScope = coroutineScope,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItem(),
                                )
                            }

                            is Album -> {
                                AlbumListItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    trailingContent = {
                                        IconButton(
                                            onClick = {
                                                menuState.show {
                                                    AlbumMenu(
                                                        originalAlbum = item,
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
                                    modifier =
                                        Modifier
                                            .fillMaxWidth()
                                            .combinedClickable(
                                                onClick = {
                                                    navController.navigate("album/${item.id}")
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        AlbumMenu(
                                                            originalAlbum = item,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                            ).animateItem(),
                                )
                            }

                            else -> {}
                        }
                    }

                    if (
                        filteredItems.isEmpty() &&
                        !showLikedPlaylist &&
                        !showDownloadedPlaylist &&
                        !showCachedPlaylists &&
                        !showTopPlaylists &&
                        !showUploadedPlaylists &&
                        searchQuery.isNotBlank()
                    ) {
                        item(key = "empty_search_result") {
                            LibrarySearchEmptyPlaceholder(modifier = Modifier.animateItem())
                        }
                    }
                }
            }

            LibraryViewType.GRID -> {
                LazyVerticalGrid(
                    state = lazyGridState,
                    columns =
                        GridCells.Adaptive(
                            minSize = if (gridItemSize == GridItemSize.BIG) 104.dp else 80.dp,
                        ),
                    contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
                ) {
                    item(
                        key = "filter",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        filterContent()
                    }

                    item(
                        key = "header",
                        span = { GridItemSpan(maxLineSpan) },
                        contentType = CONTENT_TYPE_HEADER,
                    ) {
                        headerContent()
                    }

                    if (showLikedPlaylist) {
                        item(
                            key = "likedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            RetroPlaylistGridItem(
                                playlist = likedPlaylist,
                                autoPlaylist = true,
                                route = "auto_playlist/liked",
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (showDownloadedPlaylist) {
                        item(
                            key = "downloadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            RetroPlaylistGridItem(
                                playlist = downloadPlaylist,
                                autoPlaylist = true,
                                route = "auto_playlist/downloaded",
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (showCachedPlaylists) {
                        item(
                            key = "cachedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            RetroPlaylistGridItem(
                                playlist = cachedPlaylist,
                                autoPlaylist = true,
                                route = "cache_playlist/cached",
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (showTopPlaylists) {
                        item(
                            key = "TopPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            RetroPlaylistGridItem(
                                playlist = topPlaylist,
                                autoPlaylist = true,
                                route = "top_playlist/$topSize",
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    if (showUploadedPlaylists) {
                        item(
                            key = "uploadedPlaylist",
                            contentType = { CONTENT_TYPE_PLAYLIST },
                        ) {
                            RetroPlaylistGridItem(
                                playlist = uploadedPlaylist,
                                autoPlaylist = true,
                                route = "auto_playlist/uploaded",
                                navController = navController,
                                menuState = menuState,
                                coroutineScope = coroutineScope,
                                modifier = Modifier.animateItem(),
                            )
                        }
                    }

                    items(
                        items = filteredItems,
                        key = { it.id },
                        contentType = { CONTENT_TYPE_PLAYLIST },
                    ) { item ->
                        when (item) {
                            is Playlist -> {
                                RetroPlaylistGridItem(
                                    playlist = item,
                                    autoPlaylist = false,
                                    navController = navController,
                                    menuState = menuState,
                                    coroutineScope = coroutineScope,
                                    modifier = Modifier.animateItem(),
                                )
                            }

                            is Song -> {
                                RetroMixSongGridItem(
                                    song = item,
                                    isActive = item.id == mediaMetadata?.id,
                                    isPlaying = isPlaying,
                                    filteredItems = filteredItems,
                                    playerConnection = playerConnection,
                                    navController = navController,
                                    menuState = menuState,
                                    haptic = haptic,
                                    modifier = Modifier.animateItem(),
                                )
                            }

                            is Artist -> {
                                RetroMixArtistGridItem(
                                    artist = item,
                                    navController = navController,
                                    menuState = menuState,
                                    coroutineScope = coroutineScope,
                                    haptic = haptic,
                                    modifier = Modifier.animateItem(),
                                )
                            }

                            is Album -> {
                                RetroMixAlbumGridItem(
                                    album = item,
                                    isActive = item.id == mediaMetadata?.album?.id,
                                    isPlaying = isPlaying,
                                    navController = navController,
                                    menuState = menuState,
                                    coroutineScope = coroutineScope,
                                    haptic = haptic,
                                    modifier = Modifier.animateItem(),
                                )
                            }

                            else -> {}
                        }
                    }

                    if (
                        filteredItems.isEmpty() &&
                        !showLikedPlaylist &&
                        !showDownloadedPlaylist &&
                        !showCachedPlaylists &&
                        !showTopPlaylists &&
                        !showUploadedPlaylists &&
                        searchQuery.isNotBlank()
                    ) {
                        item(
                            key = "empty_search_result",
                            span = { GridItemSpan(maxLineSpan) },
                        ) {
                            LibrarySearchEmptyPlaceholder(modifier = Modifier.animateItem())
                        }
                    }
                }
            }
        }

        RetroIconButton(
            onClick = {
                menuState.show {
                    LibraryAddSheet(
                        navController = navController,
                        onCreatePlaylist = {
                            menuState.dismiss()
                            showCreatePlaylistDialog = true
                        },
                        onDismiss = menuState::dismiss,
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current
                        .only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal)
                )
                .padding(16.dp)
                .size(48.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.add),
                contentDescription = stringResource(R.string.library_actions_content_desc),
            )
        }

        Indicator(
            isRefreshing = isRefreshing,
            state = pullRefreshState,
            modifier =
                Modifier
                    .align(Alignment.TopCenter)
                    .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
        )
    }
}

@Composable
private fun LibraryAddSheet(
    navController: NavController,
    onCreatePlaylist: () -> Unit,
    onDismiss: () -> Unit,
) {
    val menuState = LocalMenuState.current

    Text(
        text = stringResource(R.string.library_actions_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = RetroTokens.Text,
    )
    Spacer(Modifier.height(12.dp))

    LibraryActionRow(
        iconRes = R.drawable.library_add,
        title = stringResource(R.string.library_action_import_computer),
        subtitle = stringResource(R.string.library_action_import_computer_desc),
        onClick = {
            menuState.show {
                LibraryComputerImportSheet(
                    navController = navController,
                    onDismiss = onDismiss,
                )
            }
        },
    )

    HorizontalDivider(
        color = RetroTokens.BorderMuted,
        modifier = Modifier.padding(vertical = 8.dp),
    )

    LibraryActionRow(
        iconRes = R.drawable.playlist_add,
        title = stringResource(R.string.create_playlist),
        subtitle = stringResource(R.string.library_actions_create_playlist_desc),
        onClick = onCreatePlaylist,
    )

    Spacer(Modifier.height(18.dp))
}

@Composable
private fun LibraryComputerImportSheet(
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val sessionUi by DeviceSessionManager.uiState.collectAsStateWithLifecycle()

    val serverUrl by rememberPreference(PersonalLibraryServerUrlKey, "")
    val username by rememberPreference(PersonalLibraryUsernameKey, "")
    val password by rememberPreference(PersonalLibraryPasswordKey, "")

    val canImport = serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank()
    val computerName = sessionUi.computerName.ifBlank { stringResource(R.string.listen_on_this_computer) }
    val importStarted = stringResource(R.string.library_import_started)

    Text(
        text = stringResource(R.string.library_import_title),
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = RetroTokens.Text,
    )
    Spacer(Modifier.height(8.dp))
    Text(
        text =
            if (canImport) {
                stringResource(R.string.library_import_ready_body)
            } else {
                stringResource(R.string.library_import_connect_body)
            },
        style = MaterialTheme.typography.bodyMedium,
        color = RetroTokens.TextMuted,
    )

    Spacer(Modifier.height(18.dp))

    LibraryImportStepRow(
        iconRes = if (canImport) R.drawable.check else R.drawable.radio_button_checked,
        title = stringResource(R.string.library_import_step_connect),
        subtitle =
            if (canImport) {
                stringResource(R.string.library_import_connected_to, computerName)
            } else {
                stringResource(R.string.library_import_connect_title)
            },
        isActive = !canImport,
    )

    LibraryImportStepRow(
        iconRes =
            when {
                canImport -> R.drawable.radio_button_checked
                else -> R.drawable.radio_button_unchecked
            },
        title = stringResource(R.string.library_import_step_import),
        subtitle =
            if (canImport) {
                stringResource(R.string.library_import_background_body)
            } else {
                stringResource(R.string.library_action_import_computer_desc)
            },
        isActive = canImport,
    )

    Spacer(Modifier.height(18.dp))

    if (canImport) {
        RetroButton(
            onClick = {
                PersonalLibraryImportService.start(context.applicationContext)
                Toast.makeText(context, importStarted, Toast.LENGTH_SHORT).show()
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.library_import_start).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = RetroTokens.Text,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(10.dp))
        RetroTextButton(
            text = stringResource(R.string.library_import_new_code),
            onClick = {
                navController.navigate("link_computer?scan=true") {
                    launchSingleTop = true
                }
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        RetroButton(
            onClick = {
                navController.navigate("link_computer?scan=true") {
                    launchSingleTop = true
                }
                onDismiss()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = stringResource(R.string.library_import_scan_code).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = RetroTokens.Text,
                maxLines = 1,
            )
        }
    }

    Spacer(Modifier.height(18.dp))
}

@Composable
private fun LibraryActionRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = RetroTokens.Text,
            modifier = Modifier.size(24.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = RetroTokens.Text,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = RetroTokens.TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun LibraryImportStepRow(
    iconRes: Int,
    title: String,
    subtitle: String,
    isActive: Boolean,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (isActive) RetroTokens.Active else RetroTokens.TextMuted,
            modifier = Modifier.size(22.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                color = if (isActive) RetroTokens.Text else RetroTokens.TextMuted,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = RetroTokens.TextMuted,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RetroMixPlaylistGridItem(
    playlist: Playlist,
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .background(RetroTokens.Panel)
            .border(1.dp, RetroTokens.Border)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (!playlist.playlist.isEditable && playlist.songCount == 0 && playlist.playlist.browseId != null) {
                        navController.navigate("online_playlist/${playlist.playlist.browseId}")
                    } else {
                        navController.navigate("local_playlist/${playlist.id}")
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        PlaylistMenu(
                            playlist = playlist,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(RetroTokens.Background)
                .border(1.dp, RetroTokens.BorderMuted),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(playlist.thumbnails.firstOrNull())
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = playlist.playlist.name,
            style = MaterialTheme.typography.labelSmall,
            color = RetroTokens.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RetroMixSongGridItem(
    song: Song,
    isActive: Boolean,
    isPlaying: Boolean,
    filteredItems: List<Any>,
    playerConnection: com.metrolist.music.playback.PlayerConnection,
    navController: NavController,
    menuState: MenuState,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .background(if (isActive) RetroTokens.Panel2 else RetroTokens.Panel)
            .border(1.dp, if (isActive) RetroTokens.ActiveMuted else RetroTokens.Border)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (isActive) {
                        playerConnection.togglePlayPause()
                    } else {
                        val filteredSongs = filteredItems.filterIsInstance<Song>()
                        playerConnection.playQueue(
                            ListQueue(
                                title = "",
                                items = filteredSongs.map { it.toMediaItem() },
                                startIndex = filteredSongs.indexOfFirst { it.id == song.id },
                            ),
                        )
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
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(RetroTokens.Background)
                .border(1.dp, RetroTokens.BorderMuted),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(song.song.thumbnailUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = song.song.title,
            style = MaterialTheme.typography.labelSmall,
            color = RetroTokens.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = song.orderedArtists.joinToString { it.name },
            style = MaterialTheme.typography.labelSmall,
            color = RetroTokens.TextSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RetroMixArtistGridItem(
    artist: Artist,
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .background(RetroTokens.Panel)
            .border(1.dp, RetroTokens.Border)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    navController.navigate("artist/${artist.id}")
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        ArtistMenu(
                            originalArtist = artist,
                            coroutineScope = coroutineScope,
                            onDismiss = menuState::dismiss,
                        )
                    }
                },
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(RetroTokens.Background)
                .border(1.dp, RetroTokens.BorderMuted),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(artist.artist.thumbnailUrl?.resize(544, 544))
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = artist.artist.name,
            style = MaterialTheme.typography.labelSmall,
            color = RetroTokens.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        if (artist.songCount > 0) {
            Text(
                text = pluralStringResource(R.plurals.n_song, artist.songCount, artist.songCount),
                style = MaterialTheme.typography.labelSmall,
                color = RetroTokens.TextSoft,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RetroMixAlbumGridItem(
    album: Album,
    isActive: Boolean,
    isPlaying: Boolean,
    navController: NavController,
    menuState: MenuState,
    coroutineScope: CoroutineScope,
    haptic: androidx.compose.ui.hapticfeedback.HapticFeedback,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    val db = LocalDatabase.current
    val player = LocalPlayerConnection.current
    Column(
        modifier = modifier
            .background(if (isActive) RetroTokens.Panel2 else RetroTokens.Panel)
            .border(1.dp, if (isActive) RetroTokens.ActiveMuted else RetroTokens.Border)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
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
            )
            .padding(4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .background(RetroTokens.Background)
                .border(1.dp, RetroTokens.BorderMuted),
            contentAlignment = Alignment.Center,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(album.album.thumbnailUrl)
                    .memoryCachePolicy(CachePolicy.ENABLED)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .networkCachePolicy(CachePolicy.ENABLED)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (!isActive) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp)
                        .size(28.dp)
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable {
                            if (player != null) {
                                scope.launch {
                                    val albumWithSongs = withContext(Dispatchers.IO) {
                                        db.albumWithSongs(album.id).firstOrNull()
                                    }
                                    albumWithSongs?.let {
                                        player.playQueue(LocalAlbumRadio(it))
                                    }
                                }
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.play),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = album.album.title,
            style = MaterialTheme.typography.labelSmall,
            color = RetroTokens.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = album.artists.joinToString { it.name },
            style = MaterialTheme.typography.labelSmall,
            color = RetroTokens.TextSoft,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

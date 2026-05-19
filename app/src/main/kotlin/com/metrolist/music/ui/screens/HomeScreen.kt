/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import coil3.compose.AsyncImage
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.models.YTItem
import com.metrolist.innertube.utils.completed
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.GridItemSize
import com.metrolist.music.constants.GridItemsSizeKey
import com.metrolist.music.constants.GridThumbnailHeight
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.ListItemHeight
import com.metrolist.music.constants.ListThumbnailSize
import com.metrolist.music.constants.RandomizeHomeOrderKey
import com.metrolist.music.constants.SmallGridThumbnailHeight
import com.metrolist.music.constants.ThumbnailCornerRadius
import com.metrolist.music.db.entities.Album
import com.metrolist.music.db.entities.Artist
import com.metrolist.music.db.entities.LocalItem
import com.metrolist.music.db.entities.Playlist
import com.metrolist.music.db.entities.PlaylistEntity
import com.metrolist.music.db.entities.PlaylistSongMap
import com.metrolist.music.db.entities.Song
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.LocalAlbumRadio
import com.metrolist.music.playback.queues.YouTubeAlbumRadio
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.ChipsRow
import com.metrolist.music.ui.component.LocalBottomSheetPageState
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.RandomizeGridItem
import com.metrolist.music.ui.component.SpeedDialGridItem
import com.metrolist.music.ui.component.shimmer.GridItemPlaceHolder
import com.metrolist.music.ui.component.shimmer.ShimmerHost
import com.metrolist.music.ui.component.shimmer.TextPlaceholder
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
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
import com.metrolist.music.ui.theme.retroBorder
import com.metrolist.music.ui.theme.RetroTokens
import com.metrolist.music.ui.utils.SnapLayoutInfoProvider
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberEnumPreference
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.CommunityPlaylistItem
import com.metrolist.music.viewmodels.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.min
import kotlin.random.Random

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeListItem(
    title: String,
    subtitle: String?,
    thumbnail: String?,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(ListItemHeight)
            .background(if (isActive) RetroTokens.Panel2 else Color.Transparent)
            .border(1.dp, if (isActive) RetroTokens.ActiveMuted else RetroTokens.BorderDark, RoundedCornerShape(0.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        RetroArtwork(modifier = Modifier.size(48.dp)) {
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) RetroTokens.TextHot else RetroTokens.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = RetroTokens.TextSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HomeGridRow(
    title: String,
    subtitle: String?,
    thumbnail: String?,
    isActive: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(64.dp)
            .background(if (isActive) RetroTokens.Panel2 else Color.Transparent)
            .border(1.dp, if (isActive) RetroTokens.ActiveMuted else RetroTokens.BorderDark, RoundedCornerShape(0.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    ) {
        RetroArtwork(modifier = Modifier.size(48.dp)) {
            AsyncImage(
                model = thumbnail,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isActive) RetroTokens.TextHot else RetroTokens.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (!subtitle.isNullOrEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = RetroTokens.TextSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

sealed class HomeSection(
    val id: String,
    val baseWeight: Int,
) {
    data object SpeedDial : HomeSection("speed_dial", 100)

    data object QuickPicks : HomeSection("quick_picks", 90)

    data object DailyDiscover : HomeSection("daily_discover", 80)

    data object KeepListening : HomeSection("keep_listening", 50)

    data object AccountPlaylists : HomeSection("account_playlists", 40)

    data object ForgottenFavorites : HomeSection("forgotten_favorites", 30)

    data object FromTheCommunity : HomeSection("from_the_community", 20)

    data class SimilarRecommendation(
        val index: Int,
    ) : HomeSection("similar_recommendation_$index", 10)

    data class HomePageSection(
        val index: Int,
    ) : HomeSection("home_page_section_$index", 10)

    data object MoodAndGenres : HomeSection("mood_and_genres", 5)
}

@Composable
fun CommunityPlaylistCard(
    item: CommunityPlaylistItem,
    onClick: () -> Unit,
    onSongClick: (SongItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val scope = rememberCoroutineScope()

    val dbPlaylist by database.playlistByBrowseId(item.playlist.id).collectAsStateWithLifecycle(initialValue = null)
    val isBookmarked = dbPlaylist?.playlist?.bookmarkedAt != null

    RetroPanel(
        modifier = modifier.width(320.dp),
        strong = true,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(96.dp)
                            .border(1.dp, RetroTokens.BorderMuted),
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model =
                                    item.songs
                                        .getOrNull(0)
                                        ?.thumbnail
                                        ?.resize(200, 200),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                            )
                            AsyncImage(
                                model =
                                    item.songs
                                        .getOrNull(1)
                                        ?.thumbnail
                                        ?.resize(200, 200),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                            )
                        }
                        Row(modifier = Modifier.weight(1f)) {
                            AsyncImage(
                                model =
                                    item.songs
                                        .getOrNull(2)
                                        ?.thumbnail
                                        ?.resize(200, 200),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                            )
                            AsyncImage(
                                model =
                                    item.songs
                                        .getOrNull(3)
                                        ?.thumbnail
                                        ?.resize(200, 200),
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier =
                                    Modifier
                                        .weight(1f)
                                        .fillMaxSize(),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = item.playlist.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = RetroTokens.Text,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.playlist.author?.name ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = RetroTokens.TextSoft,
                        maxLines = 1,
                    )
                }
            }

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
            ) {
                item.songs.take(3).forEach { song ->
                    RetroListItem(
                        title = song.title,
                        subtitle = song.artists.joinToString(", ") { it.name },
                        onClick = { onSongClick(song) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            RetroCommandBar(
                modifier = Modifier.padding(12.dp),
            ) {
                RetroIconButton(
                    onClick = {
                        if (!isListenTogetherGuest) {
                            item.playlist.playEndpoint?.let {
                                playerConnection?.playQueue(YouTubeQueue(it))
                            }
                        }
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_widget_play),
                        contentDescription = null,
                        tint = RetroTokens.Text,
                        modifier = Modifier.size(20.dp),
                    )
                }

                RetroIconButton(
                    onClick = {
                        if (!isListenTogetherGuest) {
                            item.playlist.radioEndpoint?.let {
                                playerConnection?.playQueue(YouTubeQueue(it))
                            }
                        }
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.radio),
                        contentDescription = null,
                        tint = RetroTokens.Text,
                        modifier = Modifier.size(20.dp),
                    )
                }

                RetroIconButton(
                    onClick = {
                        scope.launch(Dispatchers.IO) {
                            if (dbPlaylist?.playlist == null) {
                                val playlistEntity =
                                    PlaylistEntity(
                                        name = item.playlist.title,
                                        browseId = item.playlist.id,
                                        thumbnailUrl = item.playlist.thumbnail,
                                        remoteSongCount =
                                            item.playlist.songCountText
                                                ?.split(" ")
                                                ?.firstOrNull()
                                                ?.toIntOrNull(),
                                        playEndpointParams = item.playlist.playEndpoint?.params,
                                        shuffleEndpointParams = item.playlist.shuffleEndpoint?.params,
                                        radioEndpointParams = item.playlist.radioEndpoint?.params,
                                    ).toggleLike()
                                val songMetadata =
                                    item.songs
                                        .ifEmpty {
                                            YouTube
                                                .playlist(item.playlist.id)
                                                .completed()
                                                .getOrNull()
                                                ?.songs
                                                .orEmpty()
                                        }.map { it.toMediaMetadata() }
                                if (songMetadata.isNotEmpty()) {
                                    database.withTransaction {
                                        insert(playlistEntity)
                                        songMetadata.onEach { insert(it) }
                                        val songIds = songMetadata.map { it.id to it.setVideoId }
                                        val createdPlaylist = database.playlistBlocking(playlistEntity.id)
                                        if (createdPlaylist != null) {
                                            addSongsToPlaylist(createdPlaylist, songIds)
                                        }
                                    }
                                }
                            } else {
                                database.transaction {
                                    val currentPlaylist = dbPlaylist!!.playlist
                                    update(currentPlaylist.toggleLike())
                                }
                            }
                        }
                    },
                    modifier = Modifier.size(40.dp),
                ) {
                    Icon(
                        painter = painterResource(if (isBookmarked) R.drawable.library_add_check else R.drawable.library_add),
                        contentDescription = null,
                        tint = if (isBookmarked) RetroTokens.TextHot else RetroTokens.Text,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DailyDiscoverCard(
    dailyDiscover: com.metrolist.music.viewmodels.DailyDiscoverItem,
    onClick: () -> Unit,
    navController: NavController,
    modifier: Modifier = Modifier,
) {
    val database = LocalDatabase.current
    val playCount by database.getLifetimePlayCount(dailyDiscover.recommendation.id).collectAsStateWithLifecycle(initialValue = 0)
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current

    val song = dailyDiscover.recommendation as? SongItem
    val playsString = stringResource(R.string.plays)

    RetroGridItem(
        title = dailyDiscover.recommendation.title,
        subtitle = buildString {
            append((dailyDiscover.recommendation as? SongItem)?.artists?.joinToString(", ") { it.name } ?: "")
            if (playCount > 0) {
                append(" | $playCount $playsString")
            }
        },
        onClick = onClick,
        modifier = modifier,
    ) {
        RetroArtwork(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = dailyDiscover.recommendation.thumbnail?.resize(544, 544),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun HomeScreen(
    navController: NavController,
    snackbarHostState: SnackbarHostState,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val menuState = LocalMenuState.current
    val bottomSheetPageState = LocalBottomSheetPageState.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val haptic = LocalHapticFeedback.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false

    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()

    val quickPicks by viewModel.quickPicks.collectAsStateWithLifecycle()
    val forgottenFavorites by viewModel.forgottenFavorites.collectAsStateWithLifecycle()
    val keepListening by viewModel.keepListening.collectAsStateWithLifecycle()
    val similarRecommendations by viewModel.similarRecommendations.collectAsStateWithLifecycle()
    val accountPlaylists by viewModel.accountPlaylists.collectAsStateWithLifecycle()
    val homePage by viewModel.homePage.collectAsStateWithLifecycle()
    val explorePage by viewModel.explorePage.collectAsStateWithLifecycle()
    val dailyDiscover by viewModel.dailyDiscover.collectAsStateWithLifecycle()
    val communityPlaylists by viewModel.communityPlaylists.collectAsStateWithLifecycle()

    val allLocalItems by viewModel.allLocalItems.collectAsStateWithLifecycle()
    val allYtItems by viewModel.allYtItems.collectAsStateWithLifecycle()
    val speedDialItems by viewModel.speedDialItems.collectAsStateWithLifecycle()
    val pinnedSpeedDialItems by viewModel.pinnedSpeedDialItems.collectAsStateWithLifecycle()
    val selectedChip by viewModel.selectedChip.collectAsStateWithLifecycle()

    val savedPodcastShows by viewModel.savedPodcastShows.collectAsStateWithLifecycle()
    val episodesForLater by viewModel.episodesForLater.collectAsStateWithLifecycle()

    val isLoading: Boolean by viewModel.isLoading.collectAsStateWithLifecycle()
    val isMoodAndGenresLoading = isLoading && explorePage?.moodAndGenres == null
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isRandomizing by viewModel.isRandomizing.collectAsStateWithLifecycle()
    val pullRefreshState = rememberPullToRefreshState()

    val quickPicksLazyGridState = rememberLazyGridState()
    val forgottenFavoritesLazyGridState = rememberLazyGridState()

    val accountName by viewModel.accountName.collectAsStateWithLifecycle()
    val accountImageUrl by viewModel.accountImageUrl.collectAsStateWithLifecycle()
    val innerTubeCookie by rememberPreference(InnerTubeCookieKey, "")
    val (randomizeHomeOrder) = rememberPreference(RandomizeHomeOrderKey, true)

    LaunchedEffect(Unit) { viewModel.loadHomeData() }

    val shouldShowWrappedCard by viewModel.showWrappedCard.collectAsStateWithLifecycle()
    val wrappedState by viewModel.wrappedManager.state.collectAsStateWithLifecycle()
    val isWrappedDataReady = wrappedState.isDataReady

    val isLoggedIn =
        remember(innerTubeCookie) {
            "SAPISID" in parseCookieString(innerTubeCookie)
        }
    val url = if (isLoggedIn) accountImageUrl else null

    var cachedPodcasts by remember { mutableStateOf<List<PodcastItem>>(emptyList()) }

    val featuredPodcasts =
        remember(homePage, selectedChip) {
            if (selectedChip == null) {
                cachedPodcasts = emptyList()
                emptyList()
            } else {
                val newPodcasts =
                    homePage
                        ?.sections
                        ?.flatMap { it.items }
                        ?.filterIsInstance<EpisodeItem>()
                        ?.mapNotNull { episode ->
                            episode.podcast?.let { podcast ->
                                PodcastItem(
                                    id = podcast.id,
                                    title = podcast.name,
                                    author = episode.author,
                                    episodeCountText = null,
                                    thumbnail = episode.thumbnail,
                                    playEndpoint = null,
                                    shuffleEndpoint = null,
                                )
                            }
                        }?.distinctBy { it.id }
                        ?.shuffled()
                        ?.take(10)
                        ?: emptyList()

                if (newPodcasts.isNotEmpty()) {
                    cachedPodcasts = newPodcasts
                }
                cachedPodcasts
            }
        }

    val scope = rememberCoroutineScope()
    var randomizeJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val lazylistState = rememberLazyListState()
    val gridItemSize by rememberEnumPreference(GridItemsSizeKey, GridItemSize.BIG)
    val currentGridHeight = if (gridItemSize == GridItemSize.BIG) GridThumbnailHeight else SmallGridThumbnailHeight
    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop =
        backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsStateWithLifecycle()

    val wrappedDismissed by backStackEntry
        ?.savedStateHandle
        ?.getStateFlow("wrapped_seen", false)
        ?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    var randomSeed by rememberSaveable { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(isRefreshing) {
        if (isRefreshing) {
            randomSeed = System.currentTimeMillis()
        }
    }

    val foundInSettings = stringResource(R.string.found_in_settings_content)
    LaunchedEffect(wrappedDismissed) {
        if (wrappedDismissed) {
            viewModel.markWrappedAsSeen()
            scope.launch {
                snackbarHostState.showSnackbar(foundInSettings)
            }
            backStackEntry?.savedStateHandle?.set("wrapped_seen", false)
        }
    }

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazylistState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow {
            lazylistState.layoutInfo.visibleItemsInfo
                .lastOrNull()
                ?.index
        }.collect { lastVisibleIndex ->
            val len = lazylistState.layoutInfo.totalItemsCount
            if (lastVisibleIndex != null && lastVisibleIndex >= len - 3) {
                viewModel.loadMoreYouTubeItems(homePage?.continuation)
            }
        }
    }

    if (selectedChip != null) {
        BackHandler {
            viewModel.toggleChip(selectedChip)
        }
    }

    val localGridItem: @Composable (LocalItem) -> Unit = {
        when (it) {
            is Song -> {
                HomeListItem(
                    title = it.title,
                    subtitle = it.artists.joinToString { artist -> artist.name },
                    thumbnail = it.thumbnailUrl,
                    isActive = it.id == mediaMetadata?.id,
                    onClick = {
                        if (!isListenTogetherGuest) {
                            if (it.id == mediaMetadata?.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue.radio(it.toMediaMetadata()),
                                )
                            }
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            SongMenu(
                                originalSong = it,
                                navController = navController,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is Album -> {
                HomeListItem(
                    title = it.title,
                    subtitle = it.artists.joinToString { artist -> artist.name },
                    thumbnail = it.thumbnailUrl,
                    isActive = it.id == mediaMetadata?.album?.id,
                    onClick = { navController.navigate("album/${it.id}") },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            AlbumMenu(
                                originalAlbum = it,
                                navController = navController,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is Artist -> {
                HomeListItem(
                    title = it.title,
                    subtitle = null,
                    thumbnail = it.thumbnailUrl,
                    isActive = false,
                    onClick = { navController.navigate("artist/${it.id}") },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            ArtistMenu(
                                originalArtist = it,
                                coroutineScope = scope,
                                onDismiss = menuState::dismiss,
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            is Playlist -> {}
        }
    }

    val ytGridItem: @Composable (YTItem) -> Unit = { item ->
        val subtitle = when (item) {
            is SongItem -> item.artists.joinToString { it.name }
            is AlbumItem -> listOfNotNull(item.artists?.joinToString { it.name }, item.year?.toString()).joinToString(" | ")
            is ArtistItem -> null
            is PlaylistItem -> listOfNotNull(item.author?.name, item.songCountText).joinToString(" | ")
            is PodcastItem -> listOfNotNull(item.author?.name, item.episodeCountText).joinToString(" | ")
            is EpisodeItem -> listOfNotNull(item.author?.name, item.publishDateText).joinToString(" | ")
            else -> null
        }
        val thumbnail = when (item) {
            is SongItem -> item.thumbnail
            is AlbumItem -> item.thumbnail
            is ArtistItem -> item.thumbnail
            is PlaylistItem -> item.thumbnail
            is PodcastItem -> item.thumbnail
            is EpisodeItem -> item.thumbnail
            else -> null
        }
        val isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id)
        HomeGridRow(
            title = item.title,
            subtitle = subtitle,
            thumbnail = thumbnail,
            isActive = isActive,
            onClick = {
                when (item) {
                    is SongItem -> {
                        if (!isListenTogetherGuest) {
                            playerConnection.playQueue(
                                YouTubeQueue(
                                    item.endpoint ?: WatchEndpoint(videoId = item.id),
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
                        if (!isListenTogetherGuest) {
                            playerConnection.playQueue(
                                ListQueue(
                                    title = item.title,
                                    items = listOf(item.toMediaMetadata().toMediaItem()),
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
                                coroutineScope = scope,
                                onDismiss = menuState::dismiss,
                            )
                        }

                        is PodcastItem -> {
                            YouTubePlaylistMenu(
                                playlist = item.asPlaylistItem(),
                                coroutineScope = scope,
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
            modifier = Modifier.width(280.dp),
        )
    }

    val homeSections =
        remember(
            randomizeHomeOrder,
            randomSeed,
            selectedChip,
            speedDialItems,
            quickPicks,
            dailyDiscover,
            keepListening,
            accountPlaylists,
            forgottenFavorites,
            communityPlaylists,
            similarRecommendations,
            homePage?.sections,
            explorePage?.moodAndGenres,
        ) {
            val list = mutableListOf<HomeSection>()
            val chipActive = selectedChip != null

            if (!chipActive && speedDialItems.isNotEmpty()) list.add(HomeSection.SpeedDial)
            if (!chipActive && quickPicks?.isNotEmpty() == true) list.add(HomeSection.QuickPicks)
            if (!chipActive && communityPlaylists?.isNotEmpty() == true) list.add(HomeSection.FromTheCommunity)
            if (!chipActive && dailyDiscover?.isNotEmpty() == true) list.add(HomeSection.DailyDiscover)
            if (!chipActive && keepListening?.isNotEmpty() == true) list.add(HomeSection.KeepListening)
            if (!chipActive && accountPlaylists?.isNotEmpty() == true) list.add(HomeSection.AccountPlaylists)
            if (!chipActive && forgottenFavorites?.isNotEmpty() == true) list.add(HomeSection.ForgottenFavorites)

            if (!chipActive) {
                similarRecommendations?.indices?.forEach { i ->
                    list.add(HomeSection.SimilarRecommendation(i))
                }
            }

            homePage?.sections?.indices?.forEach { i ->
                list.add(HomeSection.HomePageSection(i))
            }

            if (explorePage?.moodAndGenres != null) list.add(HomeSection.MoodAndGenres)

            if (randomizeHomeOrder) {
                list.sortedByDescending { section ->
                    val sectionRandom = Random(randomSeed + section.id.hashCode())
                    val base =
                        when (section) {
                            HomeSection.SpeedDial,
                            HomeSection.QuickPicks,
                            HomeSection.DailyDiscover,
                            -> 500

                            HomeSection.KeepListening,
                            HomeSection.AccountPlaylists,
                            HomeSection.ForgottenFavorites,
                            HomeSection.FromTheCommunity,
                            -> 300

                            else -> 100
                        }

                    val modifier =
                        when (section) {
                            HomeSection.SpeedDial,
                            HomeSection.QuickPicks,
                            HomeSection.DailyDiscover,
                            -> sectionRandom.nextInt(-200, 400)

                            HomeSection.KeepListening,
                            HomeSection.AccountPlaylists,
                            HomeSection.ForgottenFavorites,
                            HomeSection.FromTheCommunity,
                            -> sectionRandom.nextInt(-100, 400)

                            else -> sectionRandom.nextInt(-50, 50)
                        }
                    base + modifier
                }
            } else {
                val defaultOrder =
                    mapOf(
                        HomeSection.SpeedDial to 100,
                        HomeSection.QuickPicks to 90,
                        HomeSection.FromTheCommunity to 80,
                        HomeSection.DailyDiscover to 70,
                        HomeSection.KeepListening to 60,
                        HomeSection.AccountPlaylists to 50,
                        HomeSection.ForgottenFavorites to 40,
                        HomeSection.MoodAndGenres to 10,
                    )

                list.sortedByDescending { section ->
                    when (section) {
                        is HomeSection.SimilarRecommendation -> 30 - section.index
                        is HomeSection.HomePageSection -> 20 - section.index
                        else -> defaultOrder[section] ?: 0
                    }
                }
            }
        }

    LaunchedEffect(quickPicks) {
        quickPicksLazyGridState.scrollToItem(0)
    }

    LaunchedEffect(forgottenFavorites) {
        forgottenFavoritesLazyGridState.scrollToItem(0)
    }

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = viewModel::refresh,
        indicator = {
            Indicator(
                isRefreshing = isRefreshing,
                state = pullRefreshState,
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .padding(LocalPlayerAwareWindowInsets.current.asPaddingValues()),
            )
        },
    ) {
        BoxWithConstraints(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopStart,
        ) {
            val horizontalLazyGridItemWidthFactor = if (maxWidth * 0.475f >= 320.dp) 0.475f else 0.9f
            val horizontalLazyGridItemWidth = maxWidth * horizontalLazyGridItemWidthFactor
            val quickPicksSnapLayoutInfoProvider =
                remember(quickPicksLazyGridState) {
                    SnapLayoutInfoProvider(
                        lazyGridState = quickPicksLazyGridState,
                        positionInLayout = { layoutSize, itemSize ->
                            (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                        },
                    )
                }
            val forgottenFavoritesSnapLayoutInfoProvider =
                remember(forgottenFavoritesLazyGridState) {
                    SnapLayoutInfoProvider(
                        lazyGridState = forgottenFavoritesLazyGridState,
                        positionInLayout = { layoutSize, itemSize ->
                            (layoutSize * horizontalLazyGridItemWidthFactor / 2f - itemSize / 2f)
                        },
                    )
                }

            LazyColumn(
                state = lazylistState,
                contentPadding = LocalPlayerAwareWindowInsets.current.asPaddingValues(),
            ) {
                item {
                    ChipsRow(
                        chips = homePage?.chips?.map { it to it.title } ?: emptyList(),
                        currentValue = selectedChip,
                        onValueUpdate = {
                            viewModel.toggleChip(it)
                        },
                        modifier = Modifier.padding(top = 12.dp),
                    )
                }

                if (isLoading && homePage?.chips.isNullOrEmpty()) {
                    item(key = "chips_shimmer") {
                        ShimmerHost(showGradient = false) {
                            LazyRow(
                                contentPadding =
                                    WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            ) {
                                items(5) {
                                    TextPlaceholder(
                                        height = 30.dp,
                                        shape = RoundedCornerShape(0.dp),
                                        modifier = Modifier.width(72.dp),
                                    )
                                }
                            }
                        }
                    }
                }

                if (selectedChip?.title?.contains("Podcast", ignoreCase = true) == true) {
                    if (savedPodcastShows.isNotEmpty()) {
                        item(key = "00_your_shows_title") {
                            RetroSectionHeader(
                                title = stringResource(R.string.your_shows),
                                action = {
                                    RetroTextButton(
                                        text = "MORE",
                                        onClick = {
                                            navController.navigate("youtube_browse/FEmusic_library_non_music_audio_list")
                                        },
                                    )
                                },
                            )
                        }

                        item(key = "00_your_shows_list") {
                            LazyRow(
                                contentPadding =
                                    WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                            ) {
                                items(savedPodcastShows, key = { it.id }) { podcast ->
                                    ytGridItem(podcast)
                                }
                            }
                        }
                    }

                    if (episodesForLater.isNotEmpty()) {
                        item(key = "00_episodes_for_later_title") {
                            RetroSectionHeader(
                                title = stringResource(R.string.episodes_for_later),
                                action = {
                                    RetroTextButton(
                                        text = "MORE",
                                        onClick = {
                                            navController.navigate("online_playlist/SE")
                                        },
                                    )
                                },
                            )
                        }

                        item(key = "00_episodes_for_later_list") {
                            LazyRow(
                                contentPadding =
                                    WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                            ) {
                                items(episodesForLater, key = { it.id }) { episode ->
                                    ytGridItem(episode)
                                }
                            }
                        }
                    }

                    if (featuredPodcasts.isNotEmpty() && savedPodcastShows.isEmpty()) {
                        item(key = "0_podcast_channels_title") {
                            RetroSectionHeader(
                                title = stringResource(R.string.podcast_channels),
                            )
                        }

                        item(key = "0_podcast_channels_list") {
                            LazyRow(
                                contentPadding =
                                    WindowInsets.systemBars
                                        .only(WindowInsetsSides.Horizontal)
                                        .asPaddingValues(),
                            ) {
                                items(featuredPodcasts, key = { it.id }) { podcast ->
                                    ytGridItem(podcast)
                                }
                            }
                        }
                    }

                    if (homeSections.filterIsInstance<HomeSection.HomePageSection>().isNotEmpty()) {
                        item(key = "0_latest_episodes_title") {
                            RetroSectionHeader(
                                title = stringResource(R.string.latest_episodes),
                            )
                        }
                    }

                    homeSections.filterIsInstance<HomeSection.HomePageSection>().forEach { section ->
                        val sectionData = homePage?.sections?.getOrNull(section.index)
                        val skipTitles = listOf("your shows", "episodes for later", "podcast channels", "new episodes")
                        if (sectionData?.title?.lowercase()?.let { title -> skipTitles.any { title.contains(it) } } == true) {
                            return@forEach
                        }
                        sectionData?.let {
                            item(key = "1_chip_section_title_${section.index}") {
                                RetroSectionHeader(
                                    title = sectionData.title,
                                    action = sectionData.endpoint?.let { endpoint ->
                                        {
                                            RetroTextButton(
                                                text = "MORE",
                                                onClick = {
                                                    when {
                                                        endpoint.browseId == "FEmusic_moods_and_genres" -> {
                                                            navController.navigate("mood_and_genres")
                                                        }
                                                        else -> {
                                                            navController.navigate("youtube_browse/${endpoint.browseId}?params=${endpoint.params}")
                                                        }
                                                    }
                                                },
                                            )
                                        }
                                    },
                                )
                            }

                            item(key = "1_chip_section_list_${section.index}") {
                                LazyRow(
                                    contentPadding =
                                        WindowInsets.systemBars
                                            .only(WindowInsetsSides.Horizontal)
                                            .asPaddingValues(),
                                ) {
                                    items(sectionData.items, key = { it.id }) { item ->
                                        ytGridItem(item)
                                    }
                                }
                            }
                        }
                    }
                }

                if (selectedChip == null) {
                    item(key = "wrapped_card") {
                        AnimatedVisibility(visible = shouldShowWrappedCard) {
                            RetroPanel(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                strong = true,
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = stringResource(R.string.wrapped_ready_title),
                                        style = MaterialTheme.typography.titleMedium,
                                        color = if (isWrappedDataReady) RetroTokens.TextHot else RetroTokens.TextMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    if (isWrappedDataReady) {
                                        RetroTextButton(
                                            text = stringResource(R.string.open),
                                            onClick = { navController.navigate("wrapped") },
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                homeSections.forEach { section ->
                    when (section) {
                        HomeSection.SpeedDial -> {
                            speedDialItems.takeIf { it.isNotEmpty() }?.let { items ->
                                item(key = "speed_dial_title") {
                                    RetroSectionHeader(
                                        title = stringResource(R.string.speed_dial),
                                    )
                                }

                                item(key = "speed_dial_list") {
                                    val targetItemSize = 128.dp
                                    val availableWidth = maxWidth - 32.dp
                                    val columns = (availableWidth / targetItemSize).toInt().coerceAtLeast(4)
                                    val rows = if (columns >= 6) 1 else 2
                                    val itemsPerPage = columns * rows
                                    val itemWidth = availableWidth / columns

                                    val pagerState = rememberPagerState(pageCount = { (items.size + itemsPerPage - 1) / itemsPerPage })

                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        HorizontalPager(
                                            state = pagerState,
                                            contentPadding = PaddingValues(horizontal = 16.dp),
                                            pageSpacing = 16.dp,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(itemWidth * rows),
                                        ) { page ->
                                            val pageStartIndex = page * itemsPerPage
                                            val pageItems = items.drop(pageStartIndex).take(itemsPerPage)

                                            Column(modifier = Modifier.fillMaxSize()) {
                                                for (row in 0 until rows) {
                                                    Row(modifier = Modifier.fillMaxWidth()) {
                                                        for (col in 0 until columns) {
                                                            val itemIndex = row * columns + col
                                                            val isRandomizeSlot = (page == 0 && itemIndex == itemsPerPage - 1)

                                                            if (isRandomizeSlot) {
                                                                Box(
                                                                    modifier =
                                                                        Modifier
                                                                            .width(itemWidth)
                                                                            .height(itemWidth)
                                                                            .padding(4.dp)
                                                                            .retroBorder(),
                                                                ) {
                                                                    RandomizeGridItem(
                                                                        modifier = Modifier.fillMaxSize(),
                                                                        isLoading = isRandomizing,
                                                                        onClick = {
                                                                            if (isRandomizing) {
                                                                                randomizeJob?.cancel()
                                                                            } else if (!isListenTogetherGuest) {
                                                                                randomizeJob =
                                                                                    scope.launch {
                                                                                        val randomItem = viewModel.getRandomItem()
                                                                                        if (randomItem != null) {
                                                                                            when (randomItem) {
                                                                                                is SongItem -> {
                                                                                                    playerConnection.playQueue(
                                                                                                        YouTubeQueue(
                                                                                                            randomItem.endpoint
                                                                                                                ?: WatchEndpoint(
                                                                                                                    videoId = randomItem.id,
                                                                                                                ),
                                                                                                            randomItem.toMediaMetadata(),
                                                                                                        ),
                                                                                                    )
                                                                                                }
                                                                                                is AlbumItem -> navController.navigate("album/${randomItem.id}")
                                                                                                is ArtistItem -> navController.navigate("artist/${randomItem.id}")
                                                                                                is PlaylistItem -> navController.navigate("online_playlist/${randomItem.id}")
                                                                                                is PodcastItem -> navController.navigate("online_podcast/${randomItem.id}")
                                                                                                is EpisodeItem -> {
                                                                                                    playerConnection.playQueue(
                                                                                                        ListQueue(
                                                                                                            title = randomItem.title,
                                                                                                            items = listOf(randomItem.toMediaMetadata().toMediaItem()),
                                                                                                        ),
                                                                                                    )
                                                                                                }
                                                                                            }
                                                                                        }
                                                                                    }
                                                                            }
                                                                        },
                                                                    )
                                                                }
                                                            } else if (itemIndex < pageItems.size) {
                                                                val item = pageItems[itemIndex]
                                                                val isPinned by database.speedDialDao
                                                                    .isPinned(item.id)
                                                                    .collectAsStateWithLifecycle(initialValue = false)

                                                                Box(
                                                                    modifier =
                                                                        Modifier
                                                                            .width(itemWidth)
                                                                            .height(itemWidth)
                                                                            .padding(4.dp)
                                                                            .retroBorder(),
                                                                ) {
                                                                    SpeedDialGridItem(
                                                                        item = item,
                                                                        isPinned = isPinned,
                                                                        isActive = item.id in listOf(mediaMetadata?.album?.id, mediaMetadata?.id),
                                                                        isPlaying = isPlaying,
                                                                        modifier =
                                                                            Modifier
                                                                                .fillMaxSize()
                                                                                .combinedClickable(
                                                                                    onClick = {
                                                                                        when (item) {
                                                                                            is SongItem -> {
                                                                                                if (!isListenTogetherGuest) {
                                                                                                    playerConnection.playQueue(
                                                                                                        YouTubeQueue(
                                                                                                            item.endpoint
                                                                                                                ?: WatchEndpoint(
                                                                                                                    videoId = item.id,
                                                                                                                ),
                                                                                                            item.toMediaMetadata(),
                                                                                                        ),
                                                                                                    )
                                                                                                }
                                                                                            }
                                                                                            is AlbumItem -> navController.navigate("album/${item.id}")
                                                                                            is ArtistItem -> navController.navigate("artist/${item.id}")
                                                                                            is PlaylistItem -> {
                                                                                                val rawType = pinnedSpeedDialItems.find { it.id == item.id }?.type
                                                                                                if (rawType == "LOCAL_PLAYLIST") {
                                                                                                    navController.navigate("local_playlist/${item.id}")
                                                                                                } else {
                                                                                                    navController.navigate("online_playlist/${item.id}")
                                                                                                }
                                                                                            }
                                                                                            is PodcastItem -> navController.navigate("online_podcast/${item.id}")
                                                                                            is EpisodeItem -> {
                                                                                                if (!isListenTogetherGuest) {
                                                                                                    playerConnection.playQueue(
                                                                                                        ListQueue(
                                                                                                            title = item.title,
                                                                                                            items = listOf(item.toMediaMetadata().toMediaItem()),
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
                                                                                                        coroutineScope = scope,
                                                                                                        onDismiss = menuState::dismiss,
                                                                                                    )
                                                                                                }
                                                                                                is PodcastItem -> {
                                                                                                    YouTubePlaylistMenu(
                                                                                                        playlist = item.asPlaylistItem(),
                                                                                                        coroutineScope = scope,
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
                                                                                ),
                                                                    )
                                                                }
                                                            } else {
                                                                Spacer(modifier = Modifier.width(itemWidth))
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }

                                        if (pagerState.pageCount > 1) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .height(24.dp)
                                                        .fillMaxWidth(),
                                                contentAlignment = Alignment.Center,
                                            ) {
                                                Text(
                                                    text = "${pagerState.currentPage + 1}/${pagerState.pageCount}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = RetroTokens.TextMuted,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HomeSection.QuickPicks -> {
                            quickPicks?.takeIf { it.isNotEmpty() }?.let { quickPicks ->
                                item(key = "quick_picks_title") {
                                    val quickPicksTitle = stringResource(R.string.quick_picks)
                                    RetroSectionHeader(
                                        title = quickPicksTitle,
                                        action = {
                                            if (!isListenTogetherGuest) {
                                                RetroTextButton(
                                                    text = stringResource(R.string.play_all),
                                                    onClick = {
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = quickPicksTitle,
                                                                items = quickPicks.distinctBy { it.id }.map { it.toMediaItem() },
                                                            ),
                                                        )
                                                    },
                                                )
                                            }
                                        },
                                    )
                                }

                                item(key = "quick_picks_list") {
                                    LazyHorizontalGrid(
                                        state = quickPicksLazyGridState,
                                        rows = GridCells.Fixed(4),
                                        flingBehavior = rememberSnapFlingBehavior(quickPicksSnapLayoutInfoProvider),
                                        contentPadding =
                                            WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(ListItemHeight * 4),
                                    ) {
                                        items(
                                            items = quickPicks.distinctBy { it.id },
                                            key = { "home_quickpick_${it.id}" },
                                        ) { originalSong ->
                                            val song by database
                                                .song(originalSong.id)
                                                .collectAsStateWithLifecycle(initialValue = originalSong)

                                            HomeListItem(
                                                title = song!!.title,
                                                subtitle = song!!.artists.joinToString { it.name },
                                                thumbnail = song!!.thumbnailUrl,
                                                isActive = song!!.id == mediaMetadata?.id,
                                                onClick = {
                                                    if (!isListenTogetherGuest) {
                                                        if (song!!.id == mediaMetadata?.id) {
                                                            playerConnection.togglePlayPause()
                                                        } else {
                                                            playerConnection.playQueue(
                                                                YouTubeQueue.radio(
                                                                    song!!.toMediaMetadata(),
                                                                ),
                                                            )
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = song!!,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                                trailing = {
                                                    RetroIconButton(
                                                        onClick = {
                                                            menuState.show {
                                                                SongMenu(
                                                                    originalSong = song!!,
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
                                                modifier = Modifier.width(horizontalLazyGridItemWidth),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HomeSection.FromTheCommunity -> {
                            communityPlaylists?.takeIf { it.isNotEmpty() }?.let { playlists ->
                                item(key = "community_playlists_title") {
                                    RetroSectionHeader(
                                        title = stringResource(R.string.from_the_community),
                                    )
                                }

                                item(key = "community_playlists_content") {
                                    LazyRow(
                                        contentPadding = PaddingValues(horizontal = 16.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        items(playlists) { item ->
                                            HomeGridRow(
                                                title = item.playlist.title,
                                                subtitle = item.playlist.author?.name,
                                                thumbnail = item.playlist.thumbnail,
                                                onClick = {
                                                    navController.navigate("online_playlist/${item.playlist.id.removePrefix("VL")}")
                                                },
                                                modifier = Modifier.width(280.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        HomeSection.DailyDiscover -> {
                            dailyDiscover?.takeIf { it.isNotEmpty() }?.let { discoverList ->
                                item(key = "daily_discover_title") {
                                    val title = stringResource(R.string.your_daily_discover)
                                    RetroSectionHeader(
                                        title = title,
                                        action = {
                                            RetroTextButton(
                                                text = stringResource(R.string.play_all),
                                                onClick = {
                                                    val queueItems =
                                                        discoverList.mapNotNull {
                                                            (it.recommendation as? SongItem)?.toMediaMetadata()
                                                        }
                                                    if (queueItems.isNotEmpty()) {
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = title,
                                                                items = queueItems.map { it.toMediaItem() },
                                                            ),
                                                        )
                                                    }
                                                },
                                            )
                                        },
                                    )
                                }

                                item(key = "daily_discover_content") {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp),
                                    ) {
                                        discoverList.chunked(2).forEach { row ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            ) {
                                                row.forEach { item ->
                                                    DailyDiscoverCard(
                                                        dailyDiscover = item,
                                                        onClick = {
                                                            if (!isListenTogetherGuest) {
                                                                val song = item.recommendation as? SongItem
                                                                val mediaMetadata = song?.toMediaMetadata()
                                                                if (mediaMetadata != null) {
                                                                    playerConnection.playQueue(
                                                                        YouTubeQueue(
                                                                            song.endpoint ?: WatchEndpoint(videoId = song.id),
                                                                            mediaMetadata,
                                                                        ),
                                                                    )
                                                                }
                                                            }
                                                        },
                                                        navController = navController,
                                                        modifier = Modifier.weight(1f),
                                                    )
                                                }
                                                if (row.size < 2) {
                                                    Spacer(modifier = Modifier.weight(1f))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HomeSection.KeepListening -> {
                            keepListening?.takeIf { it.isNotEmpty() }?.let { keepListening ->
                                item(key = "keep_listening_title") {
                                    RetroSectionHeader(
                                        title = stringResource(R.string.keep_listening),
                                    )
                                }

                                item(key = "keep_listening_list") {
                                    val rows = if (keepListening.size > 6) 2 else 1
                                    LazyHorizontalGrid(
                                        state = remember("keep_listening_grid") { LazyGridState() },
                                        rows = GridCells.Fixed(rows),
                                        contentPadding =
                                            WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(
                                                    (
                                                        currentGridHeight +
                                                            with(LocalDensity.current) {
                                                                MaterialTheme.typography.bodyLarge.lineHeight
                                                                    .toDp() * 2 +
                                                                    MaterialTheme.typography.bodyMedium.lineHeight
                                                                        .toDp() * 2
                                                            }
                                                    ) * rows,
                                                ),
                                    ) {
                                        items(keepListening, key = { it.id }) {
                                            localGridItem(it)
                                        }
                                    }
                                }
                            }
                        }

                        HomeSection.AccountPlaylists -> {
                            accountPlaylists?.takeIf { it.isNotEmpty() }?.let { accountPlaylists ->
                                item(key = "account_playlists_title") {
                                    RetroSectionHeader(
                                        title = accountName ?: stringResource(R.string.mixes),
                                        action = {
                                            if (url != null) {
                                                AsyncImage(
                                                    model =
                                                        ImageRequest
                                                            .Builder(LocalContext.current)
                                                            .data(url)
                                                            .diskCachePolicy(CachePolicy.ENABLED)
                                                            .diskCacheKey(url)
                                                            .crossfade(false)
                                                            .build(),
                                                    placeholder = painterResource(id = R.drawable.person),
                                                    error = painterResource(id = R.drawable.person),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier =
                                                        Modifier
                                                            .size(ListThumbnailSize)
                                                            .border(1.dp, RetroTokens.BorderMuted),
                                                )
                                            } else {
                                                Icon(
                                                    painter = painterResource(id = R.drawable.person),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(ListThumbnailSize),
                                                    tint = RetroTokens.Text,
                                                )
                                            }
                                        },
                                    )
                                }

                                item(key = "account_playlists_list") {
                                    LazyRow(
                                        contentPadding =
                                            WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                    ) {
                                        items(
                                            items = accountPlaylists.distinctBy { it.id },
                                            key = { "home_account_playlist_${it.id}" },
                                        ) { item ->
                                            ytGridItem(item)
                                        }
                                    }
                                }
                            }
                        }

                        HomeSection.ForgottenFavorites -> {
                            forgottenFavorites?.takeIf { it.isNotEmpty() }?.let { forgottenFavorites ->
                                item(key = "forgotten_favorites_title") {
                                    val forgottenFavoritesTitle = stringResource(R.string.forgotten_favorites)
                                    RetroSectionHeader(
                                        title = forgottenFavoritesTitle,
                                        action = {
                                            if (!isListenTogetherGuest) {
                                                RetroTextButton(
                                                    text = stringResource(R.string.play_all),
                                                    onClick = {
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = forgottenFavoritesTitle,
                                                                items = forgottenFavorites.distinctBy { it.id }.map { it.toMediaItem() },
                                                            ),
                                                        )
                                                    },
                                                )
                                            }
                                        },
                                    )
                                }

                                item(key = "forgotten_favorites_list") {
                                    val rows = min(4, forgottenFavorites.size)
                                    LazyHorizontalGrid(
                                        state = forgottenFavoritesLazyGridState,
                                        rows = GridCells.Fixed(rows),
                                        contentPadding =
                                            WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                        flingBehavior =
                                            rememberSnapFlingBehavior(
                                                forgottenFavoritesSnapLayoutInfoProvider,
                                            ),
                                        modifier =
                                            Modifier
                                                .fillMaxWidth()
                                                .height(ListItemHeight * rows),
                                    ) {
                                        items(
                                            items = forgottenFavorites.distinctBy { it.id },
                                            key = { "home_forgotten_${it.id}" },
                                        ) { originalSong ->
                                            val song by database
                                                .song(originalSong.id)
                                                .collectAsStateWithLifecycle(initialValue = originalSong)

                                            HomeListItem(
                                                title = song!!.title,
                                                subtitle = song!!.artists.joinToString { it.name },
                                                thumbnail = song!!.thumbnailUrl,
                                                isActive = song!!.id == mediaMetadata?.id,
                                                onClick = {
                                                    if (!isListenTogetherGuest) {
                                                        if (song!!.id == mediaMetadata?.id) {
                                                            playerConnection.togglePlayPause()
                                                        } else {
                                                            playerConnection.playQueue(
                                                                YouTubeQueue.radio(
                                                                    song!!.toMediaMetadata(),
                                                                ),
                                                            )
                                                        }
                                                    }
                                                },
                                                onLongClick = {
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    menuState.show {
                                                        SongMenu(
                                                            originalSong = song!!,
                                                            navController = navController,
                                                            onDismiss = menuState::dismiss,
                                                        )
                                                    }
                                                },
                                                trailing = {
                                                    RetroIconButton(
                                                        onClick = {
                                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                            menuState.show {
                                                                SongMenu(
                                                                    originalSong = song!!,
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
                                                modifier = Modifier.width(horizontalLazyGridItemWidth),
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        is HomeSection.SimilarRecommendation -> {
                            val recommendation = similarRecommendations?.getOrNull(section.index)
                            recommendation?.let {
                                item(key = "similar_to_title_${section.index}") {
                                    RetroSectionHeader(
                                        title = recommendation.title.title,
                                        action = {
                                            if (recommendation.title.thumbnailUrl != null) {
                                                AsyncImage(
                                                    model = recommendation.title.thumbnailUrl,
                                                    contentDescription = null,
                                                    modifier =
                                                        Modifier
                                                            .size(ListThumbnailSize)
                                                            .border(1.dp, RetroTokens.BorderMuted),
                                                )
                                            }
                                        },
                                    )
                                }

                                item(key = "similar_to_list_${section.index}") {
                                    LazyRow(
                                        contentPadding =
                                            WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                    ) {
                                        items(recommendation.items, key = { it.id }) { item ->
                                            ytGridItem(item)
                                        }
                                    }
                                }
                            }
                        }

                        is HomeSection.HomePageSection -> {
                            if (selectedChip?.title?.contains("Podcast", ignoreCase = true) == true) {
                                return@forEach
                            }
                            val sectionData = homePage?.sections?.getOrNull(section.index)
                            sectionData?.let {
                                val sectionSongs = sectionData.items.filterIsInstance<SongItem>()
                                val hasPlayableSongs = sectionSongs.isNotEmpty()
                                val isSongsOnlySection =
                                    sectionData.items.isNotEmpty() &&
                                        sectionData.items.all { it is SongItem }

                                item(key = "home_section_title_${section.index}") {
                                    RetroSectionHeader(
                                        title = sectionData.title,
                                        action = {
                                            if (hasPlayableSongs && !isListenTogetherGuest) {
                                                RetroTextButton(
                                                    text = stringResource(R.string.play_all),
                                                    onClick = {
                                                        playerConnection.playQueue(
                                                            ListQueue(
                                                                title = sectionData.title,
                                                                items = sectionSongs.map { it.toMediaMetadata().toMediaItem() },
                                                            ),
                                                        )
                                                    },
                                                )
                                            }
                                        },
                                    )
                                }

                                if (isSongsOnlySection) {
                                    item(key = "home_section_list_${section.index}") {
                                        LazyHorizontalGrid(
                                            state = remember("section_${section.index}_grid") { LazyGridState() },
                                            rows = GridCells.Fixed(4),
                                            contentPadding =
                                                WindowInsets.systemBars
                                                    .only(WindowInsetsSides.Horizontal)
                                                    .asPaddingValues(),
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(ListItemHeight * 4),
                                        ) {
                                            items(
                                                items = sectionSongs.distinctBy { it.id },
                                                key = { "home_section_${section.index}_song_${it.id}" },
                                            ) { song ->
                                                HomeListItem(
                                                    title = song.title,
                                                    subtitle = song.artists.joinToString { it.name },
                                                    thumbnail = song.thumbnail,
                                                    isActive = song.id == mediaMetadata?.id,
                                                    onClick = {
                                                        if (!isListenTogetherGuest) {
                                                            playerConnection.playQueue(
                                                                YouTubeQueue(
                                                                    song.endpoint ?: WatchEndpoint(videoId = song.id),
                                                                    song.toMediaMetadata(),
                                                                ),
                                                            )
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
                                                    modifier = Modifier.width(horizontalLazyGridItemWidth),
                                                )
                                            }
                                        }
                                    }
                                } else {
                                    item(key = "home_section_list_${section.index}") {
                                        LazyRow(
                                            contentPadding =
                                                WindowInsets.systemBars
                                                    .only(WindowInsetsSides.Horizontal)
                                                    .asPaddingValues(),
                                        ) {
                                            items(
                                                items = sectionData.items.distinctBy { it.id },
                                                key = { "home_section_${section.index}_item_${it.id}" },
                                            ) { item ->
                                                ytGridItem(item)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        HomeSection.MoodAndGenres -> {
                            if (selectedChip?.title?.contains("Podcast", ignoreCase = true) == true) {
                                return@forEach
                            }
                            explorePage?.moodAndGenres?.let { moodAndGenres ->
                                item(key = "mood_and_genres_title") {
                                    RetroSectionHeader(
                                        title = stringResource(R.string.mood_and_genres),
                                        action = {
                                            RetroTextButton(
                                                text = "MORE",
                                                onClick = {
                                                    navController.navigate("mood_and_genres")
                                                },
                                            )
                                        },
                                    )
                                }
                                item(key = "mood_and_genres_list") {
                                    LazyRow(
                                        contentPadding =
                                            WindowInsets.systemBars
                                                .only(WindowInsetsSides.Horizontal)
                                                .asPaddingValues(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        items(
                                            items = moodAndGenres,
                                            key = { it.title },
                                        ) {
                                            Box(
                                                modifier =
                                                    Modifier
                                                        .width(156.dp)
                                                        .height(64.dp)
                                                        .background(RetroTokens.Panel)
                                                        .border(1.dp, RetroTokens.Border)
                                                        .clickable {
                                                            navController.navigate(
                                                                "youtube_browse/${it.endpoint.browseId}?params=${it.endpoint.params}",
                                                            )
                                                        }
                                                        .padding(horizontal = 12.dp),
                                                contentAlignment = Alignment.CenterStart,
                                            ) {
                                                Text(
                                                    text = it.title.uppercase(),
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = RetroTokens.Text,
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis,
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (isLoading && homePage?.sections.isNullOrEmpty()) {
                    item(key = "loading_shimmer") {
                        ShimmerHost {
                            repeat(2) {
                                TextPlaceholder(
                                    height = 36.dp,
                                    modifier =
                                        Modifier
                                            .padding(12.dp)
                                            .width(250.dp),
                                )
                                LazyRow(
                                    contentPadding =
                                        WindowInsets.systemBars
                                            .only(WindowInsetsSides.Horizontal)
                                            .asPaddingValues(),
                                ) {
                                    items(4) {
                                        GridItemPlaceHolder()
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
                            repeat(4) {
                                Row {
                                    repeat(2) {
                                        TextPlaceholder(
                                            height = 48.dp,
                                            shape = RoundedCornerShape(0.dp),
                                            modifier =
                                                Modifier
                                                    .padding(horizontal = 12.dp)
                                                    .width(200.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "terminal_status") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .background(RetroTokens.Background2)
                            .border(1.dp, RetroTokens.BorderMuted)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = ">",
                            color = RetroTokens.TextSoft,
                            style = MaterialTheme.typography.labelSmall,
                        )
                        Text(
                            text = " ${allLocalItems.size} LOCAL | ${allYtItems.size} ONLINE | ${if (isPlaying) "PLAYING" else "READY"}",
                            color = RetroTokens.TextMuted,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                }
            }
        }
    }
}

/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.innertube.models.Artist
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.innertube.utils.parseCookieString
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.CONTENT_TYPE_ARTIST
import com.metrolist.music.constants.InnerTubeCookieKey
import com.metrolist.music.constants.StatPeriod
import com.metrolist.music.extensions.toMediaItem
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.ArtistListItem
import com.metrolist.music.ui.component.EmptyPlaceholder
import com.metrolist.music.ui.component.HideOnScrollFAB
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.PlaylistGridItem
import com.metrolist.music.ui.component.TimeTransfer
import com.metrolist.music.ui.menu.AlbumMenu
import com.metrolist.music.ui.menu.ArtistMenu
import com.metrolist.music.ui.menu.SongMenu
import com.metrolist.music.ui.theme.RetroButton
import com.metrolist.music.ui.theme.RetroCheckbox
import com.metrolist.music.ui.theme.RetroPanel
import com.metrolist.music.ui.theme.RetroTokens
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.joinByBullet
import com.metrolist.music.utils.makeTimeString
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.viewmodels.StatsViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun StatsScreen(
    navController: NavController,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val sArtists = viewModel.selectedArtists
    val toggleArtistSelection: (Artist) -> Unit = { artist ->
        if (sArtists.any { it.id == artist.id }) {
            sArtists.removeAll { it.id == artist.id }
        } else {
            sArtists.add(artist)
        }
    }
    val clearArtistSelection: () -> Unit = { sArtists.clear() }
    val menuState = LocalMenuState.current
    val haptic = LocalHapticFeedback.current
    val playerConnection = LocalPlayerConnection.current ?: return
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var inSelectMode by rememberSaveable { mutableStateOf(false) }
    val selection =
        rememberSaveable(
            saver =
                listSaver<MutableList<Long>, Long>(
                    save = { it.toList() },
                    restore = { it.toMutableStateList() },
                ),
        ) { mutableStateListOf() }
    val onExitSelectionMode = {
        inSelectMode = false
        selection.clear()
    }

    var isSearching by rememberSaveable { mutableStateOf(false) }
    var query by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue())
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(isSearching) {
        if (isSearching) {
            focusRequester.requestFocus()
        }
    }
    if (isSearching) {
        BackHandler {
            isSearching = false
            query = TextFieldValue()
        }
    } else if (inSelectMode) {
        BackHandler(onBack = onExitSelectionMode)
    }

    val indexChips by viewModel.indexChips.collectAsStateWithLifecycle()
    val mostPlayedSongs by viewModel.mostPlayedSongs.collectAsStateWithLifecycle()
    val mostPlayedSongsStats by viewModel.filteredSongs.collectAsStateWithLifecycle()
    val mostPlayedArtists by viewModel.filteredArtists.collectAsStateWithLifecycle()
    val mostPlayedAlbums by viewModel.filteredAlbums.collectAsStateWithLifecycle()
    val allArtists by viewModel.mostPlayedArtists.collectAsStateWithLifecycle()
    val firstEvent by viewModel.firstEvent.collectAsStateWithLifecycle()
    val weeklyMostPlaylist by viewModel.weeklyMostPlaylist.collectAsStateWithLifecycle()
    val monthlyMostPlaylist by viewModel.monthlyMostPlaylist.collectAsStateWithLifecycle()
    val recapPlaylists by viewModel.recapPlaylists.collectAsStateWithLifecycle()
    val currentDate = LocalDateTime.now()
    val orderedMostPlayedSongs =
        remember(mostPlayedSongsStats, mostPlayedSongs) {
            val songsById = mostPlayedSongs.associateBy { it.song.id }
            mostPlayedSongsStats.mapNotNull { statsSong -> songsById[statsSong.id] }
        }
    val mostPeriodPlaylists = listOfNotNull(weeklyMostPlaylist, monthlyMostPlaylist)
    val (innerTubeCookie) = rememberPreference(InnerTubeCookieKey, "")
    val isLoggedIn =
        remember(innerTubeCookie) {
            "SAPISID" in parseCookieString(innerTubeCookie)
        }
    val visibleStatsPlaylists =
        remember(mostPeriodPlaylists, recapPlaylists, isLoggedIn) {
            if (isLoggedIn) {
                (mostPeriodPlaylists + recapPlaylists).distinctBy { it.id }
            } else {
                mostPeriodPlaylists
            }
        }

    val coroutineScope = rememberCoroutineScope()
    val lazyListState = rememberLazyListState()
    val selectedOption by viewModel.selectedOption.collectAsStateWithLifecycle()

    var showTimeTransfer by rememberSaveable { mutableStateOf(false) }
    var prevOptionOrdinal by rememberSaveable { mutableStateOf<OptionStats?>(null) }
    var prevIndexChips by rememberSaveable { mutableStateOf<Int?>(null) }

    LaunchedEffect(showTimeTransfer) {
        if (showTimeTransfer) {
            if (prevOptionOrdinal == null) prevOptionOrdinal = selectedOption
            if (prevIndexChips == null) prevIndexChips = indexChips
            viewModel.selectedOption.value = OptionStats.CONTINUOUS
            viewModel.indexChips.value = StatPeriod.ALL.ordinal
        }
    }

    if (showTimeTransfer) {
        com.metrolist.music.ui.component.TimeTransfer(
            onDismiss = {
                showTimeTransfer = false
                prevOptionOrdinal?.let { viewModel.selectedOption.value = it }
                prevIndexChips?.let { viewModel.indexChips.value = it }
                prevOptionOrdinal = null
                prevIndexChips = null
            },
        )
    }

    LaunchedEffect(Unit) {
        viewModel.syncMostPlaylistsIfNeeded()
    }

    val weeklyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(currentDate) { it.minusWeeks(1) }
                .takeWhile { it.isAfter(firstEvent?.event?.timestamp?.minusWeeks(1)) }
                .mapIndexed { index, date ->
                    val endDate = date.plusWeeks(1).minusDays(1).coerceAtMost(currentDate)
                    val formatter = DateTimeFormatter.ofPattern("dd MMM")
                    val startDateFormatted = formatter.format(date)
                    val endDateFormatted = formatter.format(endDate)
                    val startMonth = date.month
                    val endMonth = endDate.month
                    val startYear = date.year
                    val endYear = endDate.year
                    val text =
                        when {
                            startYear != currentDate.year -> "$startDateFormatted, $startYear - $endDateFormatted, $endYear"
                            startMonth != endMonth -> "$startDateFormatted - $endDateFormatted"
                            else -> "${date.dayOfMonth} - $endDateFormatted"
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val monthlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate.plusMonths(1).withDayOfMonth(1).minusDays(1),
            ) { it.minusMonths(1) }
                .takeWhile {
                    it.isAfter(
                        firstEvent
                            ?.event
                            ?.timestamp
                            ?.withDayOfMonth(1),
                    )
                }.mapIndexed { index, date ->
                    val formatter = DateTimeFormatter.ofPattern("MMM")
                    val formattedDate = formatter.format(date)
                    val text =
                        if (date.year != currentDate.year) {
                            "$formattedDate ${date.year}"
                        } else {
                            formattedDate
                        }
                    Pair(index, text)
                }.toList()
        } else {
            emptyList()
        }

    val yearlyDates =
        if (currentDate != null && firstEvent != null) {
            generateSequence(
                currentDate
                    .plusYears(1)
                    .withDayOfYear(1)
                    .minusDays(1),
            ) { it.minusYears(1) }
                .takeWhile {
                    it.isAfter(
                        firstEvent
                            ?.event
                            ?.timestamp,
                    )
                }.mapIndexed { index, date ->
                    Pair(index, "${date.year}")
                }.toList()
        } else {
            emptyList()
        }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = lazyListState,
            contentPadding =
                LocalPlayerAwareWindowInsets.current
                    .only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom)
                    .asPaddingValues(),
            modifier =
                Modifier.windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
                ),
        ) {
            val filteredArtists =
                allArtists
                    .map { artistWrapper ->
                        Artist(
                            id = artistWrapper.artist.id,
                            name = artistWrapper.artist.name,
                        )
                    }.filter { artist ->
                        artist.name.contains(query.text, ignoreCase = true)
                    }

            item(key = "title") {
                Text(
                    text = "> STATISTICS_",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = RetroTokens.Text,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            item(key = "period_buttons") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    val options = listOf(
                        OptionStats.CONTINUOUS to stringResource(id = R.string.continuous),
                        OptionStats.WEEKS to stringResource(R.string.weeks),
                        OptionStats.MONTHS to stringResource(R.string.months),
                        OptionStats.YEARS to stringResource(R.string.years),
                    )
                    options.forEach { (opt, label) ->
                        RetroButton(
                            onClick = {
                                viewModel.selectedOption.value = opt
                                viewModel.indexChips.value = 0
                            },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selectedOption == opt) RetroTokens.TextHot else RetroTokens.TextMuted,
                            )
                        }
                    }
                }
            }

            item(key = "sub_period_buttons") {
                val chips = when (selectedOption) {
                    OptionStats.WEEKS -> weeklyDates
                    OptionStats.MONTHS -> monthlyDates
                    OptionStats.YEARS -> yearlyDates
                    OptionStats.CONTINUOUS -> listOf(
                        StatPeriod.WEEK_1.ordinal to pluralStringResource(R.plurals.n_week, 1, 1),
                        StatPeriod.MONTH_1.ordinal to pluralStringResource(R.plurals.n_month, 1, 1),
                        StatPeriod.MONTH_3.ordinal to pluralStringResource(R.plurals.n_month, 3, 3),
                        StatPeriod.MONTH_6.ordinal to pluralStringResource(R.plurals.n_month, 6, 6),
                        StatPeriod.YEAR_1.ordinal to pluralStringResource(R.plurals.n_year, 1, 1),
                        StatPeriod.ALL.ordinal to stringResource(R.string.filter_all),
                    )
                }
                if (chips.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                    ) {
                        chips.forEach { (value, label) ->
                            RetroButton(
                                onClick = { viewModel.indexChips.value = value },
                            ) {
                                Text(
                                    if (indexChips == value) "[ $label ]" else label,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (indexChips == value) RetroTokens.TextHot else RetroTokens.TextMuted,
                                )
                            }
                        }
                    }
                }
            }

            if (visibleStatsPlaylists.isNotEmpty() && !isSearching && sArtists.isEmpty()) {
                item(key = "mostPeriodPlaylistsTitle") {
                    Text(
                        text = ">${pluralStringResource(R.plurals.n_playlist, visibleStatsPlaylists.size, visibleStatsPlaylists.size).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RetroTokens.TextSoft,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                itemsIndexed(
                    items = visibleStatsPlaylists,
                    key = { _, playlist -> playlist.id },
                ) { _, playlist ->
                    PlaylistGridItem(
                        playlist = playlist,
                        autoPlaylist = true,
                        modifier =
                            Modifier
                                .combinedClickable(
                                    onClick = {
                                        navController.navigate("local_playlist/${playlist.id}")
                                    },
                                ).animateItem(),
                    )
                }
            }

            if (!isSearching && mostPlayedSongsStats.isNotEmpty()) {
                item(key = "mostPlayedSongsTitle") {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = ">${mostPlayedSongsStats.size} ${stringResource(id = R.string.songs).uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = RetroTokens.TextSoft,
                            modifier = Modifier.weight(1f),
                        )
                        if (orderedMostPlayedSongs.isNotEmpty()) {
                            RetroButton(
                                onClick = {
                                    playerConnection.playQueue(
                                        ListQueue(
                                            title = context.getString(R.string.most_played_songs),
                                            items = orderedMostPlayedSongs.map { it.toMediaMetadata().toMediaItem() },
                                        ),
                                    )
                                }
                            ) {
                                Text("PLAY ALL", style = MaterialTheme.typography.labelSmall, color = RetroTokens.Text)
                            }
                        }
                    }
                }

                item(key = "mostPlayedSongsPanel") {
                    RetroPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column {
                            mostPlayedSongsStats.forEachIndexed { index, song ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(RetroTokens.BorderDark)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (song.id == mediaMetadata?.id) {
                                                playerConnection.togglePlayPause()
                                            } else {
                                                val targetSong = mostPlayedSongs.find { it.id == song.id }
                                                if (targetSong != null) {
                                                    playerConnection.playQueue(
                                                        YouTubeQueue(
                                                            endpoint = WatchEndpoint(song.id),
                                                            preloadItem = targetSong.toMediaMetadata(),
                                                        ),
                                                    )
                                                }
                                            }
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = String.format("%02d", index + 1),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RetroTokens.TextMuted,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = song.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = RetroTokens.Text,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = joinByBullet(
                                                pluralStringResource(R.plurals.n_time, song.songCountListened, song.songCountListened),
                                                makeTimeString(song.timeListened),
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RetroTokens.TextMuted,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isSearching && mostPlayedArtists.isNotEmpty()) {
                item(key = "mostPlayedArtistsTitle") {
                    Text(
                        text = ">${mostPlayedArtists.size} ${stringResource(id = R.string.artists).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RetroTokens.TextSoft,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                item(key = "mostPlayedArtistsPanel") {
                    RetroPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column {
                            mostPlayedArtists.forEachIndexed { index, artist ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(RetroTokens.BorderDark)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("artist/${artist.id}")
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = String.format("%02d", index + 1),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RetroTokens.TextMuted,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = artist.artist.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = RetroTokens.Text,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = joinByBullet(
                                                pluralStringResource(R.plurals.n_time, artist.songCount, artist.songCount),
                                                makeTimeString(artist.timeListened?.toLong()),
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RetroTokens.TextMuted,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (!isSearching && mostPlayedAlbums.isNotEmpty()) {
                item(key = "mostPlayedAlbumsTitle") {
                    Text(
                        text = ">${mostPlayedAlbums.size} ${stringResource(id = R.string.albums).uppercase()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = RetroTokens.TextSoft,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }

                item(key = "mostPlayedAlbumsPanel") {
                    RetroPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Column {
                            mostPlayedAlbums.forEachIndexed { index, album ->
                                if (index > 0) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(1.dp)
                                            .background(RetroTokens.BorderDark)
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            navController.navigate("album/${album.id}")
                                        }
                                        .padding(horizontal = 10.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = String.format("%02d", index + 1),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = RetroTokens.TextMuted,
                                        fontFamily = FontFamily.Monospace,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = album.album.title,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = RetroTokens.Text,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = joinByBullet(
                                                pluralStringResource(R.plurals.n_time, album.songCountListened ?: 0, album.songCountListened ?: 0),
                                                makeTimeString(album.timeListened),
                                            ),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = RetroTokens.TextMuted,
                                            maxLines = 1,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (isSearching) {
                items(
                    items =
                        allArtists.filter { artist ->
                            artist.artist.name.contains(query.text, ignoreCase = true)
                        },
                    key = { "stats_artist_${it.id}" },
                    contentType = { CONTENT_TYPE_ARTIST },
                ) { artist ->
                    val uiArtist = Artist(name = artist.artist.name, id = artist.id)
                    val isChecked = sArtists.any { it.id == uiArtist.id }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clickable {
                                    toggleArtistSelection(uiArtist)
                                }.padding(8.dp),
                    ) {
                        ArtistListItem(
                            artist = artist,
                            modifier = Modifier.weight(1f),
                        )
                        RetroCheckbox(
                            checked = sArtists.contains(Artist(name = artist.artist.name, id = artist.id)),
                            onCheckedChange = {
                                toggleArtistSelection(uiArtist)
                            },
                        )
                    }
                }
            }
            if (query.text.isNotEmpty() && filteredArtists.isEmpty()) {
                item(key = "no_result") {
                    EmptyPlaceholder(
                        icon = R.drawable.search,
                        text = stringResource(R.string.no_results_found),
                    )
                }
            }
        }

        if (mostPlayedSongsStats.isNotEmpty() && !isSearching) {
            HideOnScrollFAB(
                visible = true,
                lazyListState = lazyListState,
                icon = R.drawable.shuffle,
                onClick = {
                    playerConnection.playQueue(
                        ListQueue(
                            title = context.getString(R.string.most_played_songs),
                            items = orderedMostPlayedSongs.map { it.toMediaMetadata().toMediaItem() }.shuffled(),
                        ),
                    )
                },
            )
        }
    }

    TopAppBar(
        title = {
            if (inSelectMode) {
                Text(pluralStringResource(R.plurals.n_selected, selection.size, selection.size))
            } else if (isSearching) {
                Row {
                    TextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = {
                            Text(
                                text = stringResource(R.string.search),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.titleLarge,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        colors =
                            TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                            ),
                        modifier =
                            Modifier
                                .weight(1f)
                                .focusRequester(focusRequester),
                    )
                    if (sArtists.isNotEmpty()) {
                        IconButton(onClick = clearArtistSelection) {
                            Icon(
                                painter = painterResource(R.drawable.close),
                                contentDescription = "Clear Artists",
                                tint = RetroTokens.Text,
                            )
                        }
                    }
                }
            } else {
                Text(stringResource(R.string.stats))
            }
        },
        navigationIcon = {
            if (inSelectMode) {
                IconButton(onClick = onExitSelectionMode) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = "Select Button",
                    )
                }
            } else {
                IconButton(
                    onClick = {
                        if (isSearching) {
                            isSearching = false
                            query = TextFieldValue()
                        } else {
                            navController.navigateUp()
                        }
                    },
                    onLongClick = {
                        if (!isSearching) {
                            navController.backToMain()
                        }
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_back),
                        contentDescription = "Back Button",
                    )
                }
            }
        },
        actions = {
            if (inSelectMode) {
                RetroCheckbox(
                    checked = true,
                    onCheckedChange = {
                    },
                )
                IconButton(
                    enabled = selection.isNotEmpty(),
                    onClick = {
                    },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.more_vert),
                        contentDescription = "More Button",
                    )
                }
            } else if (!isSearching) {
                IconButton(
                    onClick = { isSearching = true },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.search),
                        contentDescription = "Search Button",
                    )
                }
                IconButton(
                    onClick = { showTimeTransfer = true },
                    onLongClick = { showTimeTransfer = true },
                ) {
                    Icon(
                        painterResource(R.drawable.sync),
                        contentDescription = "Time Transfer",
                    )
                }
            }
        },
    )
}

enum class OptionStats { WEEKS, MONTHS, YEARS, CONTINUOUS }

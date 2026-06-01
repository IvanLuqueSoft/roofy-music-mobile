/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings.integrations

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.PersonalLibraryEnabledKey
import com.metrolist.music.constants.PersonalLibraryHistorySyncEpochMsKey
import com.metrolist.music.constants.PersonalLibraryPasswordKey
import com.metrolist.music.constants.PersonalLibraryServerUrlKey
import com.metrolist.music.constants.PersonalLibraryUsernameKey
import com.metrolist.music.subsonic.PersonalLibraryCredentials
import com.metrolist.music.subsonic.PersonalLibrarySync
import com.metrolist.music.subsonic.SubsonicClient
import com.metrolist.music.subsonic.SubsonicSong
import com.metrolist.music.subsonic.toMediaItem
import com.metrolist.music.subsonic.toRoofyMetadata
import com.metrolist.music.playback.queues.ListQueue
import com.metrolist.music.productux.UserFacingErrors
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.component.MediaMetadataListItem
import com.metrolist.music.ui.theme.RetroButton
import com.metrolist.music.ui.theme.RetroSurface
import com.metrolist.music.ui.theme.RetroToggle
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PersonalLibrarySettings(
    navController: NavController,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()

    var enabled by rememberPreference(PersonalLibraryEnabledKey, false)
    var serverUrl by rememberPreference(PersonalLibraryServerUrlKey, "")
    var username by rememberPreference(PersonalLibraryUsernameKey, "")
    var password by rememberPreference(PersonalLibraryPasswordKey, "")
    var testing by rememberSaveable { mutableStateOf(false) }
    var syncingFavorites by rememberSaveable { mutableStateOf(false) }
    var favoriteSyncSummary by rememberSaveable { mutableStateOf("") }
    var favoriteSyncDetailed by rememberSaveable { mutableStateOf("") }
    var syncingPlaylists by rememberSaveable { mutableStateOf(false) }
    var playlistSyncSummary by rememberSaveable { mutableStateOf("") }
    var playlistSyncDetailed by rememberSaveable { mutableStateOf("") }
    var syncingHistory by rememberSaveable { mutableStateOf(false) }
    var historySyncSummary by rememberSaveable { mutableStateOf("") }
    var historySyncDetailed by rememberSaveable { mutableStateOf("") }
    var historySyncEpochMs by rememberPreference(PersonalLibraryHistorySyncEpochMsKey, 0L)
    var syncingAll by rememberSaveable { mutableStateOf(false) }
    var catalogSyncSummary by rememberSaveable { mutableStateOf("") }
    var catalogSyncDetailed by rememberSaveable { mutableStateOf("") }
    var syncingRatings by rememberSaveable { mutableStateOf(false) }
    var ratingSyncSummary by rememberSaveable { mutableStateOf("") }
    var ratingSyncDetailed by rememberSaveable { mutableStateOf("") }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf(emptyList<SubsonicSong>()) }
    var manualSetupOpen by rememberSaveable { mutableStateOf(serverUrl.isNotBlank()) }
    var showSyncDetails by rememberSaveable { mutableStateOf(false) }

    val connectedMessage = stringResource(R.string.personal_library_connection_ok)
    val libraryErrorMessage = UserFacingErrors.libraryMessage(context)

    Column(
        modifier = Modifier
            .windowInsetsPadding(LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                LocalPlayerAwareWindowInsets.current.only(WindowInsetsSides.Top),
            ),
        )

        RetroSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    text = stringResource(R.string.personal_library_settings_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (serverUrl.isBlank()) {
                    Text(
                        text = stringResource(R.string.personal_library_qr_first_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = stringResource(R.string.personal_library_qr_first_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RetroButton(
                        onClick = { navController.navigate("link_computer") },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.phone_link_scan_qr))
                    }
                } else {
                    Text(
                        text = connectedMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = stringResource(R.string.enable_personal_library),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    RetroToggle(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                    )
                }

                if (!manualSetupOpen && serverUrl.isBlank()) {
                    Text(
                        text = stringResource(R.string.personal_library_manual_setup),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier =
                            Modifier.clickable { manualSetupOpen = true },
                    )
                }

                if (manualSetupOpen || serverUrl.isNotBlank()) {
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = { serverUrl = it.trim() },
                    label = { Text(stringResource(R.string.personal_library_server_url)) },
                    placeholder = { Text("http://192.168.1.10:4533") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text(stringResource(R.string.username)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(stringResource(R.string.password)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )

                RetroButton(
                    enabled = !testing && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        testing = true
                        coroutineScope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    SubsonicClient(
                                        PersonalLibraryCredentials(
                                            serverUrl = serverUrl,
                                            username = username,
                                            password = password,
                                        )
                                    ).ping()
                                }
                            }

                            testing = false
                            result
                                .onSuccess {
                                    enabled = true
                                    Toast.makeText(context, connectedMessage, Toast.LENGTH_SHORT).show()
                                }
                                .onFailure {
                                    Toast
                                        .makeText(context, libraryErrorMessage, Toast.LENGTH_LONG)
                                        .show()
                                }
                        }
                    },
                ) {
                    Text(
                        if (testing) {
                            stringResource(R.string.personal_library_testing)
                        } else {
                            stringResource(R.string.personal_library_test_connection)
                        }
                    )
                }
                }
            }
        }

        RetroSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.personal_library_sync_title),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = stringResource(R.string.personal_library_sync_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.product_ux_view_sync_details),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    RetroToggle(
                        checked = showSyncDetails,
                        onCheckedChange = { showSyncDetails = it },
                    )
                }

                Text(
                    text = stringResource(R.string.personal_library_sync_all_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                RetroButton(
                    enabled =
                        !syncingAll &&
                            !syncingFavorites &&
                            !syncingPlaylists &&
                            !syncingHistory &&
                            !syncingRatings &&
                            serverUrl.isNotBlank() &&
                            username.isNotBlank() &&
                            password.isNotBlank(),
                    onClick = {
                        syncingAll = true
                        syncingFavorites = true
                        syncingPlaylists = true
                        syncingHistory = true
                        syncingRatings = true
                        catalogSyncSummary = ""
                        catalogSyncDetailed = ""
                        favoriteSyncSummary = ""
                        favoriteSyncDetailed = ""
                        playlistSyncSummary = ""
                        playlistSyncDetailed = ""
                        historySyncSummary = ""
                        historySyncDetailed = ""
                        ratingSyncSummary = ""
                        ratingSyncDetailed = ""
                        coroutineScope.launch {
                            val client = SubsonicClient(
                                PersonalLibraryCredentials(
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password,
                                )
                            )
                            val result = runCatching {
                                PersonalLibrarySync.syncAll(
                                    database = database,
                                    client = client,
                                    lastSyncedEpochMs = historySyncEpochMs,
                                )
                            }

                            syncingAll = false
                            syncingFavorites = false
                            syncingPlaylists = false
                            syncingHistory = false
                            syncingRatings = false
                            result
                                .onSuccess { syncResult ->
                                    val catalog = syncResult.catalog
                                    val favorites = syncResult.favorites
                                    val ratings = syncResult.ratings
                                    val playlists = syncResult.playlists
                                    val history = syncResult.history
                                    historySyncEpochMs = history.lastSyncedEpochMs
                                    catalogSyncDetailed = context.getString(
                                        R.string.personal_library_sync_catalog_summary,
                                        catalog.remoteAlbums,
                                        catalog.remoteSongs,
                                        catalog.importedSongs,
                                        catalog.updatedSongs,
                                    )
                                    catalogSyncSummary =
                                        context.getString(R.string.personal_library_sync_all_done)
                                    favoriteSyncDetailed = context.getString(
                                        R.string.personal_library_sync_summary,
                                        favorites.remoteFavorites,
                                        favorites.importedFavorites,
                                        favorites.updatedFavorites,
                                        favorites.pushedFavorites,
                                    )
                                    favoriteSyncSummary =
                                        context.getString(R.string.personal_library_sync_favorites_done)
                                    playlistSyncDetailed = context.getString(
                                        R.string.personal_library_sync_playlists_summary,
                                        playlists.remotePlaylists,
                                        playlists.importedPlaylists,
                                        playlists.updatedPlaylists,
                                        playlists.pushedPlaylists,
                                    )
                                    playlistSyncSummary =
                                        context.getString(R.string.personal_library_sync_playlists_done)
                                    historySyncDetailed = context.getString(
                                        R.string.personal_library_sync_history_summary,
                                        history.pulledPlays,
                                        history.pushedScrobbles,
                                        history.skippedEvents,
                                    )
                                    historySyncSummary =
                                        context.getString(R.string.personal_library_sync_history_done)
                                    ratingSyncDetailed = context.getString(
                                        R.string.personal_library_sync_ratings_summary,
                                        ratings.remoteRatings,
                                        ratings.importedRatings,
                                        ratings.pushedRatings,
                                    )
                                    ratingSyncSummary =
                                        context.getString(R.string.personal_library_sync_ratings_done)
                                }
                                .onFailure {
                                    Toast
                                        .makeText(context, libraryErrorMessage, Toast.LENGTH_LONG)
                                        .show()
                                }
                        }
                    },
                ) {
                    Text(
                        if (syncingAll) {
                            stringResource(R.string.personal_library_syncing)
                        } else {
                            stringResource(R.string.personal_library_sync_all)
                        }
                    )
                }

                PersonalLibrarySyncSummaryLine(
                    summary = catalogSyncSummary,
                    detailed = catalogSyncDetailed,
                    showDetails = showSyncDetails,
                )

                Text(
                    text = stringResource(R.string.personal_library_sync_ratings_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                RetroButton(
                    enabled = !syncingRatings && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        syncingRatings = true
                        ratingSyncSummary = ""
                        ratingSyncDetailed = ""
                        coroutineScope.launch {
                            val client = SubsonicClient(
                                PersonalLibraryCredentials(
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password,
                                )
                            )
                            val result = runCatching {
                                PersonalLibrarySync.syncRatings(database, client)
                            }

                            syncingRatings = false
                            result
                                .onSuccess {
                                    ratingSyncDetailed = context.getString(
                                        R.string.personal_library_sync_ratings_summary,
                                        it.remoteRatings,
                                        it.importedRatings,
                                        it.pushedRatings,
                                    )
                                    ratingSyncSummary =
                                        context.getString(R.string.personal_library_sync_ratings_done)
                                }
                                .onFailure {
                                    Toast
                                        .makeText(context, libraryErrorMessage, Toast.LENGTH_LONG)
                                        .show()
                                }
                        }
                    },
                ) {
                    Text(
                        if (syncingRatings) {
                            stringResource(R.string.personal_library_syncing)
                        } else {
                            stringResource(R.string.personal_library_sync_ratings)
                        }
                    )
                }

                PersonalLibrarySyncSummaryLine(
                    summary = ratingSyncSummary,
                    detailed = ratingSyncDetailed,
                    showDetails = showSyncDetails,
                )

                RetroButton(
                    enabled = !syncingFavorites && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        syncingFavorites = true
                        favoriteSyncSummary = ""
                        favoriteSyncDetailed = ""
                        coroutineScope.launch {
                            val client = SubsonicClient(
                                PersonalLibraryCredentials(
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password,
                                )
                            )
                            val result = runCatching {
                                PersonalLibrarySync.syncFavorites(database, client)
                            }

                            syncingFavorites = false
                            result
                                .onSuccess {
                                    favoriteSyncDetailed = context.getString(
                                        R.string.personal_library_sync_summary,
                                        it.remoteFavorites,
                                        it.importedFavorites,
                                        it.updatedFavorites,
                                        it.pushedFavorites,
                                    )
                                    favoriteSyncSummary =
                                        context.getString(R.string.personal_library_sync_favorites_done)
                                }
                                .onFailure {
                                    Toast
                                        .makeText(context, libraryErrorMessage, Toast.LENGTH_LONG)
                                        .show()
                                }
                        }
                    },
                ) {
                    Text(
                        if (syncingFavorites) {
                            stringResource(R.string.personal_library_syncing)
                        } else {
                            stringResource(R.string.personal_library_sync_favorites)
                        }
                    )
                }

                PersonalLibrarySyncSummaryLine(
                    summary = favoriteSyncSummary,
                    detailed = favoriteSyncDetailed,
                    showDetails = showSyncDetails,
                )

                Text(
                    text = stringResource(R.string.personal_library_sync_playlists_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                RetroButton(
                    enabled = !syncingPlaylists && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        syncingPlaylists = true
                        playlistSyncSummary = ""
                        playlistSyncDetailed = ""
                        coroutineScope.launch {
                            val client = SubsonicClient(
                                PersonalLibraryCredentials(
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password,
                                )
                            )
                            val result = runCatching {
                                PersonalLibrarySync.syncPlaylists(database, client)
                            }

                            syncingPlaylists = false
                            result
                                .onSuccess {
                                    playlistSyncDetailed = context.getString(
                                        R.string.personal_library_sync_playlists_summary,
                                        it.remotePlaylists,
                                        it.importedPlaylists,
                                        it.updatedPlaylists,
                                        it.pushedPlaylists,
                                    )
                                    playlistSyncSummary =
                                        context.getString(R.string.personal_library_sync_playlists_done)
                                }
                                .onFailure {
                                    Toast
                                        .makeText(context, libraryErrorMessage, Toast.LENGTH_LONG)
                                        .show()
                                }
                        }
                    },
                ) {
                    Text(
                        if (syncingPlaylists) {
                            stringResource(R.string.personal_library_syncing)
                        } else {
                            stringResource(R.string.personal_library_sync_playlists)
                        }
                    )
                }

                PersonalLibrarySyncSummaryLine(
                    summary = playlistSyncSummary,
                    detailed = playlistSyncDetailed,
                    showDetails = showSyncDetails,
                )

                Text(
                    text = stringResource(R.string.personal_library_sync_history_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                RetroButton(
                    enabled = !syncingHistory && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank(),
                    onClick = {
                        syncingHistory = true
                        historySyncSummary = ""
                        historySyncDetailed = ""
                        coroutineScope.launch {
                            val client = SubsonicClient(
                                PersonalLibraryCredentials(
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password,
                                )
                            )
                            val result = runCatching {
                                PersonalLibrarySync.syncPlayHistory(
                                    database = database,
                                    client = client,
                                    lastSyncedEpochMs = historySyncEpochMs,
                                )
                            }

                            syncingHistory = false
                            result
                                .onSuccess {
                                    historySyncEpochMs = it.lastSyncedEpochMs
                                    historySyncDetailed = context.getString(
                                        R.string.personal_library_sync_history_summary,
                                        it.pulledPlays,
                                        it.pushedScrobbles,
                                        it.skippedEvents,
                                    )
                                    historySyncSummary =
                                        context.getString(R.string.personal_library_sync_history_done)
                                }
                                .onFailure {
                                    Toast
                                        .makeText(context, libraryErrorMessage, Toast.LENGTH_LONG)
                                        .show()
                                }
                        }
                    },
                ) {
                    Text(
                        if (syncingHistory) {
                            stringResource(R.string.personal_library_syncing)
                        } else {
                            stringResource(R.string.personal_library_sync_history)
                        }
                    )
                }

                PersonalLibrarySyncSummaryLine(
                    summary = historySyncSummary,
                    detailed = historySyncDetailed,
                    showDetails = showSyncDetails,
                )
            }
        }

        RetroSurface(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.personal_library_search_title),
                    style = MaterialTheme.typography.titleMedium,
                )

                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    label = { Text(stringResource(R.string.personal_library_search_hint)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                RetroButton(
                    enabled = !searching && serverUrl.isNotBlank() && username.isNotBlank() && password.isNotBlank() && searchQuery.isNotBlank(),
                    onClick = {
                        searching = true
                        coroutineScope.launch {
                            val client = SubsonicClient(
                                PersonalLibraryCredentials(
                                    serverUrl = serverUrl,
                                    username = username,
                                    password = password,
                                )
                            )
                            val result = withContext(Dispatchers.IO) {
                                runCatching {
                                    client.search3(searchQuery.trim(), songCount = 20).song
                                }
                            }

                            searching = false
                            result
                                .onSuccess { searchResults = it }
                                .onFailure {
                                    Toast
                                        .makeText(context, libraryErrorMessage, Toast.LENGTH_LONG)
                                        .show()
                                }
                        }
                    },
                ) {
                    Text(
                        if (searching) {
                            stringResource(R.string.personal_library_searching)
                        } else {
                            stringResource(R.string.personal_library_search_button)
                        }
                    )
                }

                val client = SubsonicClient(
                    PersonalLibraryCredentials(
                        serverUrl = serverUrl,
                        username = username,
                        password = password,
                    )
                )
                searchResults.forEachIndexed { index, song ->
                    MediaMetadataListItem(
                        mediaMetadata = song.toRoofyMetadata(client),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                playerConnection?.playQueue(
                                    ListQueue(
                                        title = searchQuery,
                                        items = searchResults.map { it.toMediaItem(client) },
                                        startIndex = index,
                                    )
                                )
                            },
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.personal_library_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(80.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.personal_library)) },
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
    )
}

@Composable
private fun PersonalLibrarySyncSummaryLine(
    summary: String,
    detailed: String,
    showDetails: Boolean,
) {
    if (summary.isBlank()) return
    Text(
        text = if (showDetails && detailed.isNotBlank()) detailed else summary,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

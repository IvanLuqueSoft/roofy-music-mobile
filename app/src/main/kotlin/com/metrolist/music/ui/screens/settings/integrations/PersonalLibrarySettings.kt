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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.PersonalLibraryEnabledKey
import com.metrolist.music.constants.PersonalLibraryPasswordKey
import com.metrolist.music.constants.PersonalLibraryServerUrlKey
import com.metrolist.music.constants.PersonalLibraryUsernameKey
import com.metrolist.music.subsonic.PersonalLibraryCredentials
import com.metrolist.music.subsonic.SubsonicClient
import com.metrolist.music.subsonic.SubsonicSong
import com.metrolist.music.subsonic.toMediaItem
import com.metrolist.music.subsonic.toRoofyMetadata
import com.metrolist.music.playback.queues.ListQueue
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
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()

    var enabled by rememberPreference(PersonalLibraryEnabledKey, false)
    var serverUrl by rememberPreference(PersonalLibraryServerUrlKey, "")
    var username by rememberPreference(PersonalLibraryUsernameKey, "")
    var password by rememberPreference(PersonalLibraryPasswordKey, "")
    var testing by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var searching by rememberSaveable { mutableStateOf(false) }
    var searchResults by remember { mutableStateOf(emptyList<SubsonicSong>()) }

    val connectedMessage = stringResource(R.string.personal_library_connection_ok)
    val failedPrefix = stringResource(R.string.personal_library_connection_failed)

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
                                        .makeText(context, "$failedPrefix: ${it.message}", Toast.LENGTH_LONG)
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
                                        .makeText(context, "$failedPrefix: ${it.message}", Toast.LENGTH_LONG)
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

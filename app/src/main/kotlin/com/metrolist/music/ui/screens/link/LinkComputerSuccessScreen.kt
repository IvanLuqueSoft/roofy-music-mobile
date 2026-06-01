/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.link

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.device.DeviceSessionManager
import com.metrolist.music.constants.PersonalLibraryHistorySyncEpochMsKey
import com.metrolist.music.constants.PersonalLibraryEnabledKey
import com.metrolist.music.constants.PersonalLibraryPasswordKey
import com.metrolist.music.constants.PersonalLibraryServerUrlKey
import com.metrolist.music.constants.PersonalLibraryUsernameKey
import com.metrolist.music.subsonic.PersonalLibrarySync
import com.metrolist.music.subsonic.SubsonicClient
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.theme.RetroButton
import com.metrolist.music.ui.theme.RetroSurface
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class DesktopConnectMode {
    SYNC_LIBRARY,
    CONTROL_ONLY,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LinkComputerSuccessScreen(
    navController: NavController,
) {
    val context = LocalContext.current
    val database = LocalDatabase.current
    val coroutineScope = rememberCoroutineScope()
    var enabled by rememberPreference(PersonalLibraryEnabledKey, false)
    val serverUrl by rememberPreference(PersonalLibraryServerUrlKey, "")
    val username by rememberPreference(PersonalLibraryUsernameKey, "")
    val password by rememberPreference(PersonalLibraryPasswordKey, "")
    val sessionUi by DeviceSessionManager.uiState.collectAsStateWithLifecycle()
    var historySyncEpochMs by rememberPreference(PersonalLibraryHistorySyncEpochMsKey, 0L)
    var syncing by rememberSaveable { mutableStateOf(false) }
    var mode by rememberSaveable { mutableStateOf(DesktopConnectMode.SYNC_LIBRARY) }

    fun finish() {
        navController.navigateUp()
    }

    fun continueWithMode() {
        when (mode) {
            DesktopConnectMode.SYNC_LIBRARY -> {
                if (serverUrl.isBlank() || username.isBlank() || password.isBlank()) {
                    Toast.makeText(context, R.string.phone_link_pairing_invalid, Toast.LENGTH_LONG).show()
                    return
                }
                syncing = true
                enabled = true
                coroutineScope.launch {
                    val result =
                        withContext(Dispatchers.IO) {
                            runCatching {
                                val client =
                                    SubsonicClient(
                                        com.metrolist.music.subsonic.PersonalLibraryCredentials(
                                            serverUrl = serverUrl,
                                            username = username,
                                            password = password,
                                        ),
                                    )
                                PersonalLibrarySync.syncAll(
                                    database = database,
                                    client = client,
                                    lastSyncedEpochMs = historySyncEpochMs,
                                )
                            }
                        }
                    syncing = false
                    result
                        .onSuccess {
                            Toast.makeText(
                                context,
                                R.string.personal_library_sync_all_done,
                                Toast.LENGTH_SHORT,
                            ).show()
                            finish()
                        }
                        .onFailure {
                            Toast.makeText(
                                context,
                                R.string.phone_link_sync_failed,
                                Toast.LENGTH_LONG,
                            ).show()
                        }
                }
            }
            DesktopConnectMode.CONTROL_ONLY -> {
                enabled = false
                coroutineScope.launch {
                    context.dataStore.edit { settings ->
                        settings[PersonalLibraryEnabledKey] = false
                    }
                    if (sessionUi.session?.remote?.wsUrl?.isNotBlank() == true) {
                        DeviceSessionManager.setPlaybackTarget("computer")
                        Toast.makeText(context, R.string.phone_link_remote_ready, Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, R.string.phone_link_remote_unavailable, Toast.LENGTH_LONG).show()
                    }
                    finish()
                }
            }
        }
    }

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
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.phone_link_success_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = stringResource(R.string.phone_link_choose_mode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                ConnectModeRow(
                    body = stringResource(R.string.phone_link_mode_sync_body),
                    selected = mode == DesktopConnectMode.SYNC_LIBRARY,
                    title = stringResource(R.string.phone_link_mode_sync_title),
                    onSelect = { mode = DesktopConnectMode.SYNC_LIBRARY },
                )

                ConnectModeRow(
                    body = stringResource(R.string.phone_link_mode_control_body),
                    selected = mode == DesktopConnectMode.CONTROL_ONLY,
                    title = stringResource(R.string.phone_link_mode_control_title),
                    onSelect = { mode = DesktopConnectMode.CONTROL_ONLY },
                )

                RetroButton(
                    enabled = !syncing,
                    onClick = { continueWithMode() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        if (syncing) {
                            stringResource(R.string.personal_library_syncing)
                        } else {
                            stringResource(R.string.phone_link_continue)
                        },
                    )
                }

                RetroButton(
                    onClick = { finish() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(stringResource(R.string.phone_link_done))
                }
            }
        }

        Spacer(modifier = Modifier.height(72.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.phone_link_title)) },
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
private fun ConnectModeRow(
    title: String,
    body: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onSelect)
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Top,
    ) {
        RadioButton(selected = selected, onClick = onSelect)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

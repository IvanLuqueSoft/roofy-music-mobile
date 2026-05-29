/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings.integrations

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.DesktopImportEndpointUrlKey
import com.metrolist.music.constants.DesktopImportTokenKey
import com.metrolist.music.desktopimport.DesktopImportClient
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.theme.RetroButton
import com.metrolist.music.ui.theme.RetroSurface
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DesktopImportSettings(
    navController: NavController,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var endpointUrl by rememberPreference(DesktopImportEndpointUrlKey, "")
    var token by rememberPreference(DesktopImportTokenKey, "")
    var testing by rememberSaveable { mutableStateOf(false) }

    val connectedMessage = stringResource(R.string.desktop_import_connection_ok)
    val failedPrefix = stringResource(R.string.desktop_import_connection_failed)

    Column(
        modifier =
            Modifier
                .windowInsetsPadding(
                    LocalPlayerAwareWindowInsets.current.only(
                        WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom,
                    ),
                )
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
                    text = stringResource(R.string.desktop_import_settings_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = endpointUrl,
                    onValueChange = { endpointUrl = it.trim() },
                    label = { Text(stringResource(R.string.desktop_import_endpoint_url)) },
                    placeholder = { Text("https://example.trycloudflare.com") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                )

                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it.trim() },
                    label = { Text(stringResource(R.string.desktop_import_token)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                )

                RetroButton(
                    enabled = !testing && endpointUrl.isNotBlank() && token.isNotBlank(),
                    onClick = {
                        testing = true
                        coroutineScope.launch {
                            val result = DesktopImportClient.health(endpointUrl, token)
                            testing = false
                            result
                                .onSuccess {
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
                            stringResource(R.string.desktop_import_testing)
                        } else {
                            stringResource(R.string.desktop_import_test_connection)
                        },
                    )
                }
            }
        }

        Text(
            text = stringResource(R.string.desktop_import_privacy_note),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(modifier = Modifier.height(80.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.desktop_import)) },
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

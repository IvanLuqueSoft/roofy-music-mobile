/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.devices

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.constants.DesktopComputerNameKey
import com.metrolist.music.constants.DesktopImportEndpointUrlKey
import com.metrolist.music.constants.DesktopImportTokenKey
import com.metrolist.music.constants.DesktopRemoteControlTokenKey
import com.metrolist.music.constants.DesktopRemoteControlUrlKey
import com.metrolist.music.constants.DevicePlaybackTargetKey
import com.metrolist.music.device.DeviceSessionManager
import com.metrolist.music.desktopimport.DesktopConnect
import com.metrolist.music.desktopimport.DesktopHandoffClient
import com.metrolist.music.desktopimport.DesktopRemoteClient
import com.metrolist.music.desktopimport.HandoffPlayback
import com.metrolist.music.desktopimport.HandoffSnapshot
import com.metrolist.music.ui.component.BottomSheetState
import com.metrolist.music.utils.dataStore
import com.metrolist.music.utils.rememberPreference
import com.metrolist.music.ui.theme.RetroTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun ColumnScope.ListenOnSheet(
    onDismiss: () -> Unit,
    navController: NavController,
    playerBottomSheetState: BottomSheetState? = null,
) {
    val context = LocalContext.current
    val playerConnection = LocalPlayerConnection.current
    val lifecycleScope = LocalLifecycleOwner.current.lifecycleScope

    val endpointUrl by rememberPreference(DesktopImportEndpointUrlKey, "")
    val token by rememberPreference(DesktopImportTokenKey, "")
    val remoteControlUrl by rememberPreference(DesktopRemoteControlUrlKey, "")
    val remoteControlToken by rememberPreference(DesktopRemoteControlTokenKey, "")
    val computerName by rememberPreference(DesktopComputerNameKey, "")
    var playbackTarget by rememberPreference(DevicePlaybackTargetKey, "phone")
    val remoteState by DesktopRemoteClient.state.collectAsStateWithLifecycle()
    val sessionUi by DeviceSessionManager.uiState.collectAsStateWithLifecycle()
    val isPaired = sessionUi.isPaired
    val isRemoteReady = remoteControlUrl.isNotBlank()

    var desktopSnapshot by remember { mutableStateOf<HandoffSnapshot?>(null) }
    var desktopReachable by remember { mutableStateOf(false) }
    var desktopProbeError by remember { mutableStateOf<String?>(null) }
    var isProbing by remember(isPaired) { mutableStateOf(isPaired) }
    var isTransferring by remember { mutableStateOf(false) }

    val failedText = stringResource(R.string.listen_on_failed)
    val switchingText = stringResource(R.string.listen_on_switching)
    val nowPlayingOnComputer = stringResource(R.string.listen_on_now_playing_on_computer)
    val nowPlayingOnPhone = stringResource(R.string.listen_on_now_playing_on_phone)
    val remoteReady = stringResource(R.string.listen_on_remote_ready)
    val computerTitle = computerName.ifBlank { stringResource(R.string.listen_on_this_computer) }

    LaunchedEffect(sessionUi.playbackTarget) {
        playbackTarget = sessionUi.playbackTarget
    }

    LaunchedEffect(endpointUrl, token, isPaired) {
        if (!isPaired) {
            isProbing = false
            desktopSnapshot = null
            desktopReachable = false
            desktopProbeError = null
            return@LaunchedEffect
        }
        isProbing = true
        desktopProbeError = null

        val healthResult =
            withContext(Dispatchers.IO) {
                runCatching {
                    DesktopConnect.resolveLiveEndpoint(endpointUrl, token).getOrThrow()
                }
            }

        healthResult
            .onSuccess { liveEndpoint ->
                val normalizedStored = endpointUrl.trim().trimEnd('/')
                if (liveEndpoint != normalizedStored) {
                    context.dataStore.edit { settings ->
                        settings[DesktopImportEndpointUrlKey] = liveEndpoint
                    }
                }
                desktopReachable = true
                desktopProbeError = null

                val stateResult =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            DesktopHandoffClient.fetchState(liveEndpoint, token).getOrThrow()
                        }
                    }
                stateResult
                    .onSuccess { snapshot -> desktopSnapshot = snapshot }
                    .onFailure { desktopSnapshot = null }
            }
            .onFailure { error ->
                desktopSnapshot = null
                desktopReachable = false
                desktopProbeError =
                    (error as? IllegalStateException)?.message?.takeIf { it.isNotBlank() }
            }

        isProbing = false
    }

    val phoneHasMedia = playerConnection?.player?.currentMediaItem != null
    val phoneIsPlaying = playerConnection?.player?.isPlaying == true
    val desktopHasMedia = remoteState.track != null || desktopSnapshot?.nowPlaying != null
    val desktopIsPlaying =
        remoteState.isPlaying || desktopSnapshot?.playbackStatus.equals("playing", ignoreCase = true)

    val activeOnPhone =
        when {
            playbackTarget == "computer" -> false
            playbackTarget == "phone" -> true
            phoneIsPlaying -> true
            desktopIsPlaying -> false
            phoneHasMedia -> true
            desktopHasMedia -> false
            else -> true
        }

    val statusLine =
        when {
            isProbing -> stringResource(R.string.listen_on_checking)
            !isPaired -> stringResource(R.string.listen_on_connect_prompt)
            activeOnPhone && phoneHasMedia ->
                stringResource(R.string.listen_on_status_playing_here, stringResource(R.string.listen_on_this_phone))
            !activeOnPhone && (desktopHasMedia || isRemoteReady) ->
                stringResource(R.string.listen_on_status_playing_here, computerTitle)
            else -> stringResource(R.string.listen_on_status_pick_device)
        }

    Text(
        text = stringResource(R.string.listen_on_title),
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.SemiBold,
        color = RetroTokens.Text,
    )
    Spacer(modifier = Modifier.height(6.dp))
    Text(
        text = statusLine,
        style = MaterialTheme.typography.bodyMedium,
        color = RetroTokens.TextMuted,
    )
    Spacer(modifier = Modifier.height(20.dp))

    if (!isPaired) {
        ListenOnDeviceRow(
            title = stringResource(R.string.listen_on_this_phone),
            subtitle =
                if (phoneHasMedia) {
                    stringResource(R.string.listen_on_playing_here)
                } else {
                    stringResource(R.string.listen_on_ready)
                },
            iconRes = R.drawable.listen_on,
            isActive = true,
            enabled = true,
            onClick = onDismiss,
        )
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.listen_on_connect_desktop_stale_hint),
            style = MaterialTheme.typography.bodySmall,
            color = RetroTokens.TextMuted,
        )
        Spacer(modifier = Modifier.height(12.dp))
        FilledTonalButton(
            onClick = {
                onDismiss()
                navController.navigate("link_computer?scan=true") {
                    launchSingleTop = true
                }
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.listen_on_connect_button))
        }
        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = RetroTokens.BorderMuted)
        ListenOnCastSection(onDismiss = onDismiss)
        Spacer(modifier = Modifier.height(24.dp))
        return
    }

    if (isProbing) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                strokeWidth = 2.dp,
                color = RetroTokens.Active,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        return
    }

    ListenOnDeviceRow(
        title = stringResource(R.string.listen_on_this_phone),
        subtitle =
            when {
                activeOnPhone && phoneHasMedia -> stringResource(R.string.listen_on_playing_here)
                phoneHasMedia -> stringResource(R.string.listen_on_ready)
                else -> stringResource(R.string.listen_on_tap_to_play_here)
            },
        iconRes = R.drawable.listen_on,
        isActive = activeOnPhone,
        enabled = !isTransferring,
        onClick = {
            lifecycleScope.launch {
                withContext(NonCancellable) {
                    DeviceSessionManager.setPlaybackTarget("phone")
                }
                playbackTarget = "phone"
                Toast.makeText(context, nowPlayingOnPhone, Toast.LENGTH_SHORT).show()
                playerBottomSheetState?.collapseSoft()
                onDismiss()
            }
        },
    )

    Spacer(modifier = Modifier.height(8.dp))

    ListenOnDeviceRow(
        title = computerTitle,
        subtitle =
            when {
                playbackTarget == "computer" && remoteState.connected ->
                    stringResource(R.string.listen_on_playing_here)
                isRemoteReady && remoteState.connecting ->
                    stringResource(R.string.listen_on_checking)
                isRemoteReady && remoteState.errorMessage != null ->
                    remoteState.errorMessage ?: stringResource(R.string.listen_on_computer_unreachable)
                !desktopReachable && !isRemoteReady ->
                    desktopProbeError ?: stringResource(R.string.listen_on_computer_unreachable)
                activeOnPhone && !desktopHasMedia -> stringResource(R.string.listen_on_computer_idle)
                !activeOnPhone && desktopHasMedia -> stringResource(R.string.listen_on_playing_here)
                desktopHasMedia -> stringResource(R.string.listen_on_ready)
                phoneHasMedia -> stringResource(R.string.listen_on_handoff_to_computer)
                isRemoteReady -> remoteReady
                else -> stringResource(R.string.listen_on_tap_to_play_here)
            },
        iconRes = if (!activeOnPhone) R.drawable.cast_connected else R.drawable.cast,
        isActive = !activeOnPhone,
        enabled = !isTransferring && (desktopReachable || isRemoteReady),
        onClick = {
            if (!desktopReachable && !isRemoteReady) {
                Toast
                    .makeText(
                        context,
                        desktopProbeError ?: context.getString(R.string.listen_on_computer_unreachable),
                        Toast.LENGTH_LONG,
                    ).show()
                return@ListenOnDeviceRow
            }
            if (isRemoteReady) {
                lifecycleScope.launch {
                    playerConnection?.stopLocalPlayback()
                    withContext(NonCancellable) {
                        DeviceSessionManager.setPlaybackTarget("computer")
                    }
                    playbackTarget = "computer"
                    Toast.makeText(context, nowPlayingOnComputer, Toast.LENGTH_SHORT).show()
                    playerBottomSheetState?.collapseSoft()
                    onDismiss()
                }
                return@ListenOnDeviceRow
            } else if (!phoneHasMedia) {
                Toast.makeText(context, R.string.listen_on_nothing_on_phone, Toast.LENGTH_SHORT).show()
                return@ListenOnDeviceRow
            }
            if (!phoneHasMedia || desktopHasMedia || !desktopReachable) {
                Toast.makeText(context, nowPlayingOnComputer, Toast.LENGTH_SHORT).show()
                onDismiss()
                return@ListenOnDeviceRow
            }
            val connection = playerConnection ?: return@ListenOnDeviceRow
            isTransferring = true
            Toast.makeText(context, switchingText, Toast.LENGTH_SHORT).show()
            lifecycleScope.launch {
                val result =
                    withContext(Dispatchers.IO) {
                        runCatching {
                            HandoffPlayback.continueOnDesktop(
                                playerConnection = connection,
                                endpointUrl = endpointUrl,
                                token = token,
                            )
                        }
                    }
                isTransferring = false
                result
                    .onSuccess { refreshedEndpoint ->
                        withContext(NonCancellable) {
                            if (refreshedEndpoint != endpointUrl.trim().trimEnd('/')) {
                                context.dataStore.edit { settings ->
                                    settings[DesktopImportEndpointUrlKey] = refreshedEndpoint
                                }
                            }
                            DeviceSessionManager.setPlaybackTarget("computer")
                        }
                        playbackTarget = "computer"
                        playerConnection?.stopLocalPlayback()
                        Toast.makeText(context, nowPlayingOnComputer, Toast.LENGTH_SHORT).show()
                        onDismiss()
                    }
                    .onFailure { error ->
                        Toast
                            .makeText(
                                context,
                                error.message ?: failedText,
                                Toast.LENGTH_LONG,
                            ).show()
                    }
            }
        },
    )

    if (isTransferring) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = switchingText,
            style = MaterialTheme.typography.bodySmall,
            color = RetroTokens.TextMuted,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }

    Spacer(modifier = Modifier.height(16.dp))
    HorizontalDivider(color = RetroTokens.BorderMuted)
    Spacer(modifier = Modifier.height(12.dp))

    ListenOnCastSection(onDismiss = onDismiss)

    Spacer(modifier = Modifier.height(12.dp))

    Text(
        text = stringResource(R.string.listen_on_manage_devices),
        style = MaterialTheme.typography.bodyMedium,
        color = RetroTokens.TextMuted,
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .clickable {
                    onDismiss()
                    navController.navigate("link_computer") {
                        launchSingleTop = true
                    }
                }
                .padding(vertical = 10.dp, horizontal = 4.dp),
    )

    Spacer(modifier = Modifier.height(12.dp))
}

@Composable
internal fun ListenOnDeviceRow(
    title: String,
    subtitle: String,
    iconRes: Int,
    isActive: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val rowAlpha = if (enabled) 1f else 0.45f
    val shape = RoundedCornerShape(12.dp)
    val background =
        when {
            isActive -> RetroTokens.Active.copy(alpha = 0.14f)
            else -> Color.Transparent
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .alpha(rowAlpha)
                .clip(shape)
                .background(background)
                .clickable(enabled = enabled, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = if (isActive) RetroTokens.Active else RetroTokens.Text,
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
            )
        }
        if (isActive) {
            PlayingIndicator()
        }
    }
}

@Composable
private fun PlayingIndicator() {
    val transition = rememberInfiniteTransition(label = "listenOnPlaying")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(700, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "listenOnPlayingAlpha",
    )
    Box(
        modifier =
            Modifier
                .size(10.dp)
                .alpha(alpha)
                .clip(CircleShape)
                .background(RetroTokens.Active),
    )
}

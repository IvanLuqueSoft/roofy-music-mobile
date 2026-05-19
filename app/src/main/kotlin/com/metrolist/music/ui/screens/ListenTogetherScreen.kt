/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontFamily
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.metrolist.music.LocalListenTogetherManager
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.R
import com.metrolist.music.constants.AppBarHeight
import com.metrolist.music.constants.ListenTogetherInTopBarKey
import com.metrolist.music.constants.ListenTogetherUsernameKey
import com.metrolist.music.listentogether.ConnectionState
import com.metrolist.music.listentogether.JoinRequestPayload
import com.metrolist.music.listentogether.ListenTogetherEvent
import com.metrolist.music.listentogether.RoomRole
import com.metrolist.music.listentogether.SuggestionReceivedPayload
import com.metrolist.music.listentogether.UserInfo
import com.metrolist.music.ui.component.DefaultDialog
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.theme.RetroButton
import com.metrolist.music.ui.theme.RetroIconButton
import com.metrolist.music.ui.theme.RetroPanel
import com.metrolist.music.ui.theme.RetroProgressBar
import com.metrolist.music.ui.theme.RetroTextButton
import com.metrolist.music.ui.theme.RetroTokens
import com.metrolist.music.ui.utils.backToMain
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ListenTogetherScreen(
    navController: NavController,
    showTopBar: Boolean = false,
) {
    val context = LocalContext.current
    val listenTogetherManager = LocalListenTogetherManager.current
    val windowInsets = LocalPlayerAwareWindowInsets.current
    val joiningRoomTemplate = stringResource(R.string.joining_room)

    if (listenTogetherManager == null) {
        NotConfiguredContent()
        return
    }

    val connectionState by listenTogetherManager.connectionState.collectAsStateWithLifecycle()
    val roomState by listenTogetherManager.roomState.collectAsStateWithLifecycle()
    val userId by listenTogetherManager.userId.collectAsStateWithLifecycle()
    val pendingJoinRequests by listenTogetherManager.pendingJoinRequests.collectAsStateWithLifecycle()
    val pendingSuggestions by listenTogetherManager.pendingSuggestions.collectAsStateWithLifecycle()

    val (listenTogetherInTopBar) = rememberPreference(ListenTogetherInTopBarKey, defaultValue = true)
    val shouldShowTopBar = showTopBar || listenTogetherInTopBar

    var savedUsername by rememberPreference(ListenTogetherUsernameKey, "")
    var roomCodeInput by rememberSaveable { mutableStateOf("") }
    var usernameInput by rememberSaveable { mutableStateOf(savedUsername) }

    var isCreatingRoom by rememberSaveable { mutableStateOf(false) }
    var isJoiningRoom by rememberSaveable { mutableStateOf(false) }
    var joinErrorMessage by rememberSaveable { mutableStateOf<String?>(null) }

    var selectedUserForMenu by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedUsername by rememberSaveable { mutableStateOf<String?>(null) }

    val waitingForApprovalText = stringResource(R.string.waiting_for_approval)
    val invalidRoomCodeText = stringResource(R.string.invalid_room_code)
    val joinRequestDeniedText = stringResource(R.string.join_request_denied)

    LaunchedEffect(savedUsername) {
        if (usernameInput.isBlank() && savedUsername.isNotBlank()) {
            usernameInput = savedUsername
        }
    }

    LaunchedEffect(listenTogetherManager) {
        listenTogetherManager.events.collect { event ->
            when (event) {
                is ListenTogetherEvent.JoinRejected -> {
                    val reason = event.reason
                    joinErrorMessage =
                        when {
                            reason.isNullOrBlank() -> joinRequestDeniedText
                            reason.contains("invalid", ignoreCase = true) -> invalidRoomCodeText
                            else -> "$joinRequestDeniedText: $reason"
                        }
                    isJoiningRoom = false
                    isCreatingRoom = false
                }

                is ListenTogetherEvent.JoinApproved -> {
                    isJoiningRoom = false
                    joinErrorMessage = null
                }

                is ListenTogetherEvent.RoomCreated -> {
                    isCreatingRoom = false
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clip = android.content.ClipData.newPlainText("ListenTogetherRoom", event.roomCode)
                    clipboard.setPrimaryClip(clip)
                }

                else -> {}
            }
        }
    }

    val isInRoom = listenTogetherManager.isInRoom
    val isHost = roomState?.hostId == userId

    if (selectedUserForMenu != null && selectedUsername != null) {
        UserActionDialog(
            username = selectedUsername ?: "",
            onKick = {
                selectedUserForMenu?.let {
                    listenTogetherManager.kickUser(it, "Removed by host")
                }
                selectedUserForMenu = null
                selectedUsername = null
            },
            onPermanentKick = {
                selectedUserForMenu?.let { userId ->
                    selectedUsername?.let { username ->
                        listenTogetherManager.blockUser(username)
                        listenTogetherManager.kickUser(userId, R.string.user_blocked_by_host.toString())
                    }
                }
                selectedUserForMenu = null
                selectedUsername = null
            },
            onTransferOwnership = {
                selectedUserForMenu?.let {
                    listenTogetherManager.transferHost(it)
                }
                selectedUserForMenu = null
                selectedUsername = null
            },
            onDismiss = {
                selectedUserForMenu = null
                selectedUsername = null
            },
        )
    }

    val lazyListState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val scrollToTop = backStackEntry?.savedStateHandle?.getStateFlow("scrollToTop", false)?.collectAsStateWithLifecycle()

    LaunchedEffect(scrollToTop?.value) {
        if (scrollToTop?.value == true) {
            lazyListState.animateScrollToItem(0)
            backStackEntry?.savedStateHandle?.set("scrollToTop", false)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier =
            Modifier
                .fillMaxSize()
                .background(RetroTokens.Background)
                .imePadding(),
        contentPadding =
            PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = windowInsets.asPaddingValues().calculateTopPadding() + 16.dp,
                bottom = windowInsets.asPaddingValues().calculateBottomPadding() + 16.dp + AppBarHeight,
            ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            HeaderSection(isInRoom = isInRoom)
        }

        item {
            ConnectionStatusCard(
                connectionState = connectionState,
                onConnect = { listenTogetherManager.connect() },
                onDisconnect = { listenTogetherManager.disconnect() },
                onReconnect = { listenTogetherManager.forceReconnect() },
            )
        }

        if (connectionState == ConnectionState.CONNECTED && !isInRoom) {
            item {
                Text(
                    text = stringResource(R.string.listen_together_background_disconnect_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = RetroTokens.TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (isInRoom) {
            roomState?.let { room ->
                item {
                    RoomStatusCard(
                        roomCode = room.roomCode,
                        isHost = isHost,
                        context = context,
                    )
                }

                val connectedUsers = room.users.filter { it.isConnected }
                val currentUserIdValue = userId ?: ""
                item {
                    ConnectedUsersSection(
                        users = connectedUsers,
                        isHost = isHost,
                        currentUserId = currentUserIdValue,
                        onUserClick = { clickedUserId, username ->
                            if (isHost && clickedUserId != currentUserIdValue) {
                                selectedUserForMenu = clickedUserId
                                selectedUsername = username
                            }
                        },
                    )
                }

                if (isHost && pendingJoinRequests.isNotEmpty()) {
                    item {
                        PendingJoinRequestsSection(
                            requests = pendingJoinRequests,
                            onApprove = { listenTogetherManager.approveJoin(it) },
                            onReject = { listenTogetherManager.rejectJoin(it, "Rejected by host") },
                        )
                    }
                }

                if (isHost && pendingSuggestions.isNotEmpty()) {
                    item {
                        PendingSuggestionsSection(
                            suggestions = pendingSuggestions,
                            onApprove = { listenTogetherManager.approveSuggestion(it) },
                            onReject = { listenTogetherManager.rejectSuggestion(it, "Rejected by host") },
                        )
                    }
                }

                item {
                    RetroButton(
                        onClick = { listenTogetherManager.leaveRoom() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.logout),
                            contentDescription = null,
                            tint = RetroTokens.Text,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            stringResource(R.string.leave_room).uppercase(),
                            color = RetroTokens.Text,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            }
        } else {
            item {
                JoinCreateRoomSection(
                    usernameInput = usernameInput,
                    onUsernameChange = { usernameInput = it },
                    roomCodeInput = roomCodeInput,
                    onRoomCodeChange = { roomCodeInput = it },
                    savedUsername = savedUsername,
                    isJoiningRoom = isJoiningRoom,
                    joinErrorMessage = joinErrorMessage,
                    waitingForApprovalText = waitingForApprovalText,
                    bringIntoViewRequester = bringIntoViewRequester,
                    onCreateRoom = {
                        val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                        val finalUsername = username.trim()
                        if (finalUsername.isNotBlank()) {
                            savedUsername = finalUsername
                            Toast.makeText(context, R.string.creating_room, Toast.LENGTH_SHORT).show()
                            isCreatingRoom = true
                            isJoiningRoom = false
                            joinErrorMessage = null
                            listenTogetherManager.connect()
                            listenTogetherManager.createRoom(finalUsername)
                        } else {
                            Toast.makeText(context, R.string.error_username_empty, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onJoinRoom = {
                        val username = usernameInput.takeIf { it.isNotBlank() } ?: savedUsername
                        val finalUsername = username.trim()
                        if (finalUsername.isNotBlank()) {
                            savedUsername = finalUsername
                            Toast
                                .makeText(
                                    context,
                                    String.format(joiningRoomTemplate, roomCodeInput),
                                    Toast.LENGTH_SHORT,
                                ).show()
                            isJoiningRoom = true
                            isCreatingRoom = false
                            joinErrorMessage = null
                            listenTogetherManager.connect()
                            listenTogetherManager.joinRoom(roomCodeInput, finalUsername)
                        } else {
                            Toast.makeText(context, R.string.error_username_empty, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onFieldFocused = {
                        coroutineScope.launch {
                            bringIntoViewRequester.bringIntoView()
                        }
                    },
                )
            }
        }

        item {
            SettingsLinkRow(
                onClick = { navController.navigate("settings/integrations/listen_together") },
            )
        }
    }

    if (shouldShowTopBar) {
        TopAppBar(
            title = { Text(stringResource(R.string.together)) },
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
}

@Composable
private fun NotConfiguredContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(RetroTokens.Panel)
                    .border(1.dp, RetroTokens.Border, RoundedCornerShape(0.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.group),
                    contentDescription = null,
                    tint = RetroTokens.TextSoft,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.listen_together),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = RetroTokens.Text,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.listen_together_not_configured),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = RetroTokens.TextMuted,
            )
        }
    }
}

@Composable
private fun HeaderSection(isInRoom: Boolean = false) {
    if (isInRoom) return

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(RetroTokens.Panel)
                .border(1.dp, RetroTokens.Border, RoundedCornerShape(0.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.group_outlined),
                contentDescription = null,
                tint = RetroTokens.TextSoft,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.listen_together),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = RetroTokens.Text,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = stringResource(R.string.listen_together_description),
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = RetroTokens.TextMuted,
        )
    }
}

@Composable
private fun ConnectionStatusCard(
    connectionState: ConnectionState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
) {
    RetroPanel(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize(
                    animationSpec =
                        spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow,
                        ),
                ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            color =
                                when (connectionState) {
                                    ConnectionState.CONNECTED -> RetroTokens.BorderBright
                                    ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> RetroTokens.TextSoft
                                    ConnectionState.ERROR -> RetroTokens.TextMuted
                                    ConnectionState.DISCONNECTED -> RetroTokens.BorderDark
                                },
                        ),
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text =
                        when (connectionState) {
                            ConnectionState.CONNECTED -> "STATUS: CONNECTED"
                            ConnectionState.CONNECTING -> "STATUS: CONNECTING..."
                            ConnectionState.RECONNECTING -> "STATUS: RECONNECTING..."
                            ConnectionState.ERROR -> "STATUS: ERROR"
                            ConnectionState.DISCONNECTED -> "STATUS: DISCONNECTED"
                        },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color =
                        when (connectionState) {
                            ConnectionState.CONNECTED -> RetroTokens.TextHot
                            ConnectionState.CONNECTING, ConnectionState.RECONNECTING -> RetroTokens.TextSoft
                            ConnectionState.ERROR -> RetroTokens.TextMuted
                            ConnectionState.DISCONNECTED -> RetroTokens.TextDim
                        },
                )
            }

            if (connectionState == ConnectionState.CONNECTING || connectionState == ConnectionState.RECONNECTING) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "[..........]",
                    style = MaterialTheme.typography.labelSmall,
                    color = RetroTokens.TextSoft,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (connectionState == ConnectionState.DISCONNECTED || connectionState == ConnectionState.ERROR) {
                    RetroButton(
                        onClick = onConnect,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.link),
                            contentDescription = null,
                            tint = RetroTokens.TextSoft,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text("CONNECT", style = MaterialTheme.typography.labelMedium, color = RetroTokens.Text)
                    }
                } else {
                    RetroButton(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("DISCONNECT", style = MaterialTheme.typography.labelMedium, color = RetroTokens.Text)
                    }
                    RetroButton(
                        onClick = onReconnect,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("RECONNECT", style = MaterialTheme.typography.labelMedium, color = RetroTokens.Text)
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomStatusCard(
    roomCode: String,
    isHost: Boolean,
    context: Context,
) {
    RetroPanel(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(R.string.room_code).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                color = RetroTokens.TextMuted,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .background(RetroTokens.Background)
                    .border(1.dp, RetroTokens.Border, RoundedCornerShape(0.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                Text(
                    text = roomCode,
                    style = MaterialTheme.typography.titleLarge,
                    color = RetroTokens.TextHot,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 4.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text =
                    if (isHost) {
                        "[ HOST ]"
                    } else {
                        "[ GUEST ]"
                    },
                style = MaterialTheme.typography.bodySmall,
                color = RetroTokens.TextMuted,
                textAlign = TextAlign.Center,
            )

            if (isHost) {
                Spacer(modifier = Modifier.height(12.dp))
                val inviteLink =
                    remember(roomCode) {
                        "https://metrolist.meowery.eu/listen?code=$roomCode"
                    }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    RetroButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Listen Together Link", inviteLink)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.link),
                            contentDescription = stringResource(R.string.copy_link),
                            tint = RetroTokens.TextSoft,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("[ COPY ]", style = MaterialTheme.typography.labelSmall, color = RetroTokens.Text)
                    }

                    RetroButton(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("Room Code", roomCode)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, R.string.copied_to_clipboard, Toast.LENGTH_SHORT).show()
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.content_copy),
                            contentDescription = stringResource(R.string.copy_code),
                            tint = RetroTokens.TextSoft,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("[ SHARE ]", style = MaterialTheme.typography.labelSmall, color = RetroTokens.Text)
                    }
                }
            }
        }
    }
}

@Composable
private fun ConnectedUsersSection(
    users: List<UserInfo>,
    isHost: Boolean,
    currentUserId: String,
    onUserClick: (String, String) -> Unit,
) {
    RetroPanel(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = ">${stringResource(R.string.connected_users).uppercase()} (${users.size})",
                style = MaterialTheme.typography.labelSmall,
                color = RetroTokens.TextSoft,
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                users.forEach { user ->
                    UserAvatar(
                        user = user,
                        isCurrentUser = user.userId == currentUserId,
                        isClickable = isHost && user.userId != currentUserId,
                        onClick = { onUserClick(user.userId, user.username) },
                    )
                }
            }
        }
    }
}

@Composable
private fun UserAvatar(
    user: UserInfo,
    isCurrentUser: Boolean,
    isClickable: Boolean,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier =
            Modifier
                .width(64.dp)
                .clickable(enabled = isClickable, onClick = onClick),
    ) {
        Box(
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        when {
                            user.isHost -> RetroTokens.Panel2
                            isCurrentUser -> RetroTokens.Panel2
                            else -> RetroTokens.Background
                        },
                    )
                    .border(
                        1.dp,
                        when {
                            user.isHost -> RetroTokens.BorderBright
                            isCurrentUser -> RetroTokens.Border
                            else -> RetroTokens.BorderDark
                        },
                        RoundedCornerShape(0.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = user.username.take(1).uppercase(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = RetroTokens.TextSoft,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                )
            }

            if (user.isHost) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 2.dp, y = 2.dp)
                        .background(RetroTokens.Background)
                        .border(1.dp, RetroTokens.BorderMuted)
                        .padding(horizontal = 2.dp, vertical = 0.dp),
                ) {
                    Text(
                        text = "[H]",
                        style = MaterialTheme.typography.labelSmall,
                        color = RetroTokens.TextSoft,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = user.username,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isCurrentUser) FontWeight.Bold else FontWeight.Medium,
            color = if (user.isHost) RetroTokens.TextHot else RetroTokens.Text,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PendingJoinRequestsSection(
    requests: List<JoinRequestPayload>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    RetroPanel(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = ">${stringResource(R.string.listen_together_join_requests).uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = RetroTokens.TextSoft,
            )
            Spacer(modifier = Modifier.height(12.dp))

            requests.forEach { request ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(RetroTokens.Background)
                            .border(1.dp, RetroTokens.BorderMuted, RoundedCornerShape(0.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = request.username.take(1).uppercase(),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = RetroTokens.TextSoft,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = request.username,
                        style = MaterialTheme.typography.bodyMedium,
                        color = RetroTokens.Text,
                        modifier = Modifier.weight(1f),
                    )
                    RetroTextButton(
                        text = "[ Y ]",
                        onClick = { onApprove(request.userId) },
                    )
                    Spacer(Modifier.width(4.dp))
                    RetroTextButton(
                        text = "[ N ]",
                        onClick = { onReject(request.userId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PendingSuggestionsSection(
    suggestions: List<SuggestionReceivedPayload>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit,
) {
    RetroPanel(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = ">${stringResource(R.string.pending_suggestions).uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = RetroTokens.TextSoft,
            )
            Spacer(modifier = Modifier.height(12.dp))

            suggestions.forEach { suggestion ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.queue_music),
                        contentDescription = null,
                        tint = RetroTokens.TextSoft,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = suggestion.trackInfo.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = RetroTokens.Text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = suggestion.fromUsername,
                            style = MaterialTheme.typography.labelSmall,
                            color = RetroTokens.TextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    RetroTextButton(
                        text = "[ Y ]",
                        onClick = { onApprove(suggestion.suggestionId) },
                    )
                    Spacer(Modifier.width(4.dp))
                    RetroTextButton(
                        text = "[ N ]",
                        onClick = { onReject(suggestion.suggestionId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun JoinCreateRoomSection(
    usernameInput: String,
    onUsernameChange: (String) -> Unit,
    roomCodeInput: String,
    onRoomCodeChange: (String) -> Unit,
    savedUsername: String,
    isJoiningRoom: Boolean,
    joinErrorMessage: String?,
    waitingForApprovalText: String,
    bringIntoViewRequester: BringIntoViewRequester,
    onCreateRoom: () -> Unit,
    onJoinRoom: () -> Unit,
    onFieldFocused: () -> Unit = {},
) {
    RetroPanel(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedTextField(
                value = usernameInput,
                onValueChange = onUsernameChange,
                label = { Text(stringResource(R.string.username)) },
                placeholder = { Text(stringResource(R.string.enter_username)) },
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.person),
                        null,
                        tint = RetroTokens.TextSoft,
                    )
                },
                trailingIcon = {
                    if (usernameInput.isNotBlank()) {
                        IconButton(onClick = { onUsernameChange("") }) {
                            Icon(painterResource(R.drawable.close), null, tint = RetroTokens.TextSoft)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(0.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RetroTokens.BorderBright,
                        unfocusedBorderColor = RetroTokens.BorderMuted,
                        focusedContainerColor = RetroTokens.Background,
                        unfocusedContainerColor = RetroTokens.Background,
                        focusedLabelColor = RetroTokens.TextSoft,
                        unfocusedLabelColor = RetroTokens.TextMuted,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .onFocusChanged { if (it.isFocused) onFieldFocused() },
            )

            OutlinedTextField(
                value = roomCodeInput,
                onValueChange = { if (it.length <= 8) onRoomCodeChange(it.uppercase()) },
                label = { Text(stringResource(R.string.room_code)) },
                placeholder = { Text(stringResource(R.string.enter_room_code)) },
                leadingIcon = {
                    Icon(
                        painterResource(R.drawable.group),
                        null,
                        tint = RetroTokens.TextSoft,
                    )
                },
                trailingIcon = {
                    if (roomCodeInput.isNotBlank()) {
                        IconButton(onClick = { onRoomCodeChange("") }) {
                            Icon(painterResource(R.drawable.close), null, tint = RetroTokens.TextSoft)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(0.dp),
                colors =
                    OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = RetroTokens.BorderBright,
                        unfocusedBorderColor = RetroTokens.BorderMuted,
                        focusedContainerColor = RetroTokens.Background,
                        unfocusedContainerColor = RetroTokens.Background,
                        focusedLabelColor = RetroTokens.TextSoft,
                        unfocusedLabelColor = RetroTokens.TextMuted,
                    ),
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .bringIntoViewRequester(bringIntoViewRequester)
                        .onFocusChanged { if (it.isFocused) onFieldFocused() },
            )

            AnimatedVisibility(
                visible = isJoiningRoom,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetroTokens.Panel2)
                        .border(1.dp, RetroTokens.BorderMuted, RoundedCornerShape(0.dp))
                        .padding(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "[ ... ]",
                            style = MaterialTheme.typography.labelSmall,
                            color = RetroTokens.TextSoft,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = waitingForApprovalText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = RetroTokens.TextSoft,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = joinErrorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(RetroTokens.Background)
                        .border(1.dp, RetroTokens.BorderMuted, RoundedCornerShape(0.dp))
                        .padding(12.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "[ ! ]",
                            style = MaterialTheme.typography.labelSmall,
                            color = RetroTokens.TextMuted,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = joinErrorMessage ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RetroTokens.TextMuted,
                        )
                    }
                }
            }

            val hasUsername = usernameInput.trim().isNotBlank() || savedUsername.isNotBlank()
            val hasRoomCode = roomCodeInput.length == 8

            AnimatedVisibility(visible = hasUsername && !hasRoomCode) {
                RetroButton(
                    onClick = onCreateRoom,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasUsername,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.add),
                        contentDescription = null,
                        tint = RetroTokens.TextSoft,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("CREATE ROOM", style = MaterialTheme.typography.labelMedium, color = RetroTokens.Text)
                }
            }

            AnimatedVisibility(visible = hasUsername && hasRoomCode) {
                RetroButton(
                    onClick = onJoinRoom,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = hasUsername && hasRoomCode,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.login),
                        contentDescription = null,
                        tint = RetroTokens.TextSoft,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("JOIN ROOM", style = MaterialTheme.typography.labelMedium, color = RetroTokens.Text)
                }
            }
        }
    }
}

@Composable
private fun SettingsLinkRow(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(RetroTokens.Panel)
            .border(1.dp, RetroTokens.Border, RoundedCornerShape(0.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = ">",
            color = RetroTokens.TextMuted,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.width(20.dp),
        )
        Text(
            text = stringResource(R.string.settings).uppercase(),
            style = MaterialTheme.typography.bodyMedium,
            color = RetroTokens.Text,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = stringResource(R.string.listen_together_settings_desc),
            style = MaterialTheme.typography.bodySmall,
            color = RetroTokens.TextMuted,
        )
    }
}

@Composable
private fun UserActionDialog(
    username: String,
    onKick: () -> Unit,
    onPermanentKick: () -> Unit,
    onTransferOwnership: () -> Unit,
    onDismiss: () -> Unit,
) {
    DefaultDialog(
        onDismiss = onDismiss,
        icon = {
            Icon(
                painter = painterResource(R.drawable.group),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
                tint = RetroTokens.TextSoft,
            )
        },
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.manage_user).uppercase(),
                    color = RetroTokens.Text,
                )
                Text(
                    text = username,
                    style = MaterialTheme.typography.bodyMedium,
                    color = RetroTokens.TextMuted,
                )
            }
        },
        buttons = {
            RetroTextButton(
                text = stringResource(android.R.string.cancel),
                onClick = onDismiss,
            )
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onKick)
                    .background(RetroTokens.Background)
                    .border(1.dp, RetroTokens.BorderMuted, RoundedCornerShape(0.dp))
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = RetroTokens.TextMuted,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.kick_user).uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = RetroTokens.Text,
                        )
                        Text(
                            text = stringResource(R.string.kick_user_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = RetroTokens.TextMuted,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPermanentKick)
                    .background(RetroTokens.Background)
                    .border(1.dp, RetroTokens.BorderMuted, RoundedCornerShape(0.dp))
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.close),
                        contentDescription = null,
                        tint = RetroTokens.TextMuted,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.permanently_kick_user).uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = RetroTokens.Text,
                        )
                        Text(
                            text = stringResource(R.string.permanently_kick_user_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = RetroTokens.TextMuted,
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onTransferOwnership)
                    .background(RetroTokens.Background)
                    .border(1.dp, RetroTokens.BorderMuted, RoundedCornerShape(0.dp))
                    .padding(12.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.crown),
                        contentDescription = null,
                        tint = RetroTokens.TextSoft,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.transfer_ownership).uppercase(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = RetroTokens.Text,
                        )
                        Text(
                            text = stringResource(R.string.transfer_ownership_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = RetroTokens.TextMuted,
                        )
                    }
                }
            }
        }
    }
}

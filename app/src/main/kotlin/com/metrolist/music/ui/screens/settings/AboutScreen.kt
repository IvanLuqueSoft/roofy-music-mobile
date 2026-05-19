/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import com.metrolist.innertube.models.WatchEndpoint
import com.metrolist.music.BuildConfig
import com.metrolist.music.LocalPlayerAwareWindowInsets
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.IconButton
import com.metrolist.music.ui.theme.RetroArtwork
import com.metrolist.music.ui.theme.RetroButton
import com.metrolist.music.ui.theme.RetroIconButton
import com.metrolist.music.ui.theme.RetroPanel
import com.metrolist.music.ui.theme.RetroSectionHeader
import com.metrolist.music.ui.theme.RetroTokens
import com.metrolist.music.ui.utils.backToMain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

private data class Contributor(
    val name: String,
    val roleRes: Int,
    val githubHandle: String,
    val avatarUrl: String = "https://github.com/$githubHandle.png",
    val githubUrl: String = "https://github.com/$githubHandle",
    val favoriteSongVideoId: String? = null
)

private data class CommunityLink(
    val labelRes: Int,
    val iconRes: Int,
    val url: String
)

private val originalProject = Contributor(
    name = "Mo Agamy",
    roleRes = R.string.credits_original_project,
    githubHandle = "mostafaalagamy",
    favoriteSongVideoId = "Mh2JWGWvy_Y"
)

private val communityLinks = listOf(
    // TODO: Replace with your own Discord server invite link, or remove this entry
    // CommunityLink(R.string.credits_discord, R.drawable.discord, "https://discord.com/invite/YOUR_INVITE"),
    // TODO: Replace with your own Telegram channel link, or remove this entry
    // CommunityLink(R.string.credits_telegram, R.drawable.telegram, "https://t.me/your_channel"),
    CommunityLink(R.string.credits_view_repo, R.drawable.github, "https://github.com/IvanLuqueSoft/roofy-music-mobile"),
    CommunityLink(R.string.credits_license_name, R.drawable.info, "https://github.com/IvanLuqueSoft/roofy-music-mobile/blob/main/LICENSE")
)

private fun handleEasterEggClick(
    clickCount: Int,
    favoriteSongVideoId: String?,
    coroutineScope: CoroutineScope,
    snackbarHostState: SnackbarHostState,
    playerConnection: PlayerConnection?,
    wannaPlayStr: String,
    yeahStr: String,
    onCountUpdate: (Int) -> Unit
) {
    if (favoriteSongVideoId != null) {
        val newCount = clickCount + 1
        onCountUpdate(newCount)
        if (newCount >= 3) {
            onCountUpdate(0)
            coroutineScope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = wannaPlayStr,
                    actionLabel = yeahStr,
                    duration = SnackbarDuration.Short
                )
                if (result == SnackbarResult.ActionPerformed) {
                    playerConnection?.playQueue(YouTubeQueue(WatchEndpoint(videoId = favoriteSongVideoId)))
                }
            }
        }
    }
}

@Composable
private fun ContributorAvatar(
    avatarUrl: String,
    sizeDp: Int,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    onClick: (() -> Unit)? = null
) {
    val fallback = painterResource(R.drawable.small_icon)
    Box(
        modifier = modifier
            .size(sizeDp.dp)
            .background(RetroTokens.Background)
            .border(RetroTokens.BorderWidth, RetroTokens.BorderMuted, RoundedCornerShape(RetroTokens.Radius))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = avatarUrl,
            contentDescription = contentDescription,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
            placeholder = fallback,
            fallback = fallback,
            error = fallback,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navController: NavController,
) {
    val uriHandler = LocalUriHandler.current
    val playerConnection = LocalPlayerConnection.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val wannaPlayStr = stringResource(R.string.wanna_play_favorite_song)
    val yeahStr = stringResource(R.string.yeah)

    val windowInsets = LocalPlayerAwareWindowInsets.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            Modifier.windowInsetsPadding(
                windowInsets.only(WindowInsetsSides.Top)
            )
        )

        Spacer(Modifier.height(16.dp))

        // App Header Section
        RetroPanel(
            modifier = Modifier.fillMaxWidth(),
            strong = true
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.small_icon),
                    contentDescription = stringResource(R.string.roofy_music),
                    colorFilter = ColorFilter.tint(RetroTokens.TextSoft),
                    modifier = Modifier.size(48.dp)
                )

                Spacer(Modifier.width(16.dp))

                Column {
                    Text(
                        text = "ROOFY MUSIC",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = RetroTokens.TextHot,
                    )

                    Spacer(Modifier.height(6.dp))

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "[ ${BuildConfig.VERSION_NAME} ]",
                            style = MaterialTheme.typography.labelSmall,
                            color = RetroTokens.TextMuted,
                        )
                        Text(
                            text = "[ ${BuildConfig.ARCHITECTURE.uppercase()} ]",
                            style = MaterialTheme.typography.labelSmall,
                            color = RetroTokens.TextMuted,
                        )
                        if (BuildConfig.DEBUG) {
                            Text(
                                text = "[ DEBUG ]",
                                style = MaterialTheme.typography.labelSmall,
                                color = RetroTokens.TextMuted,
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Original Project Attribution
        RetroPanel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    var origClickCount by remember(originalProject.name) { mutableIntStateOf(0) }

                    ContributorAvatar(
                        avatarUrl = originalProject.avatarUrl,
                        sizeDp = 48,
                        contentDescription = originalProject.name,
                        onClick = {
                            handleEasterEggClick(
                                clickCount = origClickCount,
                                favoriteSongVideoId = originalProject.favoriteSongVideoId,
                                coroutineScope = coroutineScope,
                                snackbarHostState = snackbarHostState,
                                playerConnection = playerConnection,
                                wannaPlayStr = wannaPlayStr,
                                yeahStr = yeahStr,
                                onCountUpdate = { origClickCount = it }
                            )
                        }
                    )

                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = originalProject.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = RetroTokens.Text,
                        )
                        Text(
                            text = stringResource(R.string.credits_original_project),
                            style = MaterialTheme.typography.bodySmall,
                            color = RetroTokens.TextMuted,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                RetroButton(
                    onClick = { uriHandler.openUri("https://github.com/mostafaalagamy") },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        painterResource(R.drawable.github),
                        contentDescription = null,
                        tint = RetroTokens.TextSoft,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        stringResource(R.string.view_original_repo).uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = RetroTokens.Text,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Community & Info
        RetroSectionHeader(
            title = stringResource(R.string.community_and_info),
        )

        RetroPanel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                communityLinks.forEachIndexed { index, link ->
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
                            .clickable { uriHandler.openUri(link.url) }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = ">",
                            style = MaterialTheme.typography.bodyMedium,
                            color = RetroTokens.TextMuted,
                            modifier = Modifier.width(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(link.labelRes),
                                style = MaterialTheme.typography.bodyMedium,
                                color = RetroTokens.Text,
                            )
                            if (link.labelRes == R.string.credits_license_name) {
                                Text(
                                    text = stringResource(R.string.credits_license_desc),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = RetroTokens.TextMuted,
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(32.dp))

        Text(
            text = stringResource(R.string.stands_with_palestine),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = RetroTokens.TextMuted,
        )

        Spacer(Modifier.height(48.dp))
    }

    TopAppBar(
        title = { Text(stringResource(R.string.about)) },
        navigationIcon = {
            IconButton(
                onClick = navController::navigateUp,
                onLongClick = navController::backToMain,
            ) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = stringResource(R.string.cd_back),
                )
            }
        }
    )

    Box(Modifier.fillMaxSize()) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(windowInsets.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal))
        )
    }
}

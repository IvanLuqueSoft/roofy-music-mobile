/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 *
 * Performance optimized MiniPlayer - prevents unnecessary recomposition
 */

package com.metrolist.music.ui.player

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.Stable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.Player
import coil3.compose.AsyncImage
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.device.DeviceSessionManager
import com.metrolist.music.constants.CropAlbumArtKey
import com.metrolist.music.constants.MiniPlayerHeight
import com.metrolist.music.desktopimport.DesktopRemoteClient
import com.metrolist.music.constants.SwipeSensitivityKey
import com.metrolist.music.constants.SwipeThumbnailKey
import com.metrolist.music.db.entities.ArtistEntity
import com.metrolist.music.models.MediaMetadata
import com.metrolist.music.playback.PlayerConnection
import com.metrolist.music.ui.utils.resize
import com.metrolist.music.utils.rememberPreference
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import com.metrolist.music.ui.theme.RetroTokens
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.menu.AddToPlaylistDialog

/**
 * Stable wrapper for progress state - reads values only during draw phase
 * This prevents recomposition when position/duration change
 */
@Stable
class ProgressState(
    private val positionState: MutableLongState,
    private val durationState: MutableLongState,
) {
    val progress: Float
        get() {
            val duration = durationState.longValue
            return if (duration > 0) (positionState.longValue.toFloat() / duration).coerceIn(0f, 1f) else 0f
        }
}

@Composable
fun MiniPlayer(
    positionState: MutableLongState,
    durationState: MutableLongState,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    // Create stable progress state - doesn't cause recomposition on position changes
    val progressState = remember { ProgressState(positionState, durationState) }

    val playerConnection = LocalPlayerConnection.current ?: return

    val remoteState by DeviceSessionManager.remoteState.collectAsStateWithLifecycle()
    val sessionUi by DeviceSessionManager.uiState.collectAsStateWithLifecycle()
    val isComputerOutput = sessionUi.isComputerOutputSelected
    val remoteTrack = remoteState.track

    // Player states - only collect what's needed at this level
    val playbackState by playerConnection.playbackState.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val canSkipNext by playerConnection.canSkipNext.collectAsStateWithLifecycle()
    val canSkipPrevious by playerConnection.canSkipPrevious.collectAsStateWithLifecycle()

    LaunchedEffect(isComputerOutput, remoteState.positionMs, remoteTrack?.durationMs) {
        if (isComputerOutput) {
            positionState.longValue = remoteState.positionMs.coerceAtLeast(0L)
            val remoteDuration = remoteTrack?.durationMs?.takeIf { it > 0L } ?: 0L
            durationState.longValue = remoteDuration
        }
    }

    // Cast state - safely access castConnectionHandler to prevent crashes during service lifecycle changes
    val castHandler =
        remember(playerConnection) {
            try {
                playerConnection.service.castConnectionHandler
            } catch (e: Exception) {
                null
            }
        }
    val isCasting by castHandler?.isCasting?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }

    // Swipe settings
    val swipeSensitivity by rememberPreference(SwipeSensitivityKey, 0.73f)
    val swipeThumbnailPref by rememberPreference(SwipeThumbnailKey, true)

    // Disable swipe for Listen Together guests
    val listenTogetherManager = com.metrolist.music.LocalListenTogetherManager.current
    val isListenTogetherGuest = listenTogetherManager?.let { it.isInRoom && !it.isHost } ?: false
    val swipeThumbnail = swipeThumbnailPref && !isListenTogetherGuest && !isComputerOutput

    val layoutDirection = LocalLayoutDirection.current
    val coroutineScope = rememberCoroutineScope()

    val windowInfo = LocalWindowInfo.current
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val isTabletLandscape =
        remember(windowInfo.containerSize.width, configuration.orientation) {
            (windowInfo.containerSize.width / density.density) >= 600f && configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }

    // Swipe animation state
    val offsetXAnimatable = remember { Animatable(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }

    val animationSpec =
        remember {
            spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow)
        }

    val autoSwipeThreshold =
        remember(swipeSensitivity) {
            (600 / (1f + kotlin.math.exp(-(-11.44748 * swipeSensitivity + 9.04945)))).roundToInt()
        }

    val backgroundColor = RetroTokens.Panel
    val primaryColor = RetroTokens.Magenta
    val outlineColor = RetroTokens.Border
    val onSurfaceColor = RetroTokens.Text
    val errorColor = RetroTokens.Warning

    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(MiniPlayerHeight)
                .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Horizontal))
                .let { baseModifier ->
                    if (swipeThumbnail) {
                        baseModifier.pointerInput(Unit) {
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    dragStartTime = System.currentTimeMillis()
                                    totalDragDistance = 0f
                                },
                                onDragCancel = {
                                    coroutineScope.launch {
                                        offsetXAnimatable.animateTo(0f, animationSpec)
                                    }
                                },
                                onHorizontalDrag = { _, dragAmount ->
                                    val adjustedDragAmount =
                                        if (layoutDirection == LayoutDirection.Rtl) -dragAmount else dragAmount
                                    val canSkipPrevious = playerConnection.player.previousMediaItemIndex != -1
                                    val canSkipNext = playerConnection.player.nextMediaItemIndex != -1
                                    val tryingToSwipeRight = adjustedDragAmount > 0
                                    val tryingToSwipeLeft = adjustedDragAmount < 0
                                    val allowLeft = tryingToSwipeLeft && canSkipNext
                                    val allowRight = tryingToSwipeRight && canSkipPrevious

                                    val canReturnToCenter =
                                        (tryingToSwipeRight && !canSkipPrevious && offsetXAnimatable.value < 0) ||
                                            (tryingToSwipeLeft && !canSkipNext && offsetXAnimatable.value > 0)

                                    if (allowLeft || allowRight || canReturnToCenter) {
                                        totalDragDistance += kotlin.math.abs(adjustedDragAmount)
                                        coroutineScope.launch {
                                            offsetXAnimatable.snapTo(offsetXAnimatable.value + adjustedDragAmount)
                                        }
                                    }
                                },
                                onDragEnd = {
                                    val dragDuration = System.currentTimeMillis() - dragStartTime
                                    val velocity = if (dragDuration > 0) totalDragDistance / dragDuration else 0f
                                    val currentOffset = offsetXAnimatable.value
                                    val minDistanceThreshold = 50f
                                    val velocityThreshold = (swipeSensitivity * -8.25f) + 8.5f

                                    val shouldChangeSong =
                                        (kotlin.math.abs(currentOffset) > minDistanceThreshold && velocity > velocityThreshold) ||
                                            (kotlin.math.abs(currentOffset) > autoSwipeThreshold)

                                    if (shouldChangeSong) {
                                        if (currentOffset > 0 && canSkipPrevious) {
                                            playerConnection.player.seekToPreviousMediaItem()
                                        } else if (currentOffset <= 0 && canSkipNext) {
                                            playerConnection.player.seekToNext()
                                        }
                                    }
                                    coroutineScope.launch {
                                        offsetXAnimatable.animateTo(0f, animationSpec)
                                    }
                                },
                            )
                        }
                    } else {
                        baseModifier
                    }
                },
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        Column(
            modifier =
                Modifier
                    .then(if (isTabletLandscape) Modifier.width(500.dp).align(Alignment.Center) else Modifier.fillMaxWidth())
                    .offset { IntOffset(offsetXAnimatable.value.roundToInt(), 0) }
                    .background(color = backgroundColor)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = LocalIndication.current,
                        onClick = onClick
                    ),
        ) {
            // 1px top border
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(outlineColor)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp).weight(1f),
            ) {
                // Square artwork (40dp)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(RetroTokens.Background)
                        .border(1.dp, RetroTokens.BorderMuted, RoundedCornerShape(0.dp)),
                ) {
                    if (isComputerOutput) {
                        val thumbnailUrl =
                            remember(remoteTrack?.imageUrl) {
                                remoteTrack?.imageUrl?.resize(120, 120)
                            }
                        if (thumbnailUrl != null) {
                            AsyncImage(
                                model =
                                    ImageRequest.Builder(LocalContext.current)
                                        .data(thumbnailUrl)
                                        .listener(
                                            onError = { _: ImageRequest, _: ErrorResult ->
                                                DesktopRemoteClient.requestArtworkProxy()
                                            },
                                        )
                                        .build(),
                                contentDescription = stringResource(R.string.album_cover_desc),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.cast_connected),
                                contentDescription = null,
                                tint = RetroTokens.Magenta,
                                modifier = Modifier.size(22.dp).align(Alignment.Center),
                            )
                        }
                    } else {
                        mediaMetadata?.let { metadata ->
                            val thumbnailUrl =
                                remember(metadata.thumbnailUrl) {
                                    metadata.thumbnailUrl?.resize(120, 120)
                                }
                            AsyncImage(
                                model = thumbnailUrl,
                                contentDescription = stringResource(R.string.album_cover_desc),
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize(),
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Song info - title and artist (compact, marquee)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.Center,
                ) {
                    if (isComputerOutput) {
                        val computerTitle =
                            sessionUi.computerName.ifBlank {
                                stringResource(R.string.listen_on_this_computer)
                            }
                        Text(
                            text = stringResource(R.string.listen_on_controlling_computer),
                            color = primaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = remoteTrack?.title?.takeIf { it.isNotBlank() }
                                ?: stringResource(R.string.listen_on_remote_ready),
                            color = onSurfaceColor,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier =
                                Modifier.basicMarquee(
                                    iterations = 1,
                                    initialDelayMillis = 3000,
                                    velocity = 30.dp,
                                ),
                        )
                        Text(
                            text =
                                remoteTrack?.artist?.takeIf { it.isNotBlank() }
                                    ?: computerTitle,
                            color = onSurfaceColor.copy(alpha = 0.7f),
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            modifier =
                                Modifier.basicMarquee(
                                    iterations = 1,
                                    initialDelayMillis = 3000,
                                    velocity = 30.dp,
                                ),
                        )
                    } else {
                        mediaMetadata?.let { metadata ->
                            Text(
                                text = metadata.title,
                                color = onSurfaceColor,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Clip,
                                modifier =
                                    Modifier.basicMarquee(
                                        iterations = 1,
                                        initialDelayMillis = 3000,
                                        velocity = 30.dp,
                                    ),
                            )
                            if (metadata.artists.any { it.name.isNotBlank() }) {
                                Text(
                                    text = metadata.artists.joinToString { it.name },
                                    color = onSurfaceColor.copy(alpha = 0.7f),
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 1,
                                    overflow = TextOverflow.Clip,
                                    modifier =
                                        Modifier.basicMarquee(
                                            iterations = 1,
                                            initialDelayMillis = 3000,
                                            velocity = 30.dp,
                                        ),
                                )
                            }
                        }
                    }

                    val error by playerConnection.error.collectAsStateWithLifecycle()
                    AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
                        Text(
                            text = stringResource(R.string.error_playing),
                            color = errorColor,
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Square play/pause button (36dp)
                val isPlaying by playerConnection.isPlaying.collectAsStateWithLifecycle()
                val castIsPlaying by castHandler?.castIsPlaying?.collectAsStateWithLifecycle() ?: remember { mutableStateOf(false) }
                val effectiveIsPlaying =
                    when {
                        isComputerOutput -> remoteState.isPlaying
                        isCasting -> castIsPlaying
                        else -> isPlaying
                    }
                val isMuted by playerConnection.isMuted.collectAsStateWithLifecycle()

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(RetroTokens.Panel)
                        .border(1.dp, outlineColor, RoundedCornerShape(0.dp))
                        .clickable {
                            if (isListenTogetherGuest) {
                                playerConnection.toggleMute()
                                return@clickable
                            }
                            if (isComputerOutput) {
                                sessionUi.session?.let { session ->
                                    DesktopRemoteClient.connect(session.remote.wsUrl, session.remote.token)
                                }
                                DesktopRemoteClient.togglePlayPause()
                            } else if (isCasting) {
                                if (castIsPlaying) castHandler?.pause() else castHandler?.play()
                            } else if (playbackState == Player.STATE_ENDED) {
                                playerConnection.player.seekTo(0, 0)
                                playerConnection.player.playWhenReady = true
                            } else {
                                playerConnection.togglePlayPause()
                            }
                        },
                ) {
                    Icon(
                        painter = painterResource(
                            when {
                                isListenTogetherGuest -> if (isMuted) R.drawable.volume_off else R.drawable.volume_up
                                playbackState == Player.STATE_ENDED -> R.drawable.replay
                                effectiveIsPlaying -> R.drawable.pause
                                else -> R.drawable.play
                            },
                        ),
                        contentDescription =
                            when {
                                isListenTogetherGuest ->
                                    stringResource(if (isMuted) R.string.unmute else R.string.mute)
                                playbackState == Player.STATE_ENDED ->
                                    stringResource(R.string.product_ux_a11y_replay)
                                effectiveIsPlaying -> stringResource(R.string.pause)
                                else -> stringResource(R.string.play)
                            },
                        tint = RetroTokens.Text,
                        modifier = Modifier.size(20.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Square next button (36dp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .background(RetroTokens.Panel)
                        .border(1.dp, outlineColor, RoundedCornerShape(0.dp))
                        .clickable(enabled = (isComputerOutput || canSkipNext) && !isListenTogetherGuest) {
                            if (!isListenTogetherGuest) {
                                if (isComputerOutput) {
                                    sessionUi.session?.let { session ->
                                        DesktopRemoteClient.connect(session.remote.wsUrl, session.remote.token)
                                    }
                                    DesktopRemoteClient.next()
                                } else {
                                    playerConnection.seekToNext()
                                }
                            }
                        },
                ) {
                    Icon(
                        painter = painterResource(R.drawable.skip_next),
                        contentDescription = stringResource(R.string.next),
                        tint =
                            if ((isComputerOutput || canSkipNext) && !isListenTogetherGuest) {
                                RetroTokens.Text
                            } else {
                                RetroTokens.TextDim
                            },
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Thin 1px progress line at bottom
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .drawWithContent {
                        val progress = progressState.progress
                        drawRect(RetroTokens.BorderDark)
                        drawRect(primaryColor, size = Size(size.width * progress, size.height))
                    },
            )
        }
    }
}

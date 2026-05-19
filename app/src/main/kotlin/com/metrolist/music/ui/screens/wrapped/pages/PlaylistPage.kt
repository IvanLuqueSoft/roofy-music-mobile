package com.metrolist.music.ui.screens.wrapped.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.screens.wrapped.LocalWrappedManager
import com.metrolist.music.ui.screens.wrapped.PlaylistCreationState
import com.metrolist.music.ui.screens.wrapped.WrappedConstants
import com.metrolist.music.ui.theme.RetroArtwork
import com.metrolist.music.ui.theme.RetroButton
import com.metrolist.music.ui.theme.RetroPanel
import com.metrolist.music.ui.theme.RetroTokens
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun PlaylistPage() {
    val manager = LocalWrappedManager.current
    val state by manager.state.collectAsStateWithLifecycle()
    val playlistCreationState = state.playlistCreationState

    val (playlistImageRes, playlistImageName) = remember {
        if (Random.nextBoolean()) {
            Pair(R.drawable.wrapped_playlistv1, "wrapped_playlistv1")
        } else {
            Pair(R.drawable.wrapped_playlistv2, "wrapped_playlistv2")
        }
    }

    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(200)
        startAnimation = true
    }

    val contentAlpha = if (startAnimation) 1f else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .alpha(contentAlpha),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        RetroPanel(
            modifier = Modifier.padding(16.dp),
            strong = true
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.wrapped_playlist_ready).uppercase(),
                    style = TextStyle(
                        color = RetroTokens.TextHot,
                        fontSize = 16.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                RetroArtwork {
                    Image(
                        painter = painterResource(id = playlistImageRes),
                        contentDescription = stringResource(R.string.album_cover_desc),
                        modifier = Modifier
                            .size(200.dp)
                            .clip(RoundedCornerShape(0.dp))
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.wrapped_playlist_title, WrappedConstants.YEAR),
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = RetroTokens.Text,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                )
                Spacer(modifier = Modifier.height(24.dp))
                RetroButton(
                    onClick = {
                        if (playlistCreationState == PlaylistCreationState.Idle) {
                            manager.createPlaylist(playlistImageName)
                        }
                    },
                    modifier = Modifier.height(40.dp)
                ) {
                    when (playlistCreationState) {
                        is PlaylistCreationState.Idle -> Text(
                            text = stringResource(R.string.wrapped_create_playlist).uppercase(),
                            style = TextStyle(
                                color = RetroTokens.Text,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                        is PlaylistCreationState.Creating -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = RetroTokens.Text,
                            strokeWidth = 2.dp
                        )
                        is PlaylistCreationState.Success -> Text(
                            text = stringResource(R.string.wrapped_playlist_saved).uppercase(),
                            style = TextStyle(
                                color = RetroTokens.Text,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }
    }
}

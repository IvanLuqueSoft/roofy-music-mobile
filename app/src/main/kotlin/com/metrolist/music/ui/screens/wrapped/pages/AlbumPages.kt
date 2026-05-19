/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.wrapped.pages

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.metrolist.music.R
import com.metrolist.music.db.entities.Album
import com.metrolist.music.ui.theme.RetroArtwork
import com.metrolist.music.ui.theme.RetroPanel
import com.metrolist.music.ui.theme.RetroTokens
import kotlinx.coroutines.delay

@Composable
fun WrappedTotalAlbumsScreen(uniqueAlbumCount: Int, isVisible: Boolean) {
    val animatedAlbums = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible, uniqueAlbumCount) {
        if (isVisible) {
            visible = true
            if (uniqueAlbumCount > 0) {
                animatedAlbums.animateTo(
                    targetValue = uniqueAlbumCount.toFloat(),
                    animationSpec = tween(1500, easing = FastOutSlowInEasing)
                )
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        RetroPanel(
            modifier = Modifier.fillMaxWidth(),
            strong = true
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 200))
                ) {
                    Text(
                        text = stringResource(R.string.wrapped_total_albums_title).uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = TextStyle(
                            color = RetroTokens.TextHot,
                            fontSize = 16.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                }
                Spacer(Modifier.height(24.dp))
                BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                    val density = LocalDensity.current
                    val baseStyle = TextStyle(
                        color = RetroTokens.TextHot,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        fontFamily = FontFamily.Monospace,
                        drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(with(density) { 1.dp.toPx() })
                    )
                    val textStyle = remember(uniqueAlbumCount, maxWidth) {
                        val finalText = uniqueAlbumCount.toString()
                        var style = baseStyle.copy(fontSize = 72.sp)
                        var textWidth = textMeasurer.measure(finalText, style).size.width
                        while (textWidth > constraints.maxWidth) {
                            style = style.copy(fontSize = style.fontSize * 0.95f)
                            textWidth = textMeasurer.measure(finalText, style).size.width
                        }
                        style.copy(lineHeight = style.fontSize * 1.08f)
                    }
                    Text(
                        text = animatedAlbums.value.toInt().toString(),
                        style = textStyle,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(animationSpec = tween(600, delayMillis = 600))
                ) {
                    Text(
                        text = stringResource(R.string.wrapped_total_albums_subtitle),
                        modifier = Modifier.padding(horizontal = 8.dp),
                        style = TextStyle(
                            color = RetroTokens.TextSoft,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun WrappedTopAlbumScreen(topAlbum: Album?, isVisible: Boolean) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(isVisible) {
        if (isVisible) {
            visible = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(600, delayMillis = 200))
        ) {
            Text(
                text = stringResource(R.string.wrapped_top_album_title).uppercase(),
                style = TextStyle(
                    color = RetroTokens.TextHot,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }
        Spacer(modifier = Modifier.height(24.dp))
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(600, delayMillis = 400))
        ) {
            RetroArtwork {
                AsyncImage(
                    model = topAlbum?.thumbnailUrl,
                    contentDescription = stringResource(R.string.album_art_for, topAlbum?.title ?: ""),
                    modifier = Modifier
                        .size(160.dp)
                        .clip(RoundedCornerShape(0.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(600, delayMillis = 600))
        ) {
            RetroPanel(
                modifier = Modifier.padding(horizontal = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = topAlbum?.title ?: stringResource(id = R.string.wrapped_no_data),
                        style = TextStyle(
                            color = RetroTokens.TextHot,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.wrapped_album_listening_time, topAlbum?.timeListened?.div(60000) ?: 0),
                        style = TextStyle(
                            color = RetroTokens.TextSoft,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun WrappedTop5AlbumsScreen(topAlbums: List<Album>, isVisible: Boolean) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(isVisible) {
        if (isVisible) {
            delay(200)
            visible = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(600, delayMillis = 200))
        ) {
            Text(
                text = stringResource(R.string.wrapped_top_5_albums_title).uppercase(),
                style = TextStyle(
                    color = RetroTokens.TextHot,
                    fontSize = 16.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        RetroPanel(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                topAlbums.forEachIndexed { index, album ->
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(animationSpec = tween(400, delayMillis = 300 + (index * 150)))
                    ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format("%02d", index + 1),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 14.sp,
                            color = RetroTokens.TextMuted,
                            modifier = Modifier.width(28.dp)
                        )
                        RetroArtwork {
                            AsyncImage(
                                model = album.thumbnailUrl,
                                contentDescription = stringResource(R.string.album_art_for, album.title),
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(0.dp)),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = album.title,
                                color = RetroTokens.Text,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                            Text(
                                text = stringResource(R.string.wrapped_album_listening_time_minutes, album.timeListened?.div(60000) ?: 0),
                                color = RetroTokens.TextSoft,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

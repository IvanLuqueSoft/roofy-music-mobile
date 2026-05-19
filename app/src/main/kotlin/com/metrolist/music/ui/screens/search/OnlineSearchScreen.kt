/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.search

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.EpisodeItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.PodcastItem
import com.metrolist.innertube.models.SongItem
import com.metrolist.music.LocalDatabase
import com.metrolist.music.LocalPlayerConnection
import com.metrolist.music.R
import com.metrolist.music.models.toMediaMetadata
import com.metrolist.music.playback.queues.YouTubeQueue
import com.metrolist.music.ui.component.LocalMenuState
import com.metrolist.music.ui.component.YouTubeListItem
import com.metrolist.music.ui.menu.YouTubeAlbumMenu
import com.metrolist.music.ui.menu.YouTubeArtistMenu
import com.metrolist.music.ui.menu.YouTubePlaylistMenu
import com.metrolist.music.ui.menu.YouTubeSongMenu
import com.metrolist.music.ui.theme.RetroListItem
import com.metrolist.music.ui.theme.RetroTokens
import com.metrolist.music.viewmodels.OnlineSearchSuggestionViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop

@OptIn(ExperimentalFoundationApi::class, ExperimentalComposeUiApi::class, ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun OnlineSearchScreen(
    query: String,
    onQueryChange: (TextFieldValue) -> Unit,
    navController: NavController,
    onSearch: (String) -> Unit,
    onDismiss: () -> Unit,
    pureBlack: Boolean,
    viewModel: OnlineSearchSuggestionViewModel = hiltViewModel(),
) {
    val database = LocalDatabase.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val menuState = LocalMenuState.current
    val playerConnection = LocalPlayerConnection.current ?: return

    val coroutineScope = rememberCoroutineScope()

    val haptic = LocalHapticFeedback.current
    val isPlaying by playerConnection.isEffectivelyPlaying.collectAsStateWithLifecycle()
    val mediaMetadata by playerConnection.mediaMetadata.collectAsStateWithLifecycle()
    val viewState by viewModel.viewState.collectAsStateWithLifecycle()

    val lazyListState = rememberLazyListState()

    LaunchedEffect(Unit) {
        snapshotFlow { lazyListState.firstVisibleItemScrollOffset }
            .drop(1)
            .collect {
                keyboardController?.hide()
            }
    }

    LaunchedEffect(query) {
        snapshotFlow { query }.debounce(300L).collectLatest {
            viewModel.query.value = it
        }
    }

    LazyColumn(
        state = lazyListState,
        contentPadding = WindowInsets.systemBars.only(WindowInsetsSides.Bottom).asPaddingValues(),
        modifier =
            Modifier
                .fillMaxSize()
                .background(if (pureBlack) Color.Black else RetroTokens.Background),
    ) {
        // Show parsed URL item at the top if present
        if (viewState.isUrlQuery && viewState.parsedUrlItem != null) {
            item(key = "parsed_url_header") {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.parsed_from_link),
                        style = MaterialTheme.typography.labelMedium,
                        color = RetroTokens.TextSoft,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
            }

            item(key = "parsed_url_item") {
                val item = viewState.parsedUrlItem!!
                val subtitle = when (item) {
                    is SongItem -> item.artists.joinToString { it.name }
                    is AlbumItem -> item.artists?.joinToString { it.name }
                    is ArtistItem -> null
                    is PlaylistItem -> item.author?.name
                    is PodcastItem -> item.author?.name
                    is EpisodeItem -> item.author?.name
                    else -> null
                }
                RetroSearchResultItem(
                    index = null,
                    title = item.title,
                    subtitle = subtitle,
                    onClick = {
                        when (item) {
                            is SongItem -> {
                                if (item.id == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        YouTubeQueue.radio(item.toMediaMetadata()),
                                    )
                                    onDismiss()
                                }
                            }
                            is AlbumItem -> {
                                navController.navigate("album/${item.id}")
                                onDismiss()
                            }
                            is ArtistItem -> {
                                navController.navigate("artist/${item.id}")
                                onDismiss()
                            }
                            is PlaylistItem -> {
                                navController.navigate("online_playlist/${item.id}")
                                onDismiss()
                            }
                            is PodcastItem -> {
                                navController.navigate("online_podcast/${item.id}")
                                onDismiss()
                            }
                            is EpisodeItem -> {
                                if (item.id == mediaMetadata?.id) {
                                    playerConnection.togglePlayPause()
                                } else {
                                    playerConnection.playQueue(
                                        YouTubeQueue.radio(item.toMediaMetadata()),
                                    )
                                    onDismiss()
                                }
                            }
                        }
                    },
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        menuState.show {
                            when (item) {
                                is SongItem -> {
                                    YouTubeSongMenu(
                                        song = item,
                                        navController = navController,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                }
                                is AlbumItem -> {
                                    YouTubeAlbumMenu(
                                        albumItem = item,
                                        navController = navController,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                }
                                is ArtistItem -> {
                                    YouTubeArtistMenu(
                                        artist = item,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                }
                                is PlaylistItem -> {
                                    YouTubePlaylistMenu(
                                        playlist = item,
                                        coroutineScope = coroutineScope,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                }
                                is PodcastItem -> {
                                    YouTubePlaylistMenu(
                                        playlist = item.asPlaylistItem(),
                                        coroutineScope = coroutineScope,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                }
                                is EpisodeItem -> {
                                    YouTubeSongMenu(
                                        song = item.asSongItem(),
                                        navController = navController,
                                        onDismiss = {
                                            menuState.dismiss()
                                            onDismiss()
                                        },
                                    )
                                }
                            }
                        }
                    },
                    trailing = {
                        IconButton(
                            onClick = {
                                menuState.show {
                                    when (item) {
                                        is SongItem -> {
                                            YouTubeSongMenu(
                                                song = item,
                                                navController = navController,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }
                                        is AlbumItem -> {
                                            YouTubeAlbumMenu(
                                                albumItem = item,
                                                navController = navController,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }
                                        is ArtistItem -> {
                                            YouTubeArtistMenu(
                                                artist = item,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }
                                        is PlaylistItem -> {
                                            YouTubePlaylistMenu(
                                                playlist = item,
                                                coroutineScope = coroutineScope,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }
                                        is PodcastItem -> {
                                            YouTubePlaylistMenu(
                                                playlist = item.asPlaylistItem(),
                                                coroutineScope = coroutineScope,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }
                                        is EpisodeItem -> {
                                            YouTubeSongMenu(
                                                song = item.asSongItem(),
                                                navController = navController,
                                                onDismiss = {
                                                    menuState.dismiss()
                                                    onDismiss()
                                                },
                                            )
                                        }
                                    }
                                }
                            },
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.more_vert),
                                contentDescription = null,
                                tint = RetroTokens.TextSoft,
                            )
                        }
                    },
                    modifier = Modifier.animateItem(),
                )
            }
        }

        items(viewState.history, key = { "history_${it.query}" }) { history ->
            RetroListItem(
                index = null,
                title = history.query,
                subtitle = null,
                onClick = null,
                trailing = {
                    Row {
                        IconButton(
                            onClick = {
                                database.query {
                                    delete(history)
                                }
                            },
                            modifier = Modifier.alpha(0.7f),
                        ) {
                            Text(
                                text = "[x]",
                                style = MaterialTheme.typography.labelSmall,
                                color = RetroTokens.TextSoft,
                            )
                        }
                        IconButton(
                            onClick = {
                                onQueryChange(TextFieldValue(history.query, TextRange(history.query.length)))
                            },
                            modifier = Modifier.alpha(0.7f),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.arrow_top_left),
                                contentDescription = null,
                                tint = RetroTokens.TextSoft,
                            )
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            onSearch(history.query)
                            onDismiss()
                        },
                    )
                    .animateItem(),
            )
        }

        items(viewState.suggestions, key = { "suggestion_$it" }) { suggestion ->
            RetroListItem(
                index = null,
                title = suggestion,
                subtitle = null,
                onClick = null,
                trailing = {
                    IconButton(
                        onClick = {
                            onQueryChange(TextFieldValue(suggestion, TextRange(suggestion.length)))
                        },
                        modifier = Modifier.alpha(0.7f),
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_top_left),
                            contentDescription = null,
                            tint = RetroTokens.TextSoft,
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .combinedClickable(
                        onClick = {
                            onSearch(suggestion)
                            onDismiss()
                        },
                    )
                    .animateItem(),
            )
        }

        if (viewState.items.isNotEmpty() && viewState.history.size + viewState.suggestions.size > 0) {
            item(key = "search_divider_spacer") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        items(viewState.items, key = { "item_${it.id}" }) { item ->
            val subtitle = when (item) {
                is SongItem -> item.artists.joinToString { it.name }
                is AlbumItem -> listOfNotNull(item.artists?.joinToString { it.name }, item.year?.toString()).joinToString(" | ")
                is ArtistItem -> null
                is PlaylistItem -> listOfNotNull(item.author?.name, item.songCountText).joinToString(" | ")
                is PodcastItem -> listOfNotNull(item.author?.name, item.episodeCountText).joinToString(" | ")
                is EpisodeItem -> listOfNotNull(item.author?.name, item.publishDateText).joinToString(" | ")
                else -> null
            }
            val index = viewState.items.indexOf(item) + 1
            RetroSearchResultItem(
                index = index,
                title = item.title,
                subtitle = subtitle,
                onClick = {
                    when (item) {
                        is SongItem -> {
                            if (item.id == mediaMetadata?.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue.radio(item.toMediaMetadata()),
                                )
                                onDismiss()
                            }
                        }
                        is AlbumItem -> {
                            navController.navigate("album/${item.id}")
                            onDismiss()
                        }
                        is ArtistItem -> {
                            navController.navigate("artist/${item.id}")
                            onDismiss()
                        }
                        is PlaylistItem -> {
                            navController.navigate("online_playlist/${item.id}")
                            onDismiss()
                        }
                        is PodcastItem -> {
                            navController.navigate("online_podcast/${item.id}")
                            onDismiss()
                        }
                        is EpisodeItem -> {
                            if (item.id == mediaMetadata?.id) {
                                playerConnection.togglePlayPause()
                            } else {
                                playerConnection.playQueue(
                                    YouTubeQueue.radio(item.toMediaMetadata()),
                                )
                                onDismiss()
                            }
                        }
                    }
                },
                onLongClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    menuState.show {
                        when (item) {
                            is SongItem -> {
                                YouTubeSongMenu(
                                    song = item,
                                    navController = navController,
                                    onDismiss = {
                                        menuState.dismiss()
                                        onDismiss()
                                    },
                                )
                            }
                            is AlbumItem -> {
                                YouTubeAlbumMenu(
                                    albumItem = item,
                                    navController = navController,
                                    onDismiss = {
                                        menuState.dismiss()
                                        onDismiss()
                                    },
                                )
                            }
                            is ArtistItem -> {
                                YouTubeArtistMenu(
                                    artist = item,
                                    onDismiss = {
                                        menuState.dismiss()
                                        onDismiss()
                                    },
                                )
                            }
                            is PlaylistItem -> {
                                YouTubePlaylistMenu(
                                    playlist = item,
                                    coroutineScope = coroutineScope,
                                    onDismiss = {
                                        menuState.dismiss()
                                        onDismiss()
                                    },
                                )
                            }
                            is PodcastItem -> {
                                YouTubePlaylistMenu(
                                    playlist = item.asPlaylistItem(),
                                    coroutineScope = coroutineScope,
                                    onDismiss = {
                                        menuState.dismiss()
                                        onDismiss()
                                    },
                                )
                            }
                            is EpisodeItem -> {
                                YouTubeSongMenu(
                                    song = item.asSongItem(),
                                    navController = navController,
                                    onDismiss = {
                                        menuState.dismiss()
                                        onDismiss()
                                    },
                                )
                            }
                        }
                    }
                },
                trailing = {
                    IconButton(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            menuState.show {
                                when (item) {
                                    is SongItem -> {
                                        YouTubeSongMenu(
                                            song = item,
                                            navController = navController,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }
                                    is AlbumItem -> {
                                        YouTubeAlbumMenu(
                                            albumItem = item,
                                            navController = navController,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }
                                    is ArtistItem -> {
                                        YouTubeArtistMenu(
                                            artist = item,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }
                                    is PlaylistItem -> {
                                        YouTubePlaylistMenu(
                                            playlist = item,
                                            coroutineScope = coroutineScope,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }
                                    is PodcastItem -> {
                                        YouTubePlaylistMenu(
                                            playlist = item.asPlaylistItem(),
                                            coroutineScope = coroutineScope,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }
                                    is EpisodeItem -> {
                                        YouTubeSongMenu(
                                            song = item.asSongItem(),
                                            navController = navController,
                                            onDismiss = {
                                                menuState.dismiss()
                                                onDismiss()
                                            },
                                        )
                                    }
                                }
                            }
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.more_vert),
                            contentDescription = null,
                            tint = RetroTokens.TextSoft,
                        )
                    }
                },
                modifier = Modifier.animateItem(),
            )
        }
    }
}

@Composable
private fun RetroSearchResultItem(
    index: Int?,
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    trailing: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    RetroListItem(
        index = index,
        title = title,
        subtitle = subtitle,
        onClick = null,
        trailing = trailing,
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    )
}

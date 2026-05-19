/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.wrapped.pages

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.ui.screens.wrapped.MessagePair
import com.metrolist.music.ui.theme.RetroTokens
import kotlinx.coroutines.delay

@Composable
fun WrappedMinutesTease(
    messagePair: MessagePair?,
    onNavigateForward: () -> Unit,
    isDataReady: Boolean
) {
    LaunchedEffect(Unit) {
        delay(3500)
        onNavigateForward()
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (messagePair != null && isDataReady) {
            Text(
                text = messagePair.tease,
                modifier = Modifier.padding(horizontal = 24.dp),
                style = TextStyle(
                    color = RetroTokens.Text,
                    fontSize = 16.sp,
                    lineHeight = 22.sp,
                    textAlign = TextAlign.Center,
                    fontFamily = FontFamily.Monospace
                )
            )
        }
    }
}

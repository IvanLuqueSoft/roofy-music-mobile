/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.screens.wrapped.pages

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.metrolist.music.R
import com.metrolist.music.ui.screens.wrapped.MessagePair
import com.metrolist.music.ui.theme.RetroPanel
import com.metrolist.music.ui.theme.RetroTokens

@Composable
fun WrappedMinutesScreen(
    messagePair: MessagePair?, totalMinutes: Long,
    isVisible: Boolean
) {
    val animatedMinutes = remember { Animatable(0f) }
    val textMeasurer = rememberTextMeasurer()

    LaunchedEffect(isVisible, totalMinutes) {
        if (isVisible && totalMinutes > 0) {
            animatedMinutes.animateTo(targetValue = totalMinutes.toFloat(), animationSpec = tween(1500, easing = FastOutSlowInEasing))
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
                Text(
                    text = messagePair?.tease ?: "",
                    modifier = Modifier.padding(horizontal = 8.dp),
                    style = TextStyle(
                        color = RetroTokens.TextSoft,
                        fontSize = 14.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center
                    )
                )

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
                    val textStyle = remember(totalMinutes, maxWidth) {
                        val finalText = totalMinutes.toString()
                        var style = baseStyle.copy(fontSize = 72.sp)
                        var textWidth = textMeasurer.measure(finalText, style).size.width
                        while (textWidth > constraints.maxWidth) {
                            style = style.copy(fontSize = style.fontSize * 0.95f)
                            textWidth = textMeasurer.measure(finalText, style).size.width
                        }
                        style.copy(lineHeight = style.fontSize * 1.08f)
                    }
                    Text(
                        text = animatedMinutes.value.toInt().toString(),
                        style = textStyle,
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(16.dp))

                FormattedText(
                    text = messagePair?.reveal ?: "",
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

@Composable
fun FormattedText(text: String, modifier: Modifier = Modifier, style: androidx.compose.ui.text.TextStyle) {
    val annotatedString = buildAnnotatedString {
        val parts = text.split("(?=\\*\\*)|(?<=\\*\\*)".toRegex())
        var isBold = false
        for (part in parts) {
            if (part == "**") isBold = !isBold
            else withStyle(SpanStyle(fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal)) { append(part) }
        }
    }
    Text(annotatedString, modifier, style = style)
}

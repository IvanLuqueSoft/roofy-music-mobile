/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

object RetroTokens {
    val Background = Color(0xFF050505)
    val Background2 = Color(0xFF080808)
    val Panel = Color(0xFF0B0B0B)
    val Panel2 = Color(0xFF101010)
    val Panel3 = Color(0xFF151515)

    val BorderBright = Color(0xFFB8B8B8)
    val Border = Color(0xFF7A7A7A)
    val BorderMuted = Color(0xFF3F3F3F)
    val BorderDark = Color(0xFF222222)

    val Text = Color(0xFFD0D0D0)
    val TextSoft = Color(0xFFA0A0A0)
    val TextMuted = Color(0xFF707070)
    val TextDim = Color(0xFF4A4A4A)
    val TextHot = Color(0xFFF1F1F1)

    // Monochrome pivot: accents are now grayscale/pale gray only
    val Magenta = Color(0xFFB8B8B8)
    val MagentaDim = Color(0xFF3F3F3F)
    val Active = Color(0xFFD0D0D0)
    val ActiveMuted = Color(0xFF707070)
    val Warning = Color(0xFF707070)

    val Radius = 0.dp
    val BorderWidth = 1.dp
}

fun Modifier.retroBorder(strong: Boolean = false) =
    border(
        RetroTokens.BorderWidth,
        if (strong) RetroTokens.BorderBright else RetroTokens.Border,
        RoundedCornerShape(RetroTokens.Radius),
    )

fun Modifier.retroPanelBackground() = background(RetroTokens.Panel)

@Composable
fun RetroPanel(
    modifier: Modifier = Modifier,
    strong: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .background(RetroTokens.Panel)
            .retroBorder(strong),
        content = content,
    )
}

@Composable
fun RetroCard(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) = RetroPanel(
    modifier = modifier.padding(0.dp),
    content = content,
)

@Composable
fun RetroButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .height(32.dp)
            .background(if (enabled) RetroTokens.Panel else RetroTokens.Background2)
            .border(
                RetroTokens.BorderWidth,
                if (enabled) RetroTokens.Border else RetroTokens.BorderMuted,
                RoundedCornerShape(RetroTokens.Radius),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
        content = content,
    )
}

@Composable
fun RetroTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    RetroButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = if (enabled) RetroTokens.Text else RetroTokens.TextDim,
            maxLines = 1,
        )
    }
}

@Composable
fun RetroIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    enabled: Boolean = true,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(
                when {
                    selected -> RetroTokens.Panel2
                    enabled -> RetroTokens.Panel
                    else -> RetroTokens.Background2
                },
            )
            .border(
                RetroTokens.BorderWidth,
                if (selected) RetroTokens.ActiveMuted else RetroTokens.Border,
                RoundedCornerShape(RetroTokens.Radius),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        content = content,
    )
}

@Composable
fun RetroToggle(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(40.dp, 24.dp)
            .background(
                if (checked) RetroTokens.Panel2 else RetroTokens.Background,
            )
            .border(
                RetroTokens.BorderWidth,
                if (checked) RetroTokens.BorderBright else RetroTokens.BorderMuted,
                RoundedCornerShape(RetroTokens.Radius),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = { onCheckedChange(!checked) },
            ),
    ) {
        Text(
            text = if (checked) "[x]" else "[ ]",
            style = MaterialTheme.typography.labelSmall,
            color = if (checked) RetroTokens.TextHot else RetroTokens.TextDim,
        )
    }
}

@Composable
fun RetroCheckbox(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .background(if (checked) RetroTokens.Panel2 else RetroTokens.Background)
            .border(
                RetroTokens.BorderWidth,
                if (checked) RetroTokens.BorderBright else RetroTokens.BorderMuted,
                RoundedCornerShape(RetroTokens.Radius),
            )
            .then(
                if (onCheckedChange != null) {
                    Modifier.clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onCheckedChange(!checked) },
                    )
                } else Modifier
            ),
    ) {
        Text(
            text = if (checked) "x" else "",
            style = MaterialTheme.typography.labelSmall,
            color = RetroTokens.TextHot,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RetroRadioButton(
    selected: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(24.dp)
            .background(if (selected) RetroTokens.Panel2 else RetroTokens.Background)
            .border(
                RetroTokens.BorderWidth,
                if (selected) RetroTokens.BorderBright else RetroTokens.BorderMuted,
                RoundedCornerShape(RetroTokens.Radius),
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        enabled = enabled,
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier
            ),
    ) {
        Text(
            text = if (selected) "*" else "",
            style = MaterialTheme.typography.labelSmall,
            color = RetroTokens.TextHot,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun RetroProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    totalBlocks: Int = 20,
) {
    val filled = (progress * totalBlocks).toInt().coerceIn(0, totalBlocks)
    val empty = totalBlocks - filled
    Text(
        text = "[" + "=".repeat(filled) + ">" + " ".repeat(empty.coerceAtLeast(0)) + "]",
        style = MaterialTheme.typography.labelSmall,
        color = RetroTokens.TextSoft,
        modifier = modifier,
    )
}

@Composable
fun RetroSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .height(16.dp)
            .background(RetroTokens.Background)
            .border(
                RetroTokens.BorderWidth,
                RetroTokens.BorderMuted,
                RoundedCornerShape(RetroTokens.Radius),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                onClick = { },
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .height(14.dp)
                .background(if (enabled) RetroTokens.Border else RetroTokens.BorderMuted),
        )
    }
}

@Composable
fun RetroArtwork(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .background(RetroTokens.Background)
            .border(RetroTokens.BorderWidth, RetroTokens.BorderMuted, RoundedCornerShape(RetroTokens.Radius)),
        content = content,
    )
}

@Composable
fun RetroSectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Text(
            text = ">",
            color = RetroTokens.TextSoft,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = " ${title.uppercase()} ",
            color = RetroTokens.Text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(1.dp)
                .background(RetroTokens.BorderMuted),
        )
        action?.invoke()
    }
}

@Composable
fun RetroListItem(
    index: Int? = null,
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(if (selected) RetroTokens.Panel2 else Color.Transparent)
            .border(
                RetroTokens.BorderWidth,
                if (selected) RetroTokens.ActiveMuted else RetroTokens.BorderDark,
                RoundedCornerShape(RetroTokens.Radius),
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick,
                    )
                } else Modifier
            )
            .padding(horizontal = 12.dp),
    ) {
        if (index != null) {
            Text(
                text = String.format("%02d", index),
                style = MaterialTheme.typography.labelMedium,
                color = RetroTokens.TextMuted,
                modifier = Modifier.width(28.dp),
            )
        }
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = RetroTokens.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = RetroTokens.TextSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun RetroGridItem(
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .background(if (selected) RetroTokens.Panel2 else RetroTokens.Panel)
            .border(
                RetroTokens.BorderWidth,
                if (selected) RetroTokens.ActiveMuted else RetroTokens.Border,
                RoundedCornerShape(RetroTokens.Radius),
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            content()
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = RetroTokens.Text,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = RetroTokens.TextSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
fun RetroCommandBar(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = modifier
            .fillMaxWidth()
            .background(RetroTokens.Background2)
            .border(
                RetroTokens.BorderWidth,
                RetroTokens.Border,
                RoundedCornerShape(RetroTokens.Radius),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
        content = content,
    )
}

@Composable
fun RetroSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        color = RetroTokens.Panel,
        contentColor = RetroTokens.Text,
        shape = RoundedCornerShape(RetroTokens.Radius),
        border = BorderStroke(RetroTokens.BorderWidth, RetroTokens.Border),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
    ) {
        CompositionLocalProvider(LocalContentColor provides RetroTokens.Text) {
            content()
        }
    }
}

/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.metrolist.music.ui.theme.RetroTokens

@Composable
fun Material3SettingsGroup(
    title: String? = null,
    items: List<Material3SettingsItem>,
    useLowContrast: Boolean = false
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        title?.let {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(RetroTokens.Panel)
                    .border(
                        RetroTokens.BorderWidth,
                        RetroTokens.Border,
                        RoundedCornerShape(RetroTokens.Radius)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "[ ${it.uppercase()} ]",
                    style = MaterialTheme.typography.labelMedium,
                    color = RetroTokens.TextSoft,
                )
            }
            Spacer(modifier = Modifier.height(1.dp))
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(RetroTokens.Panel)
                .border(
                    RetroTokens.BorderWidth,
                    RetroTokens.Border,
                    RoundedCornerShape(RetroTokens.Radius)
                ),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
            items.forEachIndexed { index, item ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(RetroTokens.BorderDark)
                    )
                }
                Material3SettingsItemRow(item = item)
            }
        }
    }
}

@Composable
private fun Material3SettingsItemRow(
    item: Material3SettingsItem
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                enabled = item.enabled && item.onClick != null,
                onClick = { item.onClick?.invoke() }
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.leadingContent != null) {
            item.leadingContent.invoke()
            Spacer(modifier = Modifier.width(12.dp))
        } else if (item.icon != null) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(RetroTokens.Background)
                    .border(
                        RetroTokens.BorderWidth,
                        if (item.isHighlighted) RetroTokens.BorderBright else RetroTokens.BorderMuted,
                        RoundedCornerShape(RetroTokens.Radius)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = item.icon,
                    contentDescription = null,
                    tint = when {
                        !item.enabled -> RetroTokens.TextDim
                        item.isHighlighted -> RetroTokens.TextHot
                        item.showBadge -> RetroTokens.TextSoft
                        else -> RetroTokens.TextSoft
                    },
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
        }

        Column(
            modifier = Modifier.weight(1f)
        ) {
            ProvideTextStyle(
                MaterialTheme.typography.bodyMedium.copy(
                    color = if (!item.enabled) RetroTokens.TextDim else RetroTokens.Text
                )
            ) {
                item.title()
            }

            item.description?.let { desc ->
                Spacer(modifier = Modifier.height(1.dp))
                ProvideTextStyle(
                    MaterialTheme.typography.bodySmall.copy(
                        color = if (!item.enabled) RetroTokens.TextDim else RetroTokens.TextMuted
                    )
                ) {
                    desc()
                }
            }
        }

        item.trailingContent?.let { trailing ->
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

data class Material3SettingsItem(
    val icon: Painter? = null,
    val leadingContent: (@Composable () -> Unit)? = null,
    val title: @Composable () -> Unit,
    val description: (@Composable () -> Unit)? = null,
    val trailingContent: (@Composable () -> Unit)? = null,
    val showBadge: Boolean = false,
    val isHighlighted: Boolean = false,
    val enabled: Boolean = true,
    val onClick: (() -> Unit)? = null
)

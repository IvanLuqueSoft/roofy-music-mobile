/**
 * Metrolist Project (C) 2026
 * Modified for Roofy Music (C) 2026
 * Licensed under GPL-3.0 | See git history for contributors
 */

package com.metrolist.music.ui.theme

import android.graphics.Bitmap
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.palette.graphics.Palette
import com.materialkolor.score.Score

// Monochrome default — no magenta accent in UI chrome
val DefaultThemeColor = RetroTokens.Text

private val MonochromeColorScheme = darkColorScheme(
    primary = RetroTokens.Text,
    onPrimary = RetroTokens.Background,
    primaryContainer = RetroTokens.Panel2,
    onPrimaryContainer = RetroTokens.Text,
    inversePrimary = RetroTokens.TextHot,
    secondary = RetroTokens.TextSoft,
    onSecondary = RetroTokens.Background,
    secondaryContainer = RetroTokens.Panel2,
    onSecondaryContainer = RetroTokens.Text,
    tertiary = RetroTokens.TextMuted,
    onTertiary = RetroTokens.Background,
    tertiaryContainer = RetroTokens.Panel2,
    onTertiaryContainer = RetroTokens.Text,
    background = RetroTokens.Background,
    onBackground = RetroTokens.Text,
    surface = RetroTokens.Background2,
    onSurface = RetroTokens.Text,
    surfaceVariant = RetroTokens.Panel,
    onSurfaceVariant = RetroTokens.TextSoft,
    surfaceTint = Color.Transparent,
    inverseSurface = RetroTokens.Text,
    inverseOnSurface = RetroTokens.Background,
    error = RetroTokens.TextMuted,
    onError = RetroTokens.Background,
    errorContainer = RetroTokens.Panel2,
    onErrorContainer = RetroTokens.Text,
    outline = RetroTokens.Border,
    outlineVariant = RetroTokens.BorderMuted,
    scrim = Color.Black,
    surfaceBright = RetroTokens.Panel3,
    surfaceDim = RetroTokens.Background,
    surfaceContainerLowest = RetroTokens.Background,
    surfaceContainerLow = RetroTokens.Background2,
    surfaceContainer = RetroTokens.Panel,
    surfaceContainerHigh = RetroTokens.Panel2,
    surfaceContainerHighest = RetroTokens.Panel3,
)

private val RetroShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(0.dp),
)

@Composable
fun RoofyMusicTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    pureBlack: Boolean = false,
    themeColor: Color = DefaultThemeColor,
    content: @Composable () -> Unit,
) {
    // Force monochrome — ignore dynamic theme color
    val colorScheme = remember(pureBlack, darkTheme) {
        MonochromeColorScheme.copy(
            primary = RetroTokens.Text,
            secondary = RetroTokens.TextSoft,
            background = if (pureBlack) Color.Black else RetroTokens.Background,
            surface = if (pureBlack) Color.Black else RetroTokens.Background2,
            surfaceContainerLowest = if (pureBlack) Color.Black else RetroTokens.Background,
            surfaceContainerLow = if (pureBlack) Color.Black else RetroTokens.Background2,
            surfaceContainer = if (pureBlack) Color.Black else RetroTokens.Panel,
            surfaceContainerHigh = if (pureBlack) Color.Black else RetroTokens.Panel2,
        )
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = RetroShapes,
    ) {
        Box(Modifier.fillMaxSize()) {
            content()
            RetroCrtOverlay()
        }
    }
}

@Composable
private fun RetroCrtOverlay() {
    Canvas(Modifier.fillMaxSize()) {
        val scanStep = 3.dp.toPx()
        val bright = Color.White.copy(alpha = 0.035f)
        val dark = Color.Black.copy(alpha = 0.08f)
        var y = 0f
        while (y < size.height) {
            drawLine(bright, start = androidx.compose.ui.geometry.Offset(0f, y), end = androidx.compose.ui.geometry.Offset(size.width, y), strokeWidth = 1f)
            drawLine(dark, start = androidx.compose.ui.geometry.Offset(0f, y + 2f), end = androidx.compose.ui.geometry.Offset(size.width, y + 2f), strokeWidth = 1f)
            y += scanStep
        }

        val dotStep = 6.dp.toPx()
        var dotY = 1f
        while (dotY < size.height) {
            var dotX = if (((dotY / dotStep).toInt() % 2) == 0) 1f else dotStep / 2
            while (dotX < size.width) {
                drawCircle(Color.White.copy(alpha = 0.016f), radius = 0.6f, center = androidx.compose.ui.geometry.Offset(dotX, dotY))
                dotX += dotStep
            }
            dotY += dotStep
        }
    }
}

fun Bitmap.extractThemeColor(): Color {
    val colorsToPopulation = Palette.from(this)
        .maximumColorCount(8)
        .generate()
        .swatches
        .associate { it.rgb to it.population }
    val rankedColors = Score.score(colorsToPopulation)
    return Color(rankedColors.first())
}

fun Bitmap.extractGradientColors(): List<Color> {
    val extractedColors = Palette.from(this)
        .maximumColorCount(64)
        .generate()
        .swatches
        .associate { it.rgb to it.population }

    val orderedColors = Score.score(extractedColors, 2, 0xff4285f4.toInt(), true)
        .sortedByDescending { Color(it).luminance() }

    return if (orderedColors.size >= 2)
        listOf(Color(orderedColors[0]), Color(orderedColors[1]))
    else
        listOf(Color(0xFF595959), Color(0xFF0D0D0D))
}

fun ColorScheme.pureBlack(apply: Boolean) =
    if (apply) copy(
        surface = Color.Black,
        background = Color.Black
    ) else this

val ColorSaver = object : Saver<Color, Int> {
    override fun restore(value: Int): Color = Color(value)
    override fun SaverScope.save(value: Color): Int = value.toArgb()
}

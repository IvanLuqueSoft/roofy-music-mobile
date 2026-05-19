package com.metrolist.music.ui.screens.wrapped.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

enum class ShapeType {
    Circle, Rect, Line
}

@Composable
internal fun AnimatedBackground(
    elementCount: Int = 20,
    shapeTypes: List<ShapeType> = listOf(ShapeType.Circle)
) {
    // No-op: retro terminal design uses solid black + CRT overlay (applied globally)
}

package com.example.doc.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp > configuration.screenHeightDp
}

@Composable
fun getScreenWidth(): Int {
    return LocalConfiguration.current.screenWidthDp
}

@Composable
fun getScreenHeight(): Int {
    return LocalConfiguration.current.screenHeightDp
}

/**
 * Obtiene el padding superior necesario según los insets del sistema.
 * Si hay notch/dynamic island/cámara, agrega padding extra.
 * Si no hay obstáculos, usa padding mínimo.
 */
@Composable
fun getAdaptiveTopPadding(): Dp {
    val density = LocalDensity.current
    val systemBarsInsets = WindowInsets.systemBars

    // Obtener el inset superior en pixels
    val topInsetPx = systemBarsInsets.getTop(density)

    // Convertir a dp
    val topInsetDp = with(density) { topInsetPx.toDp() }

    // Si hay inset significativo (> 24dp), significa que hay notch/dynamic island
    // Usar solo los insets del sistema sin padding adicional.
    // Si no hay insets, usar padding mínimo porque no hay obstrucciones.
    return if (topInsetDp > 24.dp) {
        topInsetDp // Solo los insets del sistema, sin padding adicional
    } else {
        8.dp // Padding mínimo cuando no hay obstrucciones
    }
}


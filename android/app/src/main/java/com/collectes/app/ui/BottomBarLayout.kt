package com.collectes.app.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Estimation du bandeau d'onglets avant mesure (hors barre système). */
val BottomBarTabRowHeight = 64.dp

val LocalBottomBarInset = compositionLocalOf { 0.dp }

@Composable
fun rememberBottomBarInset(barHeight: Dp): Dp = barHeight

@Composable
fun rememberBottomBarFallbackHeight(): Dp {
    val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    return BottomBarTabRowHeight + navBarBottom + 12.dp
}

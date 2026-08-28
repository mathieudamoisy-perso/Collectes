package com.smirtom.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Sage = Color(0xFF5E8F66)
private val SageSoft = Color(0xFFA8C5AC)
private val Mint = Color(0xFFDCEFDE)

private val LightColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    secondary = SageSoft,
    primaryContainer = Mint,
    onPrimaryContainer = Color(0xFF2F4A34)
)

private val DarkColors = darkColorScheme(
    primary = SageSoft,
    onPrimary = Color(0xFF2F4A34),
    secondary = Color(0xFFC5D9C8),
    primaryContainer = Color(0xFF3D5C43),
    onPrimaryContainer = Mint
)

@Composable
fun SmirtomTheme(content: @Composable () -> Unit) {
    val colors = if (isSystemInDarkTheme()) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

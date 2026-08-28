package com.smirtom.app.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val Sage = Color(0xFF5E8F66)
private val SageSoft = Color(0xFFA8C5AC)
private val Mint = Color(0xFFDCEFDE)

private val LightColors = lightColorScheme(
    primary = Sage,
    onPrimary = Color.White,
    secondary = SageSoft,
    onSecondary = Color(0xFF2F4A34),
    primaryContainer = Mint,
    onPrimaryContainer = Color(0xFF2F4A34),
    background = Color(0xFFF7FBF6),
    onBackground = Color(0xFF1A1C1A),
    surface = Color(0xFFF7FBF6),
    onSurface = Color(0xFF1A1C1A),
    surfaceContainerLow = Color(0xFFEEF3ED),
    surfaceContainerHighest = Color(0xFFE4EAE3)
)

private val DarkColors = darkColorScheme(
    primary = SageSoft,
    onPrimary = Color(0xFF2F4A34),
    secondary = Color(0xFFC5D9C8),
    onSecondary = Color(0xFF1A1C1A),
    primaryContainer = Color(0xFF3D5C43),
    onPrimaryContainer = Mint,
    background = Color(0xFF1A1C1A),
    onBackground = Color(0xFFE4EAE3),
    surface = Color(0xFF1A1C1A),
    onSurface = Color(0xFFE4EAE3),
    surfaceContainerLow = Color(0xFF222522),
    surfaceContainerHighest = Color(0xFF2C302C)
)

@Composable
fun SmirtomTheme(content: @Composable () -> Unit) {
    val darkTheme = isSystemInDarkTheme()
    val context = LocalContext.current
    val colors = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }

    MaterialTheme(
        colorScheme = colors,
        content = content
    )
}

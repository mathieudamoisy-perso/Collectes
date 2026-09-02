package com.smirtom.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.smirtom.app.data.WasteType

data class WasteTypePalette(
    val accent: Color,
    val container: Color,
    val containerMuted: Color
)

object WasteTypeColors {
    private val lightPalettes = mapOf(
        WasteType.ORDURES to WasteTypePalette(
            accent = Color(0xFF6E818C),
            container = Color(0xFFE6EDF0),
            containerMuted = Color(0xFFF3F6F7)
        ),
        WasteType.EMBALLAGES to WasteTypePalette(
            accent = Color(0xFFB89B3D),
            container = Color(0xFFFBF3D0),
            containerMuted = Color(0xFFFDF8E8)
        ),
        WasteType.VEGETAUX to WasteTypePalette(
            accent = Color(0xFF8B6914),
            container = Color(0xFFF0E4C8),
            containerMuted = Color(0xFFF7F0E3)
        ),
        WasteType.VERRE to WasteTypePalette(
            accent = Color(0xFF5E8F66),
            container = Color(0xFFDCEFDE),
            containerMuted = Color(0xFFEEF6EF)
        ),
        WasteType.ENCOMBRANTS to WasteTypePalette(
            accent = Color(0xFFC88462),
            container = Color(0xFFFCE4D4),
            containerMuted = Color(0xFFFDF1E8)
        )
    )

    private val darkPalettes = mapOf(
        WasteType.ORDURES to WasteTypePalette(
            accent = Color(0xFFB0BEC5),
            container = Color(0xFF2A3338),
            containerMuted = Color(0xFF232A2E)
        ),
        WasteType.EMBALLAGES to WasteTypePalette(
            accent = Color(0xFFE6D48A),
            container = Color(0xFF3A3520),
            containerMuted = Color(0xFF2E2B1C)
        ),
        WasteType.VEGETAUX to WasteTypePalette(
            accent = Color(0xFFD4BC82),
            container = Color(0xFF3A3220),
            containerMuted = Color(0xFF2E281A)
        ),
        WasteType.VERRE to WasteTypePalette(
            accent = Color(0xFFA8C5AC),
            container = Color(0xFF243328),
            containerMuted = Color(0xFF1E2A22)
        ),
        WasteType.ENCOMBRANTS to WasteTypePalette(
            accent = Color(0xFFE8B89A),
            container = Color(0xFF3A2C24),
            containerMuted = Color(0xFF2E241E)
        )
    )

    fun paletteFor(type: WasteType, darkTheme: Boolean): WasteTypePalette {
        val palettes = if (darkTheme) darkPalettes else lightPalettes
        return palettes.getValue(type)
    }

    @Composable
    fun palette(type: WasteType): WasteTypePalette = paletteFor(type, isSystemInDarkTheme())

    fun accent(type: WasteType, darkTheme: Boolean = false): Color = paletteFor(type, darkTheme).accent

    fun accentArgb(type: WasteType): Int = accent(type, darkTheme = false).toArgb()

    fun cardBackground(type: WasteType, darkTheme: Boolean = false): Color =
        paletteFor(type, darkTheme).container

    fun cardBackground(types: List<WasteType>, darkTheme: Boolean = false): Color {
        if (types.isEmpty()) return Color.Unspecified
        return cardBackground(types.first(), darkTheme)
    }

    @Composable
    fun cardBackgroundOrDefault(types: List<WasteType>): Color {
        val tint = cardBackground(types, isSystemInDarkTheme())
        return if (tint == Color.Unspecified) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            tint
        }
    }
}

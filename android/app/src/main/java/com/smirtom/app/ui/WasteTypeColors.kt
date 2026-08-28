package com.smirtom.app.ui

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
    fun palette(type: WasteType): WasteTypePalette = when (type) {
        WasteType.ORDURES -> WasteTypePalette(
            accent = Color(0xFF6E818C),
            container = Color(0xFFE6EDF0),
            containerMuted = Color(0xFFF3F6F7)
        )
        WasteType.EMBALLAGES -> WasteTypePalette(
            accent = Color(0xFFB89B3D),
            container = Color(0xFFFBF3D0),
            containerMuted = Color(0xFFFDF8E8)
        )
        WasteType.VERRE -> WasteTypePalette(
            accent = Color(0xFF5E8F66),
            container = Color(0xFFDCEFDE),
            containerMuted = Color(0xFFEEF6EF)
        )
        WasteType.ENCOMBRANTS -> WasteTypePalette(
            accent = Color(0xFFC88462),
            container = Color(0xFFFCE4D4),
            containerMuted = Color(0xFFFDF1E8)
        )
    }

    fun accent(type: WasteType): Color = palette(type).accent

    fun accentArgb(type: WasteType): Int = accent(type).toArgb()

    fun cardBackground(type: WasteType): Color = palette(type).container

    fun cardBackground(types: List<WasteType>): Color {
        if (types.isEmpty()) return Color.Unspecified
        return cardBackground(types.first())
    }

    @Composable
    fun cardBackgroundOrDefault(types: List<WasteType>): Color {
        val tint = cardBackground(types)
        return if (tint == Color.Unspecified) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            tint
        }
    }
}

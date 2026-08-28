package com.smirtom.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.smirtom.app.data.WasteType

object WasteTypeColors {
    fun cardBackground(type: WasteType): Color = when (type) {
        WasteType.ORDURES -> Color(0xFFECEFF1)
        WasteType.EMBALLAGES -> Color(0xFFFFF8E1)
        WasteType.VERRE -> Color(0xFFE8F5E9)
        WasteType.ENCOMBRANTS -> Color(0xFFFFF3E0)
    }

    fun cardBackground(types: List<WasteType>): Color {
        if (types.isEmpty()) return Color.Unspecified
        if (types.size == 1) return cardBackground(types.first())
        return cardBackground(types.first()).copy(alpha = 0.85f)
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

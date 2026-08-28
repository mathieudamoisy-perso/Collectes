package com.smirtom.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.smirtom.app.data.WasteType

object WasteTypeColors {
    fun accent(type: WasteType): Color = when (type) {
        WasteType.ORDURES -> Color(0xFF546E7A)
        WasteType.EMBALLAGES -> Color(0xFFF9A825)
        WasteType.VERRE -> Color(0xFF2E7D32)
        WasteType.ENCOMBRANTS -> Color(0xFFE65100)
    }

    fun cardBackground(type: WasteType): Color = when (type) {
        WasteType.ORDURES -> Color(0xFFCFD8DC)
        WasteType.EMBALLAGES -> Color(0xFFFFF176)
        WasteType.VERRE -> Color(0xFF81C784)
        WasteType.ENCOMBRANTS -> Color(0xFFFFB74D)
    }

    fun cardBackground(types: List<WasteType>): Color {
        if (types.isEmpty()) return Color.Unspecified
        if (types.size == 1) return cardBackground(types.first())
        return cardBackground(types.first()).copy(alpha = 0.9f)
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

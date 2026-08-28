package com.smirtom.app.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.EventBusy
import androidx.compose.material.icons.outlined.Liquor
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Weekend
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.smirtom.app.data.WasteType

object WasteTypeIcons {
    fun imageVector(type: WasteType): ImageVector = when (type) {
        WasteType.ORDURES -> Icons.Outlined.DeleteOutline
        WasteType.EMBALLAGES -> Icons.Outlined.Recycling
        WasteType.VERRE -> Icons.Outlined.Liquor
        WasteType.ENCOMBRANTS -> Icons.Outlined.Weekend
    }
}

@Composable
fun WasteTypeIcon(
    type: WasteType,
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = WasteTypeColors.palette(type).accent
) {
    Icon(
        imageVector = WasteTypeIcons.imageVector(type),
        contentDescription = type.label,
        modifier = modifier.size(size),
        tint = tint
    )
}

@Composable
fun AllCollectionsIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Icon(
        imageVector = Icons.Outlined.CalendarMonth,
        contentDescription = "Toutes les collectes",
        modifier = modifier.size(size),
        tint = tint
    )
}

@Composable
fun NoCollectionIcon(
    modifier: Modifier = Modifier,
    size: Dp = 20.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Icon(
        imageVector = Icons.Outlined.EventBusy,
        contentDescription = "Aucune collecte",
        modifier = modifier.size(size),
        tint = tint
    )
}

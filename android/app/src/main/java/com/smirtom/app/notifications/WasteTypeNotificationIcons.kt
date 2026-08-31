package com.smirtom.app.notifications

import androidx.annotation.DrawableRes
import com.smirtom.app.R
import com.smirtom.app.data.WasteType

object WasteTypeNotificationIcons {
    @DrawableRes
    fun smallIconRes(type: WasteType): Int = when (type) {
        WasteType.ORDURES -> R.drawable.ic_notif_ordures
        WasteType.EMBALLAGES -> R.drawable.ic_notif_emballages
        WasteType.VERRE -> R.drawable.ic_notif_verre
        WasteType.ENCOMBRANTS -> R.drawable.ic_notif_encombrants
    }

    fun primaryType(wasteTypes: List<WasteType>): WasteType {
        return wasteTypes.minByOrNull { it.ordinal } ?: WasteType.ORDURES
    }

    fun notificationTitle(wasteTypes: List<WasteType>): String = when {
        wasteTypes.size == 1 -> "Rappel : ${wasteTypes.first().label}"
        wasteTypes.isEmpty() -> "Rappel collecte"
        else -> "Rappel : ${wasteTypes.size} collectes demain"
    }
}

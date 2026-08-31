package com.smirtom.app.notifications

import androidx.annotation.DrawableRes
import com.smirtom.app.R
import com.smirtom.app.data.WasteType

object WasteTypeNotificationIcons {
    /** Icône barre de statut : camion monochrome (exigence Android). */
    @DrawableRes
    fun smallIconRes(): Int = R.drawable.ic_notification

    fun primaryType(wasteTypes: List<WasteType>): WasteType {
        return wasteTypes.minByOrNull { it.ordinal } ?: WasteType.ORDURES
    }

    fun notificationTitle(wasteTypes: List<WasteType>): String = when {
        wasteTypes.size == 1 -> "Rappel : ${wasteTypes.first().label}"
        wasteTypes.isEmpty() -> "Rappel collecte"
        else -> "Rappel : ${wasteTypes.size} collectes demain"
    }
}

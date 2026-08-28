package com.smirtom.app.notifications

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.smirtom.app.R
import com.smirtom.app.data.WasteType
import com.smirtom.app.ui.WasteTypeColors

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
        wasteTypes.isEmpty() -> "Rappel collecte Poubelles"
        else -> "Rappel : ${wasteTypes.size} collectes demain"
    }

    fun buildLargeIcon(context: Context, type: WasteType): Bitmap {
        val drawable = ContextCompat.getDrawable(context, smallIconRes(type))!!.mutate()
        DrawableCompat.setTint(drawable, WasteTypeColors.accentArgb(type))

        val sizePx = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(128)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, sizePx, sizePx)
        drawable.draw(canvas)
        return bitmap
    }
}

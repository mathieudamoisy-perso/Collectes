package com.smirtom.app.notifications

import com.smirtom.app.data.WasteType
import org.junit.Assert.assertEquals
import org.junit.Test

class WasteTypeNotificationIconsTest {
    @Test
    fun primaryTypePrefersLowestOrdinal() {
        val types = listOf(WasteType.ENCOMBRANTS, WasteType.ORDURES, WasteType.VERRE)
        assertEquals(WasteType.ORDURES, WasteTypeNotificationIcons.primaryType(types))
    }

    @Test
    fun notificationTitleForSingleType() {
        val title = WasteTypeNotificationIcons.notificationTitle(listOf(WasteType.VERRE))
        assertEquals("Rappel : Verre", title)
    }

    @Test
    fun notificationTitleForMultipleTypes() {
        val title = WasteTypeNotificationIcons.notificationTitle(
            listOf(WasteType.ORDURES, WasteType.EMBALLAGES)
        )
        assertEquals("Rappel : 2 collectes demain", title)
    }
}

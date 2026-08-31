package com.smirtom.app.notifications

import android.content.Context
import com.smirtom.app.data.WasteType
import java.time.LocalDate
import kotlin.random.Random

object NotificationTestHelper {
    const val TEST_NOTIFICATION_ID = Int.MAX_VALUE

    fun randomCollectionEvent(today: LocalDate = LocalDate.now(), random: Random = Random.Default): Pair<LocalDate, List<WasteType>> {
        val typeCount = random.nextInt(WasteType.entries.size) + 1
        val wasteTypes = WasteType.entries.shuffled(random).take(typeCount).sortedBy { it.ordinal }
        // Demain, comme pour un vrai rappel : le texte et le titre restent cohérents.
        val collectionDate = today.plusDays(1)
        return collectionDate to wasteTypes
    }

    fun showRandomTestReminder(context: Context): Boolean {
        NotificationHelper.createChannel(context)
        if (!NotificationHelper.canPostNotifications(context)) return false

        val (collectionDate, wasteTypes) = randomCollectionEvent()
        val message = ReminderScheduler.formatReminderMessage(collectionDate, wasteTypes)
        NotificationHelper.showReminder(
            context = context,
            notificationId = TEST_NOTIFICATION_ID,
            wasteTypes = wasteTypes,
            message = message
        )
        return true
    }
}

package com.collectes.app.data

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object SyncStatusFormatter {
    private val zoneId = ZoneId.of("Europe/Paris")
    private val dateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRENCH)

    fun format(lastSync: Instant): String {
        val date = lastSync.atZone(zoneId).toLocalDate()
        return "Dernière synchro : ${date.format(dateFormatter)}"
    }
}

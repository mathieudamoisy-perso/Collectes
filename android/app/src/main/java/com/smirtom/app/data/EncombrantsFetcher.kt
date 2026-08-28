package com.smirtom.app.data

import java.time.LocalDate

class EncombrantsFetcher {
    companion object {
        private val DATE_PATTERN = Regex("""(\d{2})/(\d{2})/(\d{4})""")
    }

    fun fetchDates(year: Int, commune: VexinCommune): List<LocalDate> {
        return runCatching { fetchFromCommunePage(year, commune) }.getOrDefault(emptyList())
    }

    private fun fetchFromCommunePage(year: Int, commune: VexinCommune): List<LocalDate> {
        val text = SmirtomHttp.document(commune.pageUrl).text()
        val encombrantsIndex = text.indexOf("encombrants", ignoreCase = true)
        if (encombrantsIndex < 0) return emptyList()

        val sectionEnd = text.indexOf("Point(s) d'apport", encombrantsIndex).let {
            if (it < 0) (encombrantsIndex + 400).coerceAtMost(text.length) else it
        }
        val section = text.substring(encombrantsIndex, sectionEnd)

        return DATE_PATTERN.findAll(section)
            .mapNotNull { match ->
                val day = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                val month = match.groupValues[2].toIntOrNull() ?: return@mapNotNull null
                val matchYear = match.groupValues[3].toIntOrNull() ?: return@mapNotNull null
                runCatching { LocalDate.of(matchYear, month, day) }.getOrNull()
            }
            .filter { it.year == year }
            .distinct()
            .sorted()
            .toList()
    }

    fun toCollectionDays(dates: List<LocalDate>): List<CollectionDay> {
        return dates.map { date ->
            CollectionDay(date, listOf(WasteType.ENCOMBRANTS))
        }
    }
}

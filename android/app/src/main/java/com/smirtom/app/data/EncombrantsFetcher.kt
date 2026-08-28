package com.smirtom.app.data

import org.jsoup.Jsoup
import java.time.LocalDate

class EncombrantsFetcher {
    companion object {
        const val COMMUNE_PAGE_URL = "https://smirtomduvexin.net/informations_utiles/magny-en-vexin/"
        private val DATE_PATTERN = Regex("""(\d{2})/(\d{2})/(\d{4})""")
    }

    fun fetchDates(year: Int): List<LocalDate> {
        val fromWeb = runCatching { fetchFromCommunePage(year) }.getOrNull()
        if (!fromWeb.isNullOrEmpty()) return fromWeb
        return fallbackDates(year)
    }

    private fun fetchFromCommunePage(year: Int): List<LocalDate> {
        val document = Jsoup.connect(COMMUNE_PAGE_URL)
            .userAgent("SmirtomApp/1.0")
            .timeout(30_000)
            .get()

        val text = document.text()
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

    private fun fallbackDates(year: Int): List<LocalDate> {
        return when (year) {
            2026 -> listOf(
                LocalDate.of(2026, 5, 21),
                LocalDate.of(2026, 11, 18)
            )
            else -> emptyList()
        }
    }

    fun toCollectionDays(dates: List<LocalDate>): List<CollectionDay> {
        return dates.map { date ->
            CollectionDay(date, listOf(WasteType.ENCOMBRANTS))
        }
    }
}

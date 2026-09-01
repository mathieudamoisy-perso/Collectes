package com.smirtom.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

/**
 * Calendriers municipaux Emeraude (Ermont, Sannois) : rythmes « Nᵉ jour du mois »
 * et collectes hebdo / bihebdo distinctes du modèle SMIRTOM.
 */
object MunicipalCalendarParser {
    private val MONTHLY_COLLECT = Regex(
        """collecte\s*:\s*(\d{1,2})(?:er|e|ème|eme)?\s*(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)\s+du\s+mois""",
        RegexOption.IGNORE_CASE
    )
    private val WEEKDAY_COLLECT = Regex(
        """collecte\s*:\s*(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)(?:\s+(?:matin|soir))?""",
        RegexOption.IGNORE_CASE
    )

    fun parseIfPresent(text: String, year: Int): CollectionRules? {
        val normalized = CollectionRulesParser.normalizeSource(text)
        if (!normalized.contains("pavillons")) return null

        val section = extractPavillonsSection(normalized) ?: return null
        val orduresDay = weekdayAfterKeyword(section, "ordures")
            ?: return null
        val emballagesDay = weekdayAfterKeyword(section, "emballages", "papiers")
            ?: return null

        val verreMonthly = monthlyAfterKeyword(section, "verre") ?: return null
        val encombrantsMonthly = monthlyAfterKeyword(section, "encombrants")

        val emballagesAnchor = CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, emballagesDay)
        return CollectionRules(
            orduresDay = orduresDay,
            emballagesDay = emballagesDay,
            emballagesAnchor = emballagesAnchor,
            verreDay = verreMonthly.first,
            verreAnchor = emballagesAnchor,
            orduresRecurrence = CollectionRecurrence.WEEKLY,
            emballagesRecurrence = CollectionRecurrence.BIWEEKLY,
            verreRecurrence = CollectionRecurrence.MONTHLY_NTH_WEEKDAY,
            verreMonthOrdinal = verreMonthly.second,
            encombrantsDay = encombrantsMonthly?.first,
            encombrantsMonthOrdinal = encombrantsMonthly?.second
        )
    }

    private fun extractPavillonsSection(text: String): String? {
        val start = text.indexOf("pavillons")
        if (start < 0) return null
        val endMarkers = listOf("collectifs de -", "collectifs de")
        val end = endMarkers
            .map { text.indexOf(it, start + 9) }
            .filter { it > start }
            .minOrNull() ?: (start + 1200).coerceAtMost(text.length)
        return text.substring(start, end)
    }

    private fun weekdayAfterKeyword(section: String, vararg keywords: String): DayOfWeek? {
        for (keyword in keywords) {
            val index = section.indexOf(keyword)
            if (index < 0) continue
            val window = section.substring(index, (index + 120).coerceAtMost(section.length))
            WEEKDAY_COLLECT.find(window)?.let { match ->
                return frenchDayToDayOfWeek(match.groupValues[1])
            }
        }
        return null
    }

    private fun monthlyAfterKeyword(section: String, keyword: String): Pair<DayOfWeek, Int>? {
        val index = section.indexOf(keyword)
        if (index < 0) return null
        val window = section.substring(index, (index + 120).coerceAtMost(section.length))
        val match = MONTHLY_COLLECT.find(window) ?: return null
        val ordinal = match.groupValues[1].toIntOrNull() ?: return null
        val day = frenchDayToDayOfWeek(match.groupValues[2]) ?: return null
        return day to ordinal
    }

    private fun frenchDayToDayOfWeek(day: String): DayOfWeek? {
        return when (day.lowercase(Locale.FRENCH)) {
            "lundi" -> DayOfWeek.MONDAY
            "mardi" -> DayOfWeek.TUESDAY
            "mercredi" -> DayOfWeek.WEDNESDAY
            "jeudi" -> DayOfWeek.THURSDAY
            "vendredi" -> DayOfWeek.FRIDAY
            "samedi" -> DayOfWeek.SATURDAY
            "dimanche" -> DayOfWeek.SUNDAY
            else -> null
        }
    }
}

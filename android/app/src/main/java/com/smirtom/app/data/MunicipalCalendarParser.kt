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

    private val MONTH_NAMES =
        """janvier|janv|février|fevrier|févr|fevr|mars|avril|avr|mai|juin|juillet|juil|août|aout|septembre|sept|octobre|oct|novembre|nov|décembre|decembre|déc|dec"""
    private val VEGETAUX_SAME_MONTH_RANGE = Regex(
        """du\s+(\d{1,2}|1er|[123]o)(?:er|e|ème|eme)?\s+au\s+(\d{1,2}|1er|[123]o)(?:er|e|ème|eme)?\s+($MONTH_NAMES)""",
        RegexOption.IGNORE_CASE
    )
    private val VEGETAUX_CROSS_MONTH_RANGE = Regex(
        """du\s+(\d{1,2}|1er|[123]o)(?:er|e|ème|eme)?\s+($MONTH_NAMES)\s+au\s+(\d{1,2}|1er|[123]o)(?:er|e|ème|eme)?\s+($MONTH_NAMES)""",
        RegexOption.IGNORE_CASE
    )
    private val VEGETAUX_KEYWORD = Regex(
        """v[eéèêëã‰]+g[eéèêëã‰]+taux""",
        RegexOption.IGNORE_CASE
    )
    private val VEGETAUX_NAMED_DATE = Regex(
        """(?:lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)\s+(\d{1,2}|1er|[123]o)(?:er|e|ème|eme)?\s+($MONTH_NAMES)""",
        RegexOption.IGNORE_CASE
    )
    private val MONTH_MAP = mapOf(
        "janvier" to 1, "janv" to 1,
        "février" to 2, "fevrier" to 2, "févr" to 2, "fevr" to 2,
        "mars" to 3, "avril" to 4, "avr" to 4,
        "mai" to 5, "juin" to 6, "juillet" to 7, "juil" to 7,
        "août" to 8, "aout" to 8,
        "septembre" to 9, "sept" to 9,
        "octobre" to 10, "oct" to 10,
        "novembre" to 11, "nov" to 11,
        "décembre" to 12, "decembre" to 12, "déc" to 12, "dec" to 12
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
        val vegetauxSchedule = vegetauxScheduleAfterKeyword(section)
        val emballagesRecurrence = emballagesRecurrence(section)

        val emballagesAnchor = CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, emballagesDay)
        return CollectionRules(
            orduresDay = orduresDay,
            emballagesDay = emballagesDay,
            emballagesAnchor = emballagesAnchor,
            verreDay = verreMonthly.first,
            verreAnchor = emballagesAnchor,
            orduresRecurrence = CollectionRecurrence.WEEKLY,
            emballagesRecurrence = emballagesRecurrence,
            verreRecurrence = CollectionRecurrence.MONTHLY_NTH_WEEKDAY,
            verreMonthOrdinal = verreMonthly.second,
            encombrantsDay = encombrantsMonthly?.first,
            encombrantsMonthOrdinal = encombrantsMonthly?.second,
            vegetauxSchedule = vegetauxSchedule
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

    private fun emballagesRecurrence(section: String): CollectionRecurrence {
        val index = section.indexOf("emballages").takeIf { it >= 0 }
            ?: section.indexOf("papiers").takeIf { it >= 0 }
            ?: return CollectionRecurrence.WEEKLY
        val window = section.substring(index, (index + 160).coerceAtMost(section.length))
        return if (window.contains("2 semaines") || window.contains("deux semaines")) {
            CollectionRecurrence.BIWEEKLY
        } else {
            CollectionRecurrence.WEEKLY
        }
    }

    private fun vegetauxScheduleAfterKeyword(section: String): VegetauxSchedule? {
        val index = VEGETAUX_KEYWORD.find(section)?.range?.first ?: return null
        val window = normalizeOcrDigits(
            section.substring(index, (index + 220).coerceAtMost(section.length))
        )
        val day = WEEKDAY_COLLECT.find(window)?.let { frenchDayToDayOfWeek(it.groupValues[1]) }
            ?: return null

        val ranges = buildList {
            VEGETAUX_SAME_MONTH_RANGE.findAll(window).forEach { match ->
                val startDay = parseDayNumber(match.groupValues[1]) ?: return@forEach
                val endDay = parseDayNumber(match.groupValues[2]) ?: return@forEach
                val month = monthNumber(match.groupValues[3]) ?: return@forEach
                add(MonthDayRange(MonthDay(month, startDay), MonthDay(month, endDay)))
            }
            VEGETAUX_CROSS_MONTH_RANGE.findAll(window).forEach { match ->
                val startDay = parseDayNumber(match.groupValues[1]) ?: return@forEach
                val startMonth = monthNumber(match.groupValues[2]) ?: return@forEach
                val endDay = parseDayNumber(match.groupValues[3]) ?: return@forEach
                val endMonth = monthNumber(match.groupValues[4]) ?: return@forEach
                add(MonthDayRange(MonthDay(startMonth, startDay), MonthDay(endMonth, endDay)))
            }
        }
        val activeRanges = ranges.ifEmpty {
            listOf(MonthDayRange(MonthDay(1, 1), MonthDay(12, 31)))
        }

        val extraDates = VEGETAUX_NAMED_DATE.findAll(window).mapNotNull { match ->
            val dayOfMonth = parseDayNumber(match.groupValues[1]) ?: return@mapNotNull null
            val month = monthNumber(match.groupValues[2]) ?: return@mapNotNull null
            MonthDay(month, dayOfMonth)
        }.toList()

        return VegetauxSchedule(
            dayOfWeek = day,
            activeRanges = activeRanges,
            extraDates = extraDates
        )
    }

    private fun monthNumber(raw: String): Int? {
        return MONTH_MAP[raw.lowercase(Locale.FRENCH)]
    }

    private fun normalizeOcrDigits(text: String): String {
        return text.replace(Regex("""(\d)o""")) { "${it.groupValues[1]}0" }
    }

    private fun parseDayNumber(raw: String): Int? {
        val normalized = raw.lowercase(Locale.FRENCH).replace("er", "").replace("ème", "").replace("eme", "")
        if (normalized == "1") return 1
        return normalized.toIntOrNull()
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

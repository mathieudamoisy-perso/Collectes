package com.smirtom.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

object CollectionRulesParser {
    private val DAY_PATTERN = Regex(
        """\b(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)\b""",
        RegexOption.IGNORE_CASE
    )

    private val MONTH_MAP = mapOf(
        "janv" to 1, "févr" to 2, "fevr" to 2, "mars" to 3, "avr" to 4,
        "mai" to 5, "juin" to 6, "juil" to 7, "août" to 8, "aout" to 8,
        "sept" to 9, "oct" to 10, "nov" to 11, "déc" to 12, "dec" to 12
    )

    private val DATE_PATTERN = Regex(
        """(\d{1,2})\s*(janv|févr|fevr|mars|avr|mai|juin|juil|août|aout|sept|oct|nov|déc|dec)-?(\d{2})"""
    )

    fun parse(text: String, year: Int, communeName: String? = null): CollectionRules {
        val normalized = text.lowercase(Locale.FRENCH).replace('\u00a0', ' ')
        val communeWindow = communeName?.let { extractCommuneWindow(normalized, it) } ?: normalized

        val orduresDay = findDayNearKeyword(
            communeWindow,
            listOf("ordures ménagères", "ordures menageres", "ordures ménageres")
        ) ?: DayOfWeek.MONDAY

        val emballagesDay = findDayNearKeyword(
            communeWindow,
            listOf("emballages / papiers", "emballages", "papiers")
        ) ?: DayOfWeek.TUESDAY

        val verreDay = findVerreDay(communeWindow) ?: DayOfWeek.TUESDAY

        val emballagesAnchor = findAnchorDate(communeWindow, year, emballagesDay)
            ?: CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, emballagesDay)

        val verreAnchor = resolveVerreAnchor(
            text = communeWindow,
            year = year,
            verreDay = verreDay,
            emballagesDay = emballagesDay,
            emballagesAnchor = emballagesAnchor
        )

        return CollectionRules(
            orduresDay = orduresDay,
            emballagesDay = emballagesDay,
            emballagesAnchor = emballagesAnchor,
            verreDay = verreDay,
            verreAnchor = verreAnchor
        )
    }

    private fun extractCommuneWindow(text: String, communeName: String): String {
        val normalizedCommune = communeName.lowercase(Locale.FRENCH)
            .replace("-", " ")
            .replace("'", " ")
        val index = text.indexOf(normalizedCommune)
        if (index < 0) return text
        val start = (index - 80).coerceAtLeast(0)
        val end = (index + 280).coerceAtMost(text.length)
        return text.substring(start, end)
    }

    private fun findDayNearKeyword(text: String, keywords: List<String>): DayOfWeek? {
        for (keyword in keywords) {
            val index = text.indexOf(keyword)
            if (index >= 0) {
                val before = text.substring((index - 80).coerceAtLeast(0), index)
                val after = text.substring(index, (index + 120).coerceAtMost(text.length))
                DAY_PATTERN.findAll(before).lastOrNull()?.let { return frenchDayToDayOfWeek(it.value) }
                DAY_PATTERN.findAll(after).firstOrNull()?.let { return frenchDayToDayOfWeek(it.value) }
            }
        }
        return null
    }

    private fun findVerreDay(text: String): DayOfWeek? {
        val verreIndex = text.indexOf("verre")
        if (verreIndex < 0) return null
        val window = text.substring((verreIndex - 160).coerceAtLeast(0), verreIndex)
        return DAY_PATTERN.findAll(window).lastOrNull()?.let { frenchDayToDayOfWeek(it.value) }
    }

    private fun findAnchorDate(text: String, year: Int, dayOfWeek: DayOfWeek): LocalDate? {
        return DATE_PATTERN.findAll(text)
            .mapNotNull { match ->
                val day = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                val month = MONTH_MAP[match.groupValues[2]] ?: return@mapNotNull null
                val yearSuffix = match.groupValues[3].toIntOrNull() ?: return@mapNotNull null
                runCatching { LocalDate.of(2000 + yearSuffix, month, day) }.getOrNull()
            }
            .filter { it.year == year && it.dayOfWeek == dayOfWeek }
            .minByOrNull { it.dayOfYear }
    }

    private fun resolveVerreAnchor(
        text: String,
        year: Int,
        verreDay: DayOfWeek,
        emballagesDay: DayOfWeek,
        emballagesAnchor: LocalDate
    ): LocalDate {
        val fromText = findAnchorDate(text, year, verreDay)
        if (fromText != null && fromText != emballagesAnchor) return fromText

        return if (emballagesDay == verreDay) {
            emballagesAnchor.plusDays(7)
        } else {
            findAnchorDate(text, year, verreDay)
                ?: CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, verreDay)
        }
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

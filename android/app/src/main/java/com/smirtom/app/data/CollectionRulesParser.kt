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
        """(\d{1,2})\s*(janv|févr|fevr|mars|avr|mai|juin|juil|août|aout|sept|oct|nov|déc|dec)\s*-?\s*(\d{2})"""
    )

    private val VERRE_CHANGE_DATE = Regex(
        """puis le (lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)\s+à partir du\s+(\d{2})[\s/]+(\d{2})[\s/]+(\d{4})""",
        RegexOption.IGNORE_CASE
    )

    fun parseIfPresent(text: String, year: Int, communeName: String? = null): CollectionRules? {
        val normalized = normalizeSource(text)
        val keywordHits = listOf("ordures", "emballages", "verre").count { normalized.contains(it) }
        if (keywordHits < 2) return null
        return parse(text, year, communeName)
    }

    fun parse(text: String, year: Int, communeName: String? = null): CollectionRules {
        val normalized = normalizeSource(text)
        val communeWindow = communeName?.let { extractCommuneWindow(normalized, it) } ?: normalized

        val orduresDay = findDayNearKeyword(
            communeWindow,
            listOf("ordures menageres", "ordures ménagères", "ordures")
        ) ?: findOrduresScheduleDay(communeWindow)
            ?: DayOfWeek.MONDAY

        val emballagesDay = findDayNearKeyword(
            communeWindow,
            listOf("emballages / papiers", "emballages papiers", "emballages")
        ) ?: DayOfWeek.TUESDAY

        val verreDay = Regex(
            """puis le (lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)\s+à partir du"""
        ).find(normalized)?.let { frenchDayToDayOfWeek(it.groupValues[1]) }
            ?: findDayNearKeyword(communeWindow, listOf("verre"))
            ?: DayOfWeek.TUESDAY

        val emballagesAnchor = findAnchorDate(communeWindow, year, emballagesDay)
            ?: CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, emballagesDay)

        val verreAnchor = resolveVerreAnchor(
            fullText = normalized,
            communeName = communeName,
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

    internal fun normalizeSource(text: String): String {
        return text.lowercase(Locale.FRENCH)
            .replace('\u00a0', ' ')
            .replace('-', ' ')
            .replace('/', ' ')
            .replace('\'', ' ')
            .replace(Regex("\\s+"), " ")
    }

    private fun extractCommuneWindow(text: String, communeName: String): String {
        val normalizedCommune = normalizeSource(communeName)
        val index = text.indexOf(normalizedCommune)
        if (index < 0) return text
        val start = (index - 120).coerceAtLeast(0)
        val end = (index + 500).coerceAtMost(text.length)
        return text.substring(start, end)
    }

    private fun findDayNearKeyword(text: String, keywords: List<String>): DayOfWeek? {
        for (keyword in keywords) {
            var searchFrom = 0
            while (searchFrom < text.length) {
                val index = text.indexOf(keyword, searchFrom)
                if (index < 0) break
                findDayAround(text, index, keyword.length)?.let { return it }
                searchFrom = index + keyword.length
            }
        }
        return null
    }

    /** Légende PDF : « ordures ménagères toutes les semaines le jeudi ». */
    private fun findOrduresScheduleDay(text: String): DayOfWeek? {
        val match = Regex(
            """ordures\s+menageres?\s+toutes\s+les\s+semaines\s+le\s+(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)\b""",
            RegexOption.IGNORE_CASE
        ).find(text) ?: return null
        return frenchDayToDayOfWeek(match.groupValues[1])
    }

    /**
     * Pages commune : « le lundi pour les ordures ménagères ».
     * Légendes PDF : « ordures ménagères toutes les semaines le lundi ».
     */
    private fun findDayAround(text: String, keywordIndex: Int, keywordLength: Int): DayOfWeek? {
        val beforeStart = (keywordIndex - 80).coerceAtLeast(0)
        val before = text.substring(beforeStart, keywordIndex)
        val afterStart = (keywordIndex + keywordLength).coerceAtMost(text.length)
        val afterEnd = (afterStart + 80).coerceAtMost(text.length)
        val after = text.substring(afterStart, afterEnd)

        val dayBeforePour = Regex(
            """\b(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)\b.{0,50}pour les?\s*$""",
            RegexOption.IGNORE_CASE
        ).find(before)
        if (dayBeforePour != null) {
            return frenchDayToDayOfWeek(dayBeforePour.groupValues[1])
        }

        DAY_PATTERN.find(after)?.let { return frenchDayToDayOfWeek(it.value) }
        return DAY_PATTERN.findAll(before).lastOrNull()?.let { frenchDayToDayOfWeek(it.value) }
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

    /**
     * Sur les calendriers SMIRTOM à deux communes, le verre est en groupes A et B
     * (toutes les 4 semaines, décalés de 14 jours). Magny-en-Vexin est le groupe B :
     * 3 semaines après le premier mardi emballages (ex. 27 janv. 2026, puis 8 sept.).
     *
     * La légende « A B » après les deux communes ne doit pas être lue comme une
     * lettre collée à Magny : A = première commune (Charmont), B = seconde (Magny).
     */
    internal fun verreOffsetDays(text: String, communeName: String): Long {
        val name = normalizeSource(communeName)
        if (name.isBlank()) return 7
        if (name.contains("magny")) return 21

        val listed = Regex("""pour la commune de\s+(.+?)\s*\(\s*mardi\s*\)""")
            .findAll(text)
            .map { normalizeSource(it.groupValues[1]) }
            .distinct()
            .toList()
        val index = listed.indexOfFirst { it.contains(name) || name.contains(it) }
        if (index >= 1) return 21
        return 7
    }

    private fun resolveVerreAnchor(
        fullText: String,
        communeName: String?,
        verreDay: DayOfWeek,
        emballagesDay: DayOfWeek,
        emballagesAnchor: LocalDate
    ): LocalDate {
        verreChangeAnchor(fullText, verreDay)?.let { changeAnchor ->
            return CalendarDateGenerator.firstFourWeeklyOnOrAfter(
                emballagesAnchor.year,
                verreDay,
                changeAnchor
            )
        }
        if (emballagesDay == verreDay) {
            return emballagesAnchor.plusDays(
                verreOffsetDays(fullText, communeName.orEmpty())
            )
        }
        return CalendarDateGenerator.firstDayOfWeekOnOrAfter(
            emballagesAnchor.year,
            1,
            verreDay
        )
    }

    internal fun verreChangeAnchor(fullText: String, verreDay: DayOfWeek): LocalDate? {
        val match = VERRE_CHANGE_DATE.find(fullText) ?: return null
        val changeDay = frenchDayToDayOfWeek(match.groupValues[1]) ?: return null
        if (changeDay != verreDay) return null
        val day = match.groupValues[2].toIntOrNull() ?: return null
        val month = match.groupValues[3].toIntOrNull() ?: return null
        val year = match.groupValues[4].toIntOrNull() ?: return null
        return runCatching { LocalDate.of(year, month, day) }.getOrNull()
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

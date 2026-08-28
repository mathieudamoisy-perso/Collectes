package com.smirtom.app.data

import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Lettres A/B de la grille PDF SMIRTOM (`Ma 8 B`, `L 12 A`, …).
 * Permet de choisir le bon cycle verre en croisant la légende et les cellules.
 */
object PdfGridMarkers {
    private val ABBR = mapOf(
        DayOfWeek.MONDAY to "l",
        DayOfWeek.TUESDAY to "ma",
        DayOfWeek.WEDNESDAY to "me",
        DayOfWeek.THURSDAY to "j",
        DayOfWeek.FRIDAY to "v",
        DayOfWeek.SATURDAY to "s",
        DayOfWeek.SUNDAY to "d"
    )

    fun letterDayNumbers(text: String, verreDay: DayOfWeek): Pair<Set<Int>, Set<Int>> {
        val abbr = ABBR[verreDay] ?: return emptySet<Int>() to emptySet()
        val normalized = CollectionRulesParser.normalizeSource(text)
        val pattern = Regex("""\b$abbr\s+(\d{1,2})\s+([ab])\b""")
        val groupA = mutableSetOf<Int>()
        val groupB = mutableSetOf<Int>()
        pattern.findAll(normalized).forEach { match ->
            val day = match.groupValues[1].toIntOrNull() ?: return@forEach
            if (day !in 1..31) return@forEach
            if (match.groupValues[2] == "a") groupA += day else groupB += day
        }
        return groupA to groupB
    }

    fun letterCount(text: String, verreDay: DayOfWeek): Int {
        val (groupA, groupB) = letterDayNumbers(text, verreDay)
        return groupA.size + groupB.size
    }

    fun pickVerreAnchor(
        pdfText: String?,
        communeName: String,
        year: Int,
        verreDay: DayOfWeek,
        emballagesDay: DayOfWeek,
        emballagesAnchor: LocalDate
    ): LocalDate {
        val fallback = defaultAnchor(
            pdfText.orEmpty(),
            communeName,
            year,
            verreDay,
            emballagesDay,
            emballagesAnchor
        )
        if (pdfText.isNullOrBlank()) return fallback

        val (daysA, daysB) = letterDayNumbers(pdfText, verreDay)
        if (daysA.isEmpty() && daysB.isEmpty()) return fallback

        val preferredLetter = preferredLetter(pdfText, communeName)
        val scored = verreAnchorCandidates(year, verreDay, emballagesDay, emballagesAnchor)
            .flatMap { anchor ->
                val dates = fourWeeklyDates(anchor, year)
                listOf(
                    ScoredAnchor(anchor, 'a', overlap(dates, daysA)),
                    ScoredAnchor(anchor, 'b', overlap(dates, daysB))
                )
            }
        val best = scored.maxByOrNull { it.score } ?: return fallback
        if (best.score < 3) return fallback

        val preferred = scored
            .filter { it.letter == preferredLetter }
            .maxByOrNull { it.score }
        return if (preferred != null && preferred.score >= best.score - 2) {
            preferred.anchor
        } else {
            best.anchor
        }
    }

    internal fun preferredLetter(text: String, communeName: String): Char {
        val name = CalendarPdfMatcher.normalize(communeName)
        if (name.contains("magny")) return 'b'
        if (name.contains("themericourt") || name.contains("avernes")) return 'b'

        val listed = Regex(
            """pour la commune de\s+(.+?)\s*\(\s*(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)\s*\)"""
        )
            .findAll(CollectionRulesParser.normalizeSource(text))
            .map { CollectionRulesParser.normalizeSource(it.groupValues[1]) }
            .distinct()
            .toList()
        val index = listed.indexOfFirst { it.contains(name) || name.contains(it) }
        return if (index >= 1) 'b' else 'a'
    }

    private fun defaultAnchor(
        pdfText: String,
        communeName: String,
        year: Int,
        verreDay: DayOfWeek,
        emballagesDay: DayOfWeek,
        emballagesAnchor: LocalDate
    ): LocalDate {
        return if (verreDay == emballagesDay) {
            emballagesAnchor.plusDays(
                CollectionRulesParser.verreOffsetDays(pdfText, communeName)
            )
        } else {
            CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, verreDay)
        }
    }

    private fun verreAnchorCandidates(
        year: Int,
        verreDay: DayOfWeek,
        emballagesDay: DayOfWeek,
        emballagesAnchor: LocalDate
    ): List<LocalDate> {
        if (verreDay == emballagesDay) {
            return listOf(
                emballagesAnchor.plusDays(7),
                emballagesAnchor.plusDays(21)
            )
        }
        val first = CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, verreDay)
        return listOf(first, first.plusDays(7), first.plusDays(14), first.plusDays(21))
    }

    private fun fourWeeklyDates(anchor: LocalDate, year: Int): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var date = anchor
        val end = LocalDate.of(year, 12, 31)
        while (!date.isAfter(end)) {
            if (date.year == year) dates += date
            date = date.plusDays(28)
        }
        return dates
    }

    private fun overlap(dates: List<LocalDate>, letterDays: Set<Int>): Int {
        if (letterDays.isEmpty()) return 0
        return dates.count { it.dayOfMonth in letterDays }
    }

    private data class ScoredAnchor(
        val anchor: LocalDate,
        val letter: Char,
        val score: Int
    )
}

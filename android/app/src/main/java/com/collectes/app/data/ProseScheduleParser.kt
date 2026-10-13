package com.collectes.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Locale

/**
 * Extrait un rythme de collecte depuis un texte libre (PDF mémo, page mairie / EPCI).
 * Couvre les formulations courantes : « lundi et jeudi », « chaque vendredi »,
 * « 2ᵉ et 4ᵉ vendredi du mois », etc.
 */
object ProseScheduleParser {
    private val DAY_WORDS = mapOf(
        "lundi" to DayOfWeek.MONDAY,
        "mardi" to DayOfWeek.TUESDAY,
        "mercredi" to DayOfWeek.WEDNESDAY,
        "jeudi" to DayOfWeek.THURSDAY,
        "vendredi" to DayOfWeek.FRIDAY,
        "samedi" to DayOfWeek.SATURDAY,
        "dimanche" to DayOfWeek.SUNDAY
    )
    private val DAY_PATTERN = Regex(
        """\b(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)\b""",
        RegexOption.IGNORE_CASE
    )
    private val NTH_PATTERN = Regex(
        """(\d{1,2})(?:er|e|ème|eme)?(?:\s*(?:,|et)\s*(\d{1,2})(?:er|e|ème|eme)?(?:\s*(?:,|et)\s*(\d{1,2})(?:er|e|ème|eme)?)?)?\s+(lundi|mardi|mercredi|jeudi|vendredi|samedi|dimanche)""",
        RegexOption.IGNORE_CASE
    )

    fun parseIfPresent(text: String, year: Int, communeName: String? = null): CollectionRules? {
        val normalized = CollectionRulesParser.normalizeSource(text)
        val window = communeWindow(normalized, communeName)
        if (!hasEnoughKeywords(window)) return null

        val orduresDays = weekdaysNear(window, listOf("ordures menageres", "ordures ménagères", "ordures"))
        val emballagesDays = weekdaysNear(
            window,
            listOf("emballages et papiers", "emballages papiers", "emballages", "papiers cartons", "bac jaune")
        )
        if (orduresDays.isEmpty() || emballagesDays.isEmpty()) return null

        val orduresDay = orduresDays.first()
        val orduresExtra = orduresDays.drop(1).toSet()
        val emballagesDay = emballagesDays.first()

        val verreNth = monthlyNear(window, listOf("verre", "bac vert"))
        val verreDays = weekdaysNear(window, listOf("verre", "bac vert"))
        val verreDay = verreNth?.first ?: verreDays.firstOrNull() ?: emballagesDay
        val verreOrdinals = verreNth?.second.orEmpty()

        val emballagesAnchor = CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, emballagesDay)
        return CollectionRules(
            orduresDay = orduresDay,
            emballagesDay = emballagesDay,
            emballagesAnchor = emballagesAnchor,
            verreDay = verreDay,
            verreAnchor = emballagesAnchor,
            orduresRecurrence = CollectionRecurrence.WEEKLY,
            emballagesRecurrence = CollectionRecurrence.WEEKLY,
            verreRecurrence = if (verreOrdinals.isNotEmpty()) {
                CollectionRecurrence.MONTHLY_NTH_WEEKDAY
            } else {
                CollectionRecurrence.EVERY_FOUR_WEEKS
            },
            verreMonthOrdinal = verreOrdinals.minOrNull(),
            verreMonthOrdinals = verreOrdinals,
            orduresExtraDays = orduresExtra
        )
    }

    private fun hasEnoughKeywords(text: String): Boolean {
        val hits = listOf("ordures", "emballages", "verre", "collecte", "papiers").count { text.contains(it) }
        return hits >= 2
    }

    private fun communeWindow(text: String, communeName: String?): String {
        if (communeName.isNullOrBlank()) return text
        val target = CollectionRulesParser.normalizeSource(communeName)
        val index = text.indexOf(target)
        if (index < 0) return text
        val start = (index - 80).coerceAtLeast(0)
        val end = (index + 900).coerceAtMost(text.length)
        return text.substring(start, end)
    }

    private fun weekdaysNear(text: String, keywords: List<String>): List<DayOfWeek> {
        for (keyword in keywords) {
            var from = 0
            while (from < text.length) {
                val index = text.indexOf(keyword, from)
                if (index < 0) break
                val rawEnd = (index + 180).coerceAtMost(text.length)
                // Couper avant la section suivante (autre flux).
                val cut = listOf("ordures", "emballages", "verre", "encombrants", "vegetaux", "végétaux")
                    .mapNotNull { marker ->
                        val at = text.indexOf(marker, index + keyword.length)
                        at.takeIf { it in (index + keyword.length) until rawEnd }
                    }
                    .minOrNull()
                val end = cut ?: rawEnd
                val window = text.substring(index, end)
                val days = DAY_PATTERN.findAll(window)
                    .mapNotNull { DAY_WORDS[it.groupValues[1].lowercase(Locale.FRENCH)] }
                    .distinct()
                    .toList()
                if (days.isNotEmpty()) return days
                from = index + keyword.length
            }
        }
        return emptyList()
    }

    private fun monthlyNear(text: String, keywords: List<String>): Pair<DayOfWeek, Set<Int>>? {
        for (keyword in keywords) {
            var from = 0
            while (from < text.length) {
                val index = text.indexOf(keyword, from)
                if (index < 0) break
                val window = text.substring(
                    (index - 40).coerceAtLeast(0),
                    (index + 220).coerceAtMost(text.length)
                )
                NTH_PATTERN.find(window)?.let { match ->
                    val day = DAY_WORDS[match.groupValues[4].lowercase(Locale.FRENCH)] ?: return@let
                    val ordinals = listOfNotNull(
                        match.groupValues[1].toIntOrNull(),
                        match.groupValues[2].toIntOrNull()?.takeIf { it > 0 },
                        match.groupValues[3].toIntOrNull()?.takeIf { it > 0 }
                    ).filter { it in 1..5 }.toSet()
                    if (ordinals.isNotEmpty()) return day to ordinals
                }
                from = index + keyword.length
            }
        }
        return null
    }
}

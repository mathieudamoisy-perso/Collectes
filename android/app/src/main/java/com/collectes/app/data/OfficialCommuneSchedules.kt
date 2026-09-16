package com.collectes.app.data

import java.time.DayOfWeek

/**
 * Jours de collecte 2026 relevés sur les pages SMIRTOM
 * `informations_utiles/{slug}/`, croisés avec les PDF groupés
 * (actualités « Calendriers de collecte 2026 »).
 *
 * Dernier recours si PDF et page commune sont injoignables.
 * Cabourg : règles NCPA / mairie (PDF graphique non parsable comme Emeraude).
 * Blaincourt-lès-Précy : règles Thelloise (tableau de jours, pas une grille).
 */
object OfficialCommuneSchedules {
    data class Weekdays(
        val orduresDay: DayOfWeek,
        val emballagesDay: DayOfWeek,
        val verreDay: DayOfWeek,
        val verreGroupB: Boolean = false
    )

    private val weekdays: Map<String, Weekdays> = mapOf(
        "magny-en-vexin" to Weekdays(
            DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.TUESDAY, verreGroupB = true
        ),
        "charmont" to Weekdays(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.TUESDAY),
        "ambleville" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "la-chapelle-en-vexin" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "omerville" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "saint-gervais" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "clery-en-vexin" to Weekdays(DayOfWeek.THURSDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "hodent" to Weekdays(DayOfWeek.THURSDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "nucourt" to Weekdays(DayOfWeek.THURSDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "genainville" to Weekdays(DayOfWeek.THURSDAY, DayOfWeek.MONDAY, DayOfWeek.MONDAY),
        "arthies" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.THURSDAY, DayOfWeek.MONDAY),
        "banthelu" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.THURSDAY, DayOfWeek.MONDAY),
        "maudetour-en-vexin" to Weekdays(DayOfWeek.FRIDAY, DayOfWeek.THURSDAY, DayOfWeek.MONDAY),
        "wy-dit-joli-village" to Weekdays(DayOfWeek.WEDNESDAY, DayOfWeek.TUESDAY, DayOfWeek.TUESDAY),
        "themericourt" to Weekdays(DayOfWeek.WEDNESDAY, DayOfWeek.TUESDAY, DayOfWeek.THURSDAY),
        "cormeilles-en-vexin" to Weekdays(DayOfWeek.THURSDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
        "epiais-rhus" to Weekdays(DayOfWeek.THURSDAY, DayOfWeek.MONDAY, DayOfWeek.TUESDAY),
        "sannois" to Weekdays(DayOfWeek.THURSDAY, DayOfWeek.TUESDAY, DayOfWeek.MONDAY),
        "ermont" to Weekdays(DayOfWeek.TUESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
    )

    fun weekdaysFor(slug: String): Weekdays? = weekdays[VexinCommunes.normalizeSlug(slug)]

    fun rules(year: Int, communeSlug: String): CollectionRules {
        when (VexinCommunes.normalizeSlug(communeSlug)) {
            "cabourg" -> return cabourgRules(year)
            "blaincourt-les-precy" -> return blaincourtRules(year)
            "sannois" -> return municipalFallback(
                year = year,
                orduresDay = DayOfWeek.THURSDAY,
                emballagesDay = DayOfWeek.TUESDAY,
                verreDay = DayOfWeek.MONDAY,
                verreOrdinal = 2,
                encombrantsDay = DayOfWeek.WEDNESDAY,
                encombrantsOrdinal = 1,
                emballagesRecurrence = CollectionRecurrence.WEEKLY
            )
            "ermont" -> return municipalFallback(
                year = year,
                orduresDay = DayOfWeek.TUESDAY,
                emballagesDay = DayOfWeek.THURSDAY,
                verreDay = DayOfWeek.FRIDAY,
                verreOrdinal = 4,
                encombrantsDay = DayOfWeek.WEDNESDAY,
                encombrantsOrdinal = 2,
                emballagesRecurrence = CollectionRecurrence.WEEKLY,
                vegetauxSchedule = VegetauxSchedule(
                    dayOfWeek = DayOfWeek.MONDAY,
                    activeRanges = listOf(
                        MonthDayRange(MonthDay(1, 1), MonthDay(12, 31))
                    )
                )
            )
        }

        val days = weekdays[VexinCommunes.normalizeSlug(communeSlug)] ?: weekdays.getValue("magny-en-vexin")
        val emballagesAnchor = CalendarDateGenerator.firstDayOfWeekOnOrAfter(
            year,
            1,
            days.emballagesDay
        )
        val verreAnchor = if (days.emballagesDay == days.verreDay) {
            emballagesAnchor.plusDays(if (days.verreGroupB) 21L else 7L)
        } else {
            CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, days.verreDay)
        }
        return CollectionRules(
            orduresDay = days.orduresDay,
            emballagesDay = days.emballagesDay,
            emballagesAnchor = emballagesAnchor,
            verreDay = days.verreDay,
            verreAnchor = verreAnchor
        )
    }

    /** Source : calendrier NCPA 2026 + page déchets cabourg.fr. */
    private fun cabourgRules(year: Int): CollectionRules {
        val emballagesAnchor = CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, DayOfWeek.MONDAY)
        return CollectionRules(
            orduresDay = DayOfWeek.MONDAY,
            emballagesDay = DayOfWeek.MONDAY,
            emballagesAnchor = emballagesAnchor,
            verreDay = DayOfWeek.MONDAY,
            verreAnchor = emballagesAnchor,
            orduresRecurrence = CollectionRecurrence.WEEKLY,
            emballagesRecurrence = CollectionRecurrence.WEEKLY,
            // Pas de porte-à-porte verre (apport volontaire uniquement).
            verreRecurrence = CollectionRecurrence.EVERY_FOUR_WEEKS,
            orduresExtraDays = setOf(DayOfWeek.FRIDAY),
            orduresSeasonalExtraDays = listOf(
                SeasonalWeekday(
                    dayOfWeek = DayOfWeek.WEDNESDAY,
                    activeRanges = listOf(MonthDayRange(MonthDay(7, 1), MonthDay(8, 31)))
                )
            ),
            vegetauxSchedule = VegetauxSchedule(
                dayOfWeek = DayOfWeek.THURSDAY,
                activeRanges = listOf(MonthDayRange(MonthDay(3, 19), MonthDay(11, 12)))
            ),
            excludedDates = listOf(MonthDay(1, 1), MonthDay(12, 25))
        )
    }

    /** Source : agenda Thelloise 2026 (PDF) + mairie Blaincourt-lès-Précy. */
    private fun blaincourtRules(year: Int): CollectionRules {
        val emballagesAnchor = CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, DayOfWeek.THURSDAY)
        return CollectionRules(
            orduresDay = DayOfWeek.THURSDAY,
            emballagesDay = DayOfWeek.THURSDAY,
            emballagesAnchor = emballagesAnchor,
            verreDay = DayOfWeek.THURSDAY,
            verreAnchor = emballagesAnchor,
            orduresRecurrence = CollectionRecurrence.WEEKLY,
            emballagesRecurrence = CollectionRecurrence.WEEKLY,
            // Pas de porte-à-porte verre (borne d’apport volontaire).
            verreRecurrence = CollectionRecurrence.EVERY_FOUR_WEEKS,
            vegetauxSchedule = VegetauxSchedule(
                dayOfWeek = DayOfWeek.THURSDAY,
                activeRanges = listOf(MonthDayRange(MonthDay(3, 30), MonthDay(11, 27)))
            ),
            dateSubstitutions = listOf(
                DateSubstitution(from = MonthDay(1, 1), to = MonthDay(1, 3))
            )
        )
    }

    private fun municipalFallback(
        year: Int,
        orduresDay: DayOfWeek,
        emballagesDay: DayOfWeek,
        verreDay: DayOfWeek,
        verreOrdinal: Int,
        encombrantsDay: DayOfWeek? = null,
        encombrantsOrdinal: Int? = null,
        emballagesRecurrence: CollectionRecurrence = CollectionRecurrence.BIWEEKLY,
        vegetauxSchedule: VegetauxSchedule? = null
    ): CollectionRules {
        val emballagesAnchor = CalendarDateGenerator.firstDayOfWeekOnOrAfter(year, 1, emballagesDay)
        return CollectionRules(
            orduresDay = orduresDay,
            emballagesDay = emballagesDay,
            emballagesAnchor = emballagesAnchor,
            verreDay = verreDay,
            verreAnchor = emballagesAnchor,
            orduresRecurrence = CollectionRecurrence.WEEKLY,
            emballagesRecurrence = emballagesRecurrence,
            verreRecurrence = CollectionRecurrence.MONTHLY_NTH_WEEKDAY,
            verreMonthOrdinal = verreOrdinal,
            encombrantsDay = encombrantsDay,
            encombrantsMonthOrdinal = encombrantsOrdinal,
            vegetauxSchedule = vegetauxSchedule
        )
    }
}

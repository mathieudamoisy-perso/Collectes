package com.smirtom.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters

object CalendarDateGenerator {
    fun generate(
        year: Int,
        rules: CollectionRules,
        includeNextYearJanuary: Boolean = true
    ): List<CollectionDay> {
        return if (usesSmirtomStyleGeneration(rules)) {
            generateSmirtomStyle(year, rules, includeNextYearJanuary)
        } else {
            generateExplicitSchedules(year, rules, includeNextYearJanuary)
        }
    }

    private fun usesSmirtomStyleGeneration(rules: CollectionRules): Boolean {
        return rules.orduresRecurrence == CollectionRecurrence.WEEKLY &&
            rules.emballagesRecurrence == CollectionRecurrence.BIWEEKLY &&
            rules.verreRecurrence == CollectionRecurrence.EVERY_FOUR_WEEKS &&
            rules.encombrantsMonthOrdinal == null
    }

    private fun generateSmirtomStyle(
        year: Int,
        rules: CollectionRules,
        includeNextYearJanuary: Boolean
    ): List<CollectionDay> {
        val start = LocalDate.of(year, 1, 1)
        val end = if (includeNextYearJanuary) {
            LocalDate.of(year + 1, 1, 31)
        } else {
            LocalDate.of(year, 12, 31)
        }

        val events = linkedMapOf<LocalDate, MutableSet<WasteType>>()
        var date = start
        while (!date.isAfter(end)) {
            val hasEmballages = date.dayOfWeek == rules.emballagesDay &&
                isBiweekly(date, rules.emballagesAnchor)
            val hasVerre = date.dayOfWeek == rules.verreDay &&
                isEveryFourWeeks(date, rules.verreAnchor)
            val recycling = resolveRecyclingType(rules, hasEmballages, hasVerre)

            when {
                recycling != null -> events.getOrPut(date) { mutableSetOf() }.add(recycling)
                date.dayOfWeek == rules.orduresDay -> {
                    events.getOrPut(date) { mutableSetOf() }.add(WasteType.ORDURES)
                }
            }
            date = date.plusDays(1)
        }

        return events.map { (eventDate, types) ->
            CollectionDay(eventDate, types.sortedBy { it.ordinal })
        }.sortedBy { it.date }
    }

    private fun generateExplicitSchedules(
        year: Int,
        rules: CollectionRules,
        includeNextYearJanuary: Boolean
    ): List<CollectionDay> {
        val days = mutableListOf<CollectionDay>()
        orduresDates(year, rules, includeNextYearJanuary).forEach { date ->
            days += CollectionDay(date, listOf(WasteType.ORDURES))
        }
        emballagesDates(year, rules, includeNextYearJanuary).forEach { date ->
            days += CollectionDay(date, listOf(WasteType.EMBALLAGES))
        }
        verreDates(year, rules, includeNextYearJanuary).forEach { date ->
            days += CollectionDay(date, listOf(WasteType.VERRE))
        }
        encombrantsDates(year, rules, includeNextYearJanuary).forEach { date ->
            days += CollectionDay(date, listOf(WasteType.ENCOMBRANTS))
        }
        vegetauxDates(year, rules, includeNextYearJanuary).forEach { date ->
            days += CollectionDay(date, listOf(WasteType.VEGETAUX))
        }
        return CollectionDayMerger.merge(days)
    }

    private fun orduresDates(
        year: Int,
        rules: CollectionRules,
        includeNextYearJanuary: Boolean
    ): List<LocalDate> {
        return when (rules.orduresRecurrence) {
            CollectionRecurrence.WEEKLY -> weeklyDates(year, rules.orduresDay, includeNextYearJanuary)
            else -> emptyList()
        }
    }

    private fun emballagesDates(
        year: Int,
        rules: CollectionRules,
        includeNextYearJanuary: Boolean
    ): List<LocalDate> {
        return when (rules.emballagesRecurrence) {
            CollectionRecurrence.WEEKLY -> weeklyDates(
                year,
                rules.emballagesDay,
                includeNextYearJanuary
            )
            CollectionRecurrence.BIWEEKLY -> biweeklyDates(
                year,
                rules.emballagesDay,
                rules.emballagesAnchor,
                includeNextYearJanuary
            )
            else -> emptyList()
        }
    }

    private fun verreDates(
        year: Int,
        rules: CollectionRules,
        includeNextYearJanuary: Boolean
    ): List<LocalDate> {
        return when (rules.verreRecurrence) {
            CollectionRecurrence.EVERY_FOUR_WEEKS -> emptyList()
            CollectionRecurrence.MONTHLY_NTH_WEEKDAY -> {
                val ordinal = rules.verreMonthOrdinal ?: return emptyList()
                monthlyNthWeekdays(year, rules.verreDay, ordinal, includeNextYearJanuary)
            }
            else -> emptyList()
        }
    }

    fun encombrantsDates(
        year: Int,
        rules: CollectionRules,
        includeNextYearJanuary: Boolean
    ): List<LocalDate> {
        val day = rules.encombrantsDay ?: return emptyList()
        val ordinal = rules.encombrantsMonthOrdinal ?: return emptyList()
        return monthlyNthWeekdays(year, day, ordinal, includeNextYearJanuary)
    }

    fun vegetauxDates(
        year: Int,
        rules: CollectionRules,
        includeNextYearJanuary: Boolean
    ): List<LocalDate> {
        val schedule = rules.vegetauxSchedule ?: return emptyList()
        return datesInRange(year, includeNextYearJanuary) { schedule.includes(it) }
    }

    private fun weeklyDates(
        year: Int,
        dayOfWeek: DayOfWeek,
        includeNextYearJanuary: Boolean
    ): List<LocalDate> {
        return datesInRange(year, includeNextYearJanuary) { date ->
            date.dayOfWeek == dayOfWeek
        }
    }

    private fun biweeklyDates(
        year: Int,
        dayOfWeek: DayOfWeek,
        anchor: LocalDate,
        includeNextYearJanuary: Boolean
    ): List<LocalDate> {
        return datesInRange(year, includeNextYearJanuary) { date ->
            date.dayOfWeek == dayOfWeek && isBiweekly(date, anchor)
        }
    }

    internal fun monthlyNthWeekdays(
        year: Int,
        dayOfWeek: DayOfWeek,
        ordinal: Int,
        includeNextYearJanuary: Boolean
    ): List<LocalDate> {
        val monthCount = if (includeNextYearJanuary) 13 else 12
        return (0 until monthCount).mapNotNull { offset ->
            val month = (offset % 12) + 1
            val calendarYear = year + offset / 12
            nthWeekdayOfMonth(calendarYear, month, dayOfWeek, ordinal)
        }
    }

    internal fun nthWeekdayOfMonth(
        year: Int,
        month: Int,
        dayOfWeek: DayOfWeek,
        ordinal: Int
    ): LocalDate? {
        var count = 0
        var date = LocalDate.of(year, month, 1)
        val end = date.with(TemporalAdjusters.lastDayOfMonth())
        while (!date.isAfter(end)) {
            if (date.dayOfWeek == dayOfWeek) {
                count++
                if (count == ordinal) return date
            }
            date = date.plusDays(1)
        }
        return null
    }

    internal fun firstFourWeeklyOnOrAfter(
        year: Int,
        dayOfWeek: DayOfWeek,
        cycleAnchor: LocalDate
    ): LocalDate {
        var date = LocalDate.of(year, 1, 1)
        val end = LocalDate.of(year, 12, 31)
        while (!date.isAfter(end)) {
            if (date.dayOfWeek == dayOfWeek && isEveryFourWeeks(date, cycleAnchor)) {
                return date
            }
            date = date.plusDays(1)
        }
        return firstDayOfWeekOnOrAfter(year, 1, dayOfWeek)
    }

    private fun datesInRange(
        year: Int,
        includeNextYearJanuary: Boolean,
        predicate: (LocalDate) -> Boolean
    ): List<LocalDate> {
        val start = LocalDate.of(year, 1, 1)
        val end = if (includeNextYearJanuary) {
            LocalDate.of(year + 1, 1, 31)
        } else {
            LocalDate.of(year, 12, 31)
        }
        val dates = mutableListOf<LocalDate>()
        var date = start
        while (!date.isAfter(end)) {
            if (predicate(date)) dates += date
            date = date.plusDays(1)
        }
        return dates
    }

    /** Un seul flux régulier par jour : verre / emballages en alternance, jamais avec les ordures. */
    private fun resolveRecyclingType(
        rules: CollectionRules,
        hasEmballages: Boolean,
        hasVerre: Boolean
    ): WasteType? {
        val candidates = if (rules.emballagesDay == rules.verreDay) {
            resolveAlternatingTuesday(hasEmballages, hasVerre)
        } else {
            buildSet {
                if (hasEmballages) add(WasteType.EMBALLAGES)
                if (hasVerre) add(WasteType.VERRE)
            }
        }
        return when {
            WasteType.VERRE in candidates && WasteType.EMBALLAGES in candidates -> WasteType.EMBALLAGES
            else -> candidates.singleOrNull()
        }
    }

    /** Emballages et verre ne sont jamais collectés le même jour (alternance). */
    internal fun resolveAlternatingTuesday(
        emballages: Boolean,
        verre: Boolean
    ): Set<WasteType> {
        val types = mutableSetOf<WasteType>()
        when {
            emballages && verre -> types += WasteType.EMBALLAGES
            emballages -> types += WasteType.EMBALLAGES
            verre -> types += WasteType.VERRE
        }
        return types
    }

    private fun isBiweekly(date: LocalDate, anchor: LocalDate): Boolean {
        val daysBetween = ChronoUnit.DAYS.between(anchor, date)
        return daysBetween >= 0 && daysBetween % 14L == 0L
    }

    private fun isEveryFourWeeks(date: LocalDate, anchor: LocalDate): Boolean {
        val daysBetween = ChronoUnit.DAYS.between(anchor, date)
        return daysBetween >= 0 && daysBetween % 28L == 0L
    }

    fun firstDayOfWeekOnOrAfter(year: Int, month: Int, dayOfWeek: DayOfWeek): LocalDate {
        val first = LocalDate.of(year, month, 1)
        return if (first.dayOfWeek == dayOfWeek) {
            first
        } else {
            first.with(TemporalAdjusters.next(dayOfWeek))
        }
    }
}

package com.smirtom.app.data

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

object CalendarDateGenerator {
    fun generate(
        year: Int,
        rules: CollectionRules,
        includeNextYearJanuary: Boolean = true
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
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(anchor, date)
        return daysBetween >= 0 && daysBetween % 14L == 0L
    }

    private fun isEveryFourWeeks(date: LocalDate, anchor: LocalDate): Boolean {
        val daysBetween = java.time.temporal.ChronoUnit.DAYS.between(anchor, date)
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

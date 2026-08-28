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
            if (date.dayOfWeek == rules.orduresDay) {
                events.getOrPut(date) { mutableSetOf() }.add(WasteType.ORDURES)
            }
            if (date.dayOfWeek == rules.emballagesDay &&
                isBiweekly(date, rules.emballagesAnchor)
            ) {
                events.getOrPut(date) { mutableSetOf() }.add(WasteType.EMBALLAGES)
            }
            if (date.dayOfWeek == rules.verreDay &&
                isEveryFourWeeks(date, rules.verreAnchor)
            ) {
                events.getOrPut(date) { mutableSetOf() }.add(WasteType.VERRE)
            }
            date = date.plusDays(1)
        }

        return events.map { (eventDate, types) ->
            CollectionDay(eventDate, types.sortedBy { it.ordinal })
        }.sortedBy { it.date }
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

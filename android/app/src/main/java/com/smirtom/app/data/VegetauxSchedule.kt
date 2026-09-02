package com.smirtom.app.data

import java.time.DayOfWeek
import java.time.LocalDate

data class MonthDay(val month: Int, val day: Int) {
    fun toLocalDate(year: Int): LocalDate = LocalDate.of(year, month, day)

    fun isWithin(range: MonthDayRange): Boolean {
        val value = month * 100 + day
        val start = range.start.month * 100 + range.start.day
        val end = range.end.month * 100 + range.end.day
        return value in start..end
    }
}

data class MonthDayRange(val start: MonthDay, val end: MonthDay)

data class VegetauxSchedule(
    val dayOfWeek: DayOfWeek,
    val activeRanges: List<MonthDayRange>,
    val extraDates: List<MonthDay> = emptyList()
) {
    fun includes(date: LocalDate): Boolean {
        if (date.dayOfWeek != dayOfWeek) return false
        val monthDay = MonthDay(date.monthValue, date.dayOfMonth)
        return extraDates.any { it.month == monthDay.month && it.day == monthDay.day } ||
            activeRanges.any { monthDay.isWithin(it) }
    }
}

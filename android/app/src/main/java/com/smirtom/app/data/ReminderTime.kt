package com.smirtom.app.data

object ReminderTime {
    const val STEP_MINUTES = 30
    const val MIN_MINUTES = 6 * 60
    const val MAX_MINUTES = 12 * 60
    const val DEFAULT_MINUTES = 12 * 60

    fun options(): List<Int> = (MIN_MINUTES..MAX_MINUTES step STEP_MINUTES).toList()

    fun coerce(minutesOfDay: Int): Int {
        return options().minByOrNull { kotlin.math.abs(it - minutesOfDay) } ?: DEFAULT_MINUTES
    }

    fun format(minutesOfDay: Int): String {
        val hours = minutesOfDay / 60
        val minutes = minutesOfDay % 60
        return if (minutes == 0) {
            "${hours}h00"
        } else {
            "${hours}h${minutes.toString().padStart(2, '0')}"
        }
    }
}

package com.smirtom.app.data

import android.graphics.Bitmap
import android.graphics.Color
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import java.io.File
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Calendriers CCVT (ex. Bouconvillers) : grille visuelle avec bandes jaunes
 * par colonne (bac jaune + bac gris le même jour) et cadres magenta (encombrants).
 */
object CcvtCalendarParser {
    private const val RENDER_SCALE = 6f

    private data class MonthBlock(
        val month: Int,
        val xMin: Float,
        val xMax: Float,
        val yStart: Float,
        val yEnd: Float
    )

    private val monthBlocks = listOf(
        MonthBlock(1, 12f, 132f, 246f, 330f),
        MonthBlock(2, 148f, 268f, 246f, 330f),
        MonthBlock(3, 284f, 404f, 246f, 330f),
        MonthBlock(4, 12f, 132f, 329f, 413f),
        MonthBlock(5, 148f, 268f, 329f, 413f),
        MonthBlock(6, 284f, 404f, 329f, 413f),
        MonthBlock(7, 12f, 132f, 406f, 490f),
        MonthBlock(8, 148f, 268f, 406f, 490f),
        MonthBlock(9, 284f, 404f, 406f, 490f),
        MonthBlock(10, 12f, 132f, 493f, 577f),
        MonthBlock(11, 148f, 268f, 493f, 577f),
        MonthBlock(12, 284f, 404f, 493f, 577f)
    )

    private const val COLUMN_YELLOW_THRESHOLD = 4000
    private const val BORDER_ENCOMBRANTS_SIDE_THRESHOLD = 15
    private const val BORDER_ENCOMBRANTS_SIDES_REQUIRED = 2
    private const val CELL_VERRE_THRESHOLD = 800
    private const val CELL_VERRE_NEIGHBOR_SOFT = 500

    fun parseIfPresent(pdfFile: File, year: Int): List<CollectionDay>? {
        if (!looksLikeCcvtCalendar(pdfFile)) return null
        return runCatching { parse(pdfFile, year) }.getOrNull()
    }

    fun parse(pdfFile: File, year: Int): List<CollectionDay> {
        PDDocument.load(pdfFile).use { document ->
            if (document.numberOfPages < 1) return emptyList()
            val renderer = PDFRenderer(document)
            val bitmap = renderer.renderImage(0, RENDER_SCALE)
            try {
                return parseBitmap(bitmap, year)
            } finally {
                bitmap.recycle()
            }
        }
    }

    internal fun parseBitmap(bitmap: Bitmap, year: Int): List<CollectionDay> {
        val events = linkedMapOf<LocalDate, MutableSet<WasteType>>()
        val scale = RENDER_SCALE

        monthBlocks.forEach { block ->
            val sx0 = (block.xMin * scale).toInt()
            val sy0 = (block.yStart * scale).toInt()
            val blockW = ((block.xMax - block.xMin) * scale).toInt()
            val blockH = ((block.yEnd - block.yStart) * scale).toInt()
            val colW = blockW / 7
            val rowH = blockH / 6
            val weeks = monthWeeks(year, block.month)
            val weekOffset = firstDisplayWeekOffset(weeks)

            // Une seule colonne de collecte régulière par mois : le fond décoratif
            // du PDF peut contenir assez de jaune pour dépasser un seuil naïf.
            var bestYellowCol = -1
            var bestYellowScore = 0
            for (col in 0 until 7) {
                val x0 = sx0 + col * colW
                val x1 = x0 + colW
                val yellow = columnYellowPixels(bitmap, x0, sy0, x1, sy0 + blockH)
                if (yellow > bestYellowScore) {
                    bestYellowScore = yellow
                    bestYellowCol = col
                }
            }
            val yellowDays = mutableSetOf<LocalDate>()
            if (bestYellowCol >= 0 && bestYellowScore >= COLUMN_YELLOW_THRESHOLD) {
                weekdayDates(year, block.month, gridColumnToDayOfWeek(bestYellowCol)).forEach { date ->
                    yellowDays += date
                    events.getOrPut(date) { mutableSetOf() }.apply {
                        add(WasteType.EMBALLAGES)
                        add(WasteType.ORDURES)
                    }
                }
            }

            val verreScores = Array(6) { IntArray(7) }
            for (row in 0 until 6) {
                val calendarRow = row + weekOffset
                if (calendarRow >= weeks.size) continue
                val week = weeks[calendarRow]
                for (col in 0 until 7) {
                    val mappedDay = week[col]
                    if (mappedDay == 0) continue
                    val x0 = sx0 + col * colW
                    val y0 = sy0 + row * rowH
                    val x1 = x0 + colW
                    val y1 = y0 + rowH
                    val date = LocalDate.of(year, block.month, mappedDay)
                    if (borderHasEncombrants(bitmap, x0, y0, x1, y1)) {
                        events.getOrPut(date) { mutableSetOf() }.add(WasteType.ENCOMBRANTS)
                    }
                    verreScores[row][col] = cellVerrePixels(bitmap, x0, y0, x1, y1)
                }
            }

            for (row in 0 until 6) {
                val calendarRow = row + weekOffset
                if (calendarRow >= weeks.size) continue
                val week = weeks[calendarRow]
                for (col in 0 until 7) {
                    val mappedDay = week[col]
                    if (mappedDay == 0) continue
                    val date = LocalDate.of(year, block.month, mappedDay)
                    // Le verre est un carré vert sur le jour de collecte régulière ;
                    // le fond décoratif peut créer des faux positifs ailleurs.
                    if (date !in yellowDays) continue
                    val score = verreScores[row][col]
                    if (score < CELL_VERRE_THRESHOLD) continue
                    val neighbor = maxNeighborScore(verreScores, row, col)
                    // Carré isolé = pic local ; le fond vert touche plusieurs cases.
                    if (score <= neighbor) continue
                    if (neighbor > CELL_VERRE_NEIGHBOR_SOFT && score < neighbor * 3 / 2) continue
                    events.getOrPut(date) { mutableSetOf() }.add(WasteType.VERRE)
                }
            }
        }

        return events.map { (date, types) ->
            CollectionDay(date, types.sortedBy { it.ordinal })
        }.sortedBy { it.date }
    }

    private fun looksLikeCcvtCalendar(pdfFile: File): Boolean {
        val text = PdfCalendarParser().extractText(pdfFile).lowercase()
        return text.contains("vexin") &&
            text.contains("thelle") &&
            text.contains("bac jaune")
    }

    private fun firstDisplayWeekOffset(weeks: List<IntArray>): Int {
        val firstRowDays = weeks.firstOrNull()?.count { it > 0 } ?: 0
        return if (firstRowDays in 1..4) 1 else 0
    }

    private fun monthWeeks(year: Int, month: Int): List<IntArray> {
        val firstOfMonth = LocalDate.of(year, month, 1)
        var date = firstOfMonth
        while (date.dayOfWeek != DayOfWeek.MONDAY) {
            date = date.minusDays(1)
        }
        val weeks = mutableListOf<IntArray>()
        repeat(6) {
            val week = IntArray(7)
            for (col in 0 until 7) {
                week[col] = if (date.monthValue == month) date.dayOfMonth else 0
                date = date.plusDays(1)
            }
            weeks += week
        }
        return weeks
    }

    private fun gridColumnToDayOfWeek(col: Int): DayOfWeek = when (col) {
        0 -> DayOfWeek.MONDAY
        1 -> DayOfWeek.TUESDAY
        2 -> DayOfWeek.WEDNESDAY
        3 -> DayOfWeek.THURSDAY
        4 -> DayOfWeek.FRIDAY
        5 -> DayOfWeek.SATURDAY
        else -> DayOfWeek.SUNDAY
    }

    private fun weekdayDates(year: Int, month: Int, dayOfWeek: DayOfWeek): List<LocalDate> {
        val dates = mutableListOf<LocalDate>()
        var date = LocalDate.of(year, month, 1)
        val end = date.withDayOfMonth(date.lengthOfMonth())
        while (date.dayOfWeek != dayOfWeek) {
            date = date.plusDays(1)
        }
        while (!date.isAfter(end)) {
            dates += date
            date = date.plusDays(7)
        }
        return dates
    }

    private fun columnYellowPixels(bitmap: Bitmap, x0: Int, y0: Int, x1: Int, y1: Int): Int {
        var count = 0
        val maxX = minOf(x1, bitmap.width)
        val maxY = minOf(y1, bitmap.height)
        for (y in y0 until maxY) {
            for (x in x0 until maxX) {
                if (isYellow(bitmap.getPixel(x, y))) count++
            }
        }
        return count
    }

    private fun borderHasEncombrants(bitmap: Bitmap, x0: Int, y0: Int, x1: Int, y1: Int): Boolean {
        val maxX = minOf(x1, bitmap.width)
        val maxY = minOf(y1, bitmap.height)
        if (x0 >= maxX || y0 >= maxY) return false

        val top = horizontalEdgeMagentaCount(bitmap, x0, maxX, y0 + 1)
        val bottom = horizontalEdgeMagentaCount(bitmap, x0, maxX, maxY - 2)
        val left = verticalEdgeMagentaCount(bitmap, x0 + 1, y0, maxY)
        val right = verticalEdgeMagentaCount(bitmap, maxX - 2, y0, maxY)
        val strongSides = listOf(top, bottom, left, right).count { it >= BORDER_ENCOMBRANTS_SIDE_THRESHOLD }
        return strongSides >= BORDER_ENCOMBRANTS_SIDES_REQUIRED
    }

    private fun maxNeighborScore(scores: Array<IntArray>, row: Int, col: Int): Int {
        var max = 0
        for (dr in -1..1) {
            for (dc in -1..1) {
                if (dr == 0 && dc == 0) continue
                val r = row + dr
                val c = col + dc
                if (r in scores.indices && c in scores[r].indices) {
                    max = maxOf(max, scores[r][c])
                }
            }
        }
        return max
    }

    private fun horizontalEdgeMagentaCount(bitmap: Bitmap, xStart: Int, xEnd: Int, y: Int): Int {
        if (y !in 0 until bitmap.height) return 0
        var count = 0
        for (x in xStart until xEnd step 2) {
            if (x in 0 until bitmap.width && isMagenta(bitmap.getPixel(x, y))) count++
        }
        return count
    }

    private fun verticalEdgeMagentaCount(bitmap: Bitmap, x: Int, yStart: Int, yEnd: Int): Int {
        if (x !in 0 until bitmap.width) return 0
        var count = 0
        for (y in yStart until yEnd step 2) {
            if (y in 0 until bitmap.height && isMagenta(bitmap.getPixel(x, y))) count++
        }
        return count
    }

    private fun cellVerrePixels(bitmap: Bitmap, x0: Int, y0: Int, x1: Int, y1: Int): Int {
        val maxX = minOf(x1, bitmap.width)
        val maxY = minOf(y1, bitmap.height)
        if (x0 >= maxX || y0 >= maxY) return 0
        var count = 0
        // Scanner toute la case : le carré vert peut être en haut (ex. 2 décembre),
        // pas forcément au centre.
        for (y in y0 until maxY) {
            for (x in x0 until maxX) {
                if (isGlassGreen(bitmap.getPixel(x, y))) count++
            }
        }
        return count
    }

    private fun isYellow(pixel: Int): Boolean {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return r >= 240 && g >= 200 && b <= 60
    }

    private fun isMagenta(pixel: Int): Boolean {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return r >= 180 && g <= 130 && b >= 130
    }

    /** Vert légende CCVT « Verre » ≈ (62, 172, 71), pas le fond décoratif. */
    private fun isGlassGreen(pixel: Int): Boolean {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        return kotlin.math.abs(r - 62) <= 25 &&
            kotlin.math.abs(g - 172) <= 25 &&
            kotlin.math.abs(b - 71) <= 25
    }
}

package com.collectes.app.data

import android.graphics.Bitmap
import android.graphics.Color
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.rendering.PDFRenderer
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import java.io.File
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Calendriers dessinés (peu de texte exploitable) : une colonne par mois,
 * une ligne par jour, icônes colorées à gauche du libellé.
 */
object ImageCalendarParser {
    private const val RENDER_SCALE = 2f
    private val MONTHS = listOf(
        "janvier", "fevrier", "mars", "avril", "mai", "juin",
        "juillet", "aout", "septembre", "octobre", "novembre", "decembre"
    )

    fun parseIfPresent(pdfFile: File, year: Int): List<CollectionDay>? {
        val events = runCatching { parse(pdfFile, year) }.getOrNull().orEmpty()
        return events.takeIf { it.size >= 8 }
    }

    fun parse(pdfFile: File, year: Int): List<CollectionDay> {
        PDDocument.load(pdfFile).use { document ->
            val renderer = PDFRenderer(document)
            val events = linkedMapOf<LocalDate, MutableSet<WasteType>>()
            for (pageIndex in 0 until document.numberOfPages.coerceAtMost(2)) {
                val page = document.getPage(pageIndex)
                val width = page.mediaBox.width
                val height = page.mediaBox.height
                val chunks = TextChunkStripper().also { stripper ->
                    stripper.startPage = pageIndex + 1
                    stripper.endPage = pageIndex + 1
                    stripper.getText(document)
                }.chunks
                val bitmap = renderer.renderImage(pageIndex, RENDER_SCALE)
                try {
                    collectPage(bitmap, chunks, width, height, year, events)
                } finally {
                    bitmap.recycle()
                }
            }
            return events.map { (date, types) ->
                CollectionDay(date, types.sortedBy { it.ordinal })
            }.sortedBy { it.date }
        }
    }

    internal fun wasteTypesAt(bitmap: Bitmap, xPx: Int, yPx: Int): Set<WasteType> {
        val types = mutableSetOf<WasteType>()
        val x0 = (xPx - 28).coerceAtLeast(0)
        val x1 = xPx.coerceAtMost(bitmap.width)
        val y0 = (yPx - 8).coerceAtLeast(0)
        val y1 = (yPx + 10).coerceAtMost(bitmap.height)
        var yellow = 0
        var green = 0
        var gray = 0
        for (y in y0 until y1) {
            for (x in x0 until x1) {
                when (classify(bitmap.getPixel(x, y))) {
                    WasteType.EMBALLAGES -> yellow++
                    WasteType.VERRE -> green++
                    WasteType.ORDURES -> gray++
                    else -> Unit
                }
            }
        }
        if (yellow >= 8) types += WasteType.EMBALLAGES
        if (green >= 8) types += WasteType.VERRE
        if (gray >= 12) types += WasteType.ORDURES
        return types
    }

    internal fun classify(pixel: Int): WasteType? {
        val r = Color.red(pixel)
        val g = Color.green(pixel)
        val b = Color.blue(pixel)
        if (r > 190 && g > 140 && b < 90) return WasteType.EMBALLAGES
        if (g > 110 && r < 140 && b < 140 && g > r + 30) return WasteType.VERRE
        if (r < 90 && g < 90 && b < 90 && kotlin.math.abs(r - g) < 25 && kotlin.math.abs(g - b) < 25) {
            return WasteType.ORDURES
        }
        return null
    }

    private fun collectPage(
        bitmap: Bitmap,
        chunks: List<TextChunk>,
        pageWidth: Float,
        pageHeight: Float,
        year: Int,
        events: MutableMap<LocalDate, MutableSet<WasteType>>
    ) {
        val months = chunks.mapNotNull { chunk ->
            val key = CalendarPdfMatcher.normalize(chunk.text)
            val month = MONTHS.indexOfFirst { key == it || key.startsWith(it) }
            if (month < 0) null else month + 1 to chunk
        }.distinctBy { it.first }
        months.forEach { (month, header) ->
            val rows = chunks
                .filter { it.y > header.y + 4 && kotlin.math.abs(it.x - header.x) < 70 }
                .filter { CalendarPdfMatcher.normalize(it.text) !in MONTHS }
                .sortedBy { it.y }
            val days = java.time.YearMonth.of(year, month).lengthOfMonth()
            rows.take(days).forEachIndexed { index, row ->
                val date = LocalDate.of(year, month, index + 1)
                val xPx = (row.x / pageWidth * bitmap.width).roundToInt()
                val yPx = (row.y / pageHeight * bitmap.height).roundToInt()
                wasteTypesAt(bitmap, xPx, yPx).forEach { type ->
                    events.getOrPut(date) { mutableSetOf() }.add(type)
                }
            }
        }
    }

    internal class TextChunkStripper : PDFTextStripper() {
        val chunks = mutableListOf<TextChunk>()

        override fun writeString(text: String?, textPositions: List<TextPosition>?) {
            val value = text?.trim().orEmpty()
            val first = textPositions?.firstOrNull() ?: return
            if (value.isEmpty()) return
            chunks += TextChunk(value, first.xDirAdj, first.yDirAdj)
        }
    }

    internal data class TextChunk(val text: String, val x: Float, val y: Float)
}

package com.smirtom.app.data

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

class PdfCalendarParser {
    fun extractText(pdfFile: File): String {
        PDDocument.load(pdfFile).use { document ->
            val stripper = PDFTextStripper()
            return stripper.getText(document)
        }
    }

    fun parse(pdfFile: File, year: Int, commune: VexinCommune): List<CollectionDay> {
        val text = extractText(pdfFile)
        val rules = CalendarReconciler.reconcile(
            pdfText = text,
            pageText = null,
            commune = commune,
            year = year
        )
        return CalendarDateGenerator.generate(year, rules, includeNextYearJanuary = true)
    }
}

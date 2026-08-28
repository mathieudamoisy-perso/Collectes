package com.smirtom.app.data

import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.File

class PdfCalendarParser {
    fun parse(pdfFile: File, year: Int, commune: VexinCommune): List<CollectionDay> {
        PDDocument.load(pdfFile).use { document ->
            val stripper = PDFTextStripper()
            val text = stripper.getText(document)
            val rules = CollectionRulesParser.parse(text, year, commune.displayName)
            return CalendarDateGenerator.generate(year, rules, includeNextYearJanuary = true)
        }
    }
}

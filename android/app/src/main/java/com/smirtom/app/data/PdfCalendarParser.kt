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
}

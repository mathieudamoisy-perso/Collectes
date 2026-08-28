package com.smirtom.app.data

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/** Affichage du PDF SMIRTOM dans le navigateur, sans fichier local. */
object OfficialCalendarLinks {
    fun onlineViewerUrl(pdfUrl: String): String {
        val encoded = URLEncoder.encode(pdfUrl, StandardCharsets.UTF_8.name())
        return "https://docs.google.com/gview?embedded=true&url=$encoded"
    }
}

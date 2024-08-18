package com.corner.quickjs.bean

import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class Cache {
    var pdfhHtml: String? = null
    var pdfaHtml: String? = null
    var pdfhDoc: Document? = null
    var pdfaDoc: Document? = null

    fun getPdfh(html: String): Document? {
        updatePdfh(html)
        return pdfhDoc
    }

    fun getPdfa(html: String): Document? {
        updatePdfa(html)
        return pdfaDoc
    }

    private fun updatePdfh(html: String) {
        if (html == pdfhHtml) return
        pdfhDoc = Jsoup.parse(html.also { pdfhHtml = it })
    }

    private fun updatePdfa(html: String) {
        if (html == pdfaHtml) return
        pdfaDoc = Jsoup.parse(html.also { pdfaHtml = it })
    }
}

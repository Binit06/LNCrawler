package com.halovoid.lncrawler.data.export

import com.halovoid.lncrawler.domain.models.Novel
import java.io.File

/**
 * Placeholder exporter for PDF format in the Data layer.
 * Planned to use Android's PdfDocument for future implementation.
 */
class PdfExporter {
    /**
     * Placeholder method for PDF export.
     * Currently does not perform any action.
     */
    fun export(novel: Novel, chapters: List<Pair<String, String>>, outputFile: File) {
        // TODO: Implement PDF generation using Android's PdfDocument
        // This is a placeholder for future implementation
    }
}

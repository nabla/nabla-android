package com.nabla.sdk.docscanner.core

import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.docscanner.core.components.DocumentDetector
import com.nabla.sdk.docscanner.core.components.PdfGenerator
import com.nabla.sdk.docscanner.core.models.NormalizedCorners

internal class DocumentScanClient(
    private val pdfGeneratorComponent: PdfGenerator,
    private val documentDetectorComponent: DocumentDetector,
) {

    suspend fun generatePdf(imageUriWithCorners: List<Pair<Uri, NormalizedCorners?>>): Uri {
        return pdfGeneratorComponent.generatePdf(imageUriWithCorners)
    }

    suspend fun detectDocumentCorners(imageUri: Uri): NormalizedCorners? {
        return documentDetectorComponent.detectDocumentCorners(imageUri)
    }
}

package com.nabla.sdk.docscanner.core.components

import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.docscanner.core.models.NormalizedCorners

internal interface PdfGenerator {
    suspend fun generatePdf(imageUriWithCorners: List<Pair<Uri, NormalizedCorners?>>): Uri
}

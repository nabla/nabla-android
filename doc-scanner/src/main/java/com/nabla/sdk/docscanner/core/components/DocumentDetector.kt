package com.nabla.sdk.docscanner.core.components

import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.docscanner.core.models.NormalizedCorners

internal interface DocumentDetector {
    suspend fun detectDocumentCorners(imageUri: Uri): NormalizedCorners?
}

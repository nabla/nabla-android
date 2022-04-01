package com.nabla.sdk.core.domain.boundary

import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.core.domain.entity.Uri

interface FileUploadRepository {
    suspend fun uploadFile(localPath: Uri): Id
}

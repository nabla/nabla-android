package com.nabla.sdk.core.domain.boundary

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.Uri

interface FileUploadRepository {
    suspend fun uploadFile(localPath: Uri): Uuid
}

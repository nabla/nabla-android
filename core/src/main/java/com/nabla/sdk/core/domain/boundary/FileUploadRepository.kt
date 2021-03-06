package com.nabla.sdk.core.domain.boundary

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Uri

@NablaInternal
public interface FileUploadRepository {
    public suspend fun uploadFile(localPath: Uri, fileName: String?, mimeType: MimeType): Uuid
}

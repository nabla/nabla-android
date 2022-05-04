package com.nabla.sdk.core.domain.entity

import com.benasher44.uuid.Uuid

public data class Attachment(
    val id: Uuid,
    val url: Uri,
    val mimeType: MimeType,
    val thumbnailUrl: Uri,
)

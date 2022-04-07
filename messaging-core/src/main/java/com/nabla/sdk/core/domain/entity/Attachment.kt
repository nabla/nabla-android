package com.nabla.sdk.core.domain.entity

import com.benasher44.uuid.Uuid

data class Attachment(
    val id: Uuid,
    val url: Uri,
    val mimeType: MimeType,
    val thumbnailUrl: Uri,
)

sealed class MimeType {
    abstract val value: String

    data class Generic(override val value: String) : MimeType()
}

package com.nabla.sdk.core.domain.entity

data class Attachment(
    val id: Id,
    val url: Uri,
    val mimeType: MimeType,
    val thumbnailUrl: Uri,
)

sealed class MimeType {
    abstract val value: String

    data class Generic(override val value: String) : MimeType()
}

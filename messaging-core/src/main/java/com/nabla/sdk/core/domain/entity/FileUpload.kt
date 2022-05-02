package com.nabla.sdk.core.domain.entity

import com.benasher44.uuid.Uuid

/**
 * Url and metadata for a distant server-hosted file.
 */
sealed class FileUpload {
    abstract val fileUpload: BaseFileUpload

    data class Image(
        val size: Size?,
        override val fileUpload: BaseFileUpload,
    ) : FileUpload()

    data class Document(
        val thumbnail: Image?,
        override val fileUpload: BaseFileUpload,
    ) : FileUpload()
}

data class Size(
    val width: Int,
    val height: Int
)

data class BaseFileUpload(
    val id: Uuid,
    val url: EphemeralUrl,
    val fileName: String,
    val mimeType: MimeType,
)

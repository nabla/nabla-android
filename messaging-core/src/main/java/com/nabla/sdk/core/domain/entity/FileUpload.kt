package com.nabla.sdk.core.domain.entity

sealed class FileUpload {
    abstract val fileUpload: BaseFileUpload
    data class Image(
        val width: Int,
        val height: Int,
        override val fileUpload: BaseFileUpload,
    ) : FileUpload()
    data class Document(
        val thumbnail: Image?,
        override val fileUpload: BaseFileUpload,
    ) : FileUpload()
}

data class BaseFileUpload(
    val id: Id,
    val url: EphemeralUrl,
    val fileName: String,
    val mimeType: MimeType,
)

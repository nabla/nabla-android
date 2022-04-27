package com.nabla.sdk.core.domain.entity

sealed interface MimeType {
    val stringRepresentation: String

    enum class Image(override val stringRepresentation: String) : MimeType {
        JPEG("image/jpeg"),
        PNG("image/png"),
    }

    enum class Application(override val stringRepresentation: String) : MimeType {
        PDF("application/pdf"),
    }

    companion object {
        @Throws(IllegalArgumentException::class)
        fun fromStringRepresentation(representation: String): MimeType = when (representation) {
            Image.JPEG.stringRepresentation -> Image.JPEG
            Image.PNG.stringRepresentation -> Image.PNG
            Application.PDF.stringRepresentation -> Application.PDF
            else -> throw NablaException.Internal(IllegalStateException("Unhandled mimeType: $representation"))
        }
    }
}

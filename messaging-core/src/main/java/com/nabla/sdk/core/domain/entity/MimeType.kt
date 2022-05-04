package com.nabla.sdk.core.domain.entity

public sealed interface MimeType {
    public val stringRepresentation: String

    public enum class Image(override val stringRepresentation: String) : MimeType {
        JPEG("image/jpeg"),
        PNG("image/png"),
    }

    public enum class Application(override val stringRepresentation: String) : MimeType {
        PDF("application/pdf"),
    }

    public companion object {
        @Throws(IllegalArgumentException::class)
        public fun fromStringRepresentation(representation: String): MimeType = when (representation) {
            Image.JPEG.stringRepresentation -> Image.JPEG
            Image.PNG.stringRepresentation -> Image.PNG
            Application.PDF.stringRepresentation -> Application.PDF
            else -> throw NablaException.Internal(IllegalStateException("Unhandled mimeType: $representation"))
        }
    }
}

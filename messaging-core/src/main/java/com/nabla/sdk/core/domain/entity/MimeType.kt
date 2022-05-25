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

    public enum class Audio(override val stringRepresentation: String) : MimeType {
        MP3("audio/mpeg"),
    }

    public companion object {
        @Throws(IllegalArgumentException::class)
        public fun fromStringRepresentation(representation: String): MimeType = when (representation) {
            Image.JPEG.stringRepresentation -> Image.JPEG
            Image.PNG.stringRepresentation -> Image.PNG
            Application.PDF.stringRepresentation -> Application.PDF
            Audio.MP3.stringRepresentation -> Audio.MP3
            else -> throw NablaException.Internal(IllegalStateException("Unhandled mimeType: $representation"))
        }
    }
}

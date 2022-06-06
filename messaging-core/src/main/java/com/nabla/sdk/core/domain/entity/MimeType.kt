package com.nabla.sdk.core.domain.entity

public sealed class MimeType(public val type: String) {
    public abstract val subtype: String

    public val stringRepresentation: String
        get() = "$type/$subtype"

    public sealed class Image(public override val subtype: String) : MimeType(TYPE) {
        public object Jpeg : Image("jpeg")
        public object Png : Image("png")
        public data class Other(public override val subtype: String) : Image(subtype)

        internal companion object {
            internal const val TYPE = "image"

            fun fromSubtype(subtype: String) = when (subtype) {
                Jpeg.subtype -> Jpeg
                Png.subtype -> Png
                else -> Other(subtype)
            }
        }
    }

    public sealed class Application(public override val subtype: String) : MimeType(TYPE) {
        public object Pdf : Application("pdf")
        public data class Other(public override val subtype: String) : Application(subtype)

        internal companion object {
            internal const val TYPE = "application"

            fun fromSubtype(subtype: String) = when (subtype) {
                Pdf.subtype -> Pdf
                else -> Other(subtype)
            }
        }
    }

    public sealed class Audio(public override val subtype: String) : MimeType(TYPE) {
        public object Mp3 : Audio("mp3")
        public data class Other(public override val subtype: String) : Audio(subtype)

        internal companion object {
            internal const val TYPE = "audio"

            fun fromSubtype(subtype: String) = when (subtype) {
                Mp3.subtype -> Mp3
                else -> Other(subtype)
            }
        }
    }

    public companion object {
        @Throws(IllegalArgumentException::class)
        public fun fromStringRepresentation(representation: String): MimeType {
            return try {
                val (type, subtype) = representation.split("/")
                when (type) {
                    Image.TYPE -> Image.fromSubtype(subtype)
                    Application.TYPE -> Application.fromSubtype(subtype)
                    Audio.TYPE -> Audio.fromSubtype(subtype)
                    else -> throw IllegalStateException("Unhandled mimeType: $representation")
                }
            } catch (exception: Exception) {
                throw NablaException.Internal(exception)
            }
        }
    }
}

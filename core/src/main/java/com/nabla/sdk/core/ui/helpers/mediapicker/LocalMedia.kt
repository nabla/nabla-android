package com.nabla.sdk.core.ui.helpers.mediapicker

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.InternalException.Companion.throwNablaInternalException
import com.nabla.sdk.core.domain.entity.MimeType
import java.net.URI

@NablaInternal
public sealed class LocalMedia {
    public abstract val uri: URI
    public abstract val mimeType: MimeType
    public abstract val name: String?

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int

    public data class Image(override val uri: URI, override val name: String?, override val mimeType: MimeType.Image) : LocalMedia()
    public data class Video(
        override val uri: URI,
        override val name: String?,
        override val mimeType: MimeType.Video,
    ) : LocalMedia()

    public data class Document(override val uri: URI, override val name: String?, override val mimeType: MimeType) : LocalMedia()

    public companion object {
        @Throws(IllegalArgumentException::class)
        internal fun create(uri: URI, mimeTypeRepresentation: String, name: String?): LocalMedia {
            return when (val mimeType = MimeType.fromStringRepresentation(mimeTypeRepresentation)) {
                is MimeType.Image -> Image(uri, name, mimeType)
                is MimeType.Video -> Video(uri, name, mimeType)
                is MimeType.Application -> Document(uri, name, mimeType)
                else -> throwNablaInternalException("Unhandled mimeType: $mimeTypeRepresentation")
            }
        }
    }
}

package com.nabla.sdk.core.ui.helpers.mediapicker

import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.NablaException
import java.net.URI

internal sealed class LocalMedia {
    abstract val uri: URI
    abstract val mimeType: MimeType
    abstract val name: String?

    abstract override fun equals(other: Any?): Boolean
    abstract override fun hashCode(): Int

    data class Image(override val uri: URI, override val name: String?, override val mimeType: MimeType.Image) : LocalMedia()
    data class Document(override val uri: URI, override val name: String?, override val mimeType: MimeType) : LocalMedia()

    companion object {
        @Throws(IllegalArgumentException::class)
        fun create(uri: URI, mimeTypeRepresentation: String, name: String?): LocalMedia {
            return when (val mimeType = MimeType.fromStringRepresentation(mimeTypeRepresentation)) {
                is MimeType.Image -> Image(uri, name, mimeType)
                is MimeType.Application -> Document(uri, name, mimeType)
                else -> throw NablaException.Internal(IllegalStateException("Unhandled mimeType: $mimeTypeRepresentation"))
            }
        }
    }
}

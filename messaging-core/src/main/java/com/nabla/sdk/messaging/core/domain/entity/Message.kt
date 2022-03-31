package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.core.domain.entity.Uri
import kotlinx.datetime.Instant

sealed interface Message {
    val localId: Id?
    val remoteId: Id?
    val sentAt: Instant
    val sender: MessageSender
    val status: MessageStatus

    sealed interface Media : Message {
        val uri: Uri
        val mimeType: String
        val fileName: String
    }

    data class Text(
        override val localId: Id?,
        override val remoteId: Id?,
        override val sentAt: Instant,
        override val sender: MessageSender,
        override val status: MessageStatus,
        val text: String,
    ) : Message {
        companion object
    }

    data class Image(
        override val localId: Id?,
        override val remoteId: Id?,
        override val sentAt: Instant,
        override val sender: MessageSender,
        override val status: MessageStatus,
        override val uri: Uri,
        override val mimeType: String,
        override val fileName: String,
    ) : Media {
        companion object
    }

    data class File(
        override val localId: Id?,
        override val remoteId: Id?,
        override val sentAt: Instant,
        override val sender: MessageSender,
        override val status: MessageStatus,
        override val uri: Uri,
        override val mimeType: String,
        override val fileName: String,
        val fileId: Id?, // null in case of a not yet uploaded file
        val isPrescription: Boolean,
        val thumbnailUri: Uri?,
    ) : Media {
        companion object
    }

    data class Deleted(
        override val localId: Id?,
        override val remoteId: Id?,
        override val sentAt: Instant,
        override val sender: MessageSender,
        override val status: MessageStatus,
    ) : Message {
        companion object
    }
}

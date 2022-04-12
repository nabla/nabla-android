package com.nabla.sdk.messaging.core.domain.entity

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.FileUpload
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Uri
import kotlinx.datetime.Instant

data class BaseMessage(
    val id: MessageId,
    val sentAt: Instant,
    val sender: MessageSender,
    val sendStatus: SendStatus,
    val conversationId: ConversationId,
)

sealed interface FileLocal {
    val uri: Uri
    data class Image(override val uri: Uri) : FileLocal
    data class Document(
        override val uri: Uri,
        val documentName: String?,
        val mimeType: MimeType,
    ) : FileLocal
}

sealed class FileSource<FileLocalType : FileLocal, FileUploadType : FileUpload> {
    data class Local<FileLocalType : FileLocal, FileUploadType : FileUpload>(
        val fileLocal: FileLocalType,
    ) : FileSource<FileLocalType, FileUploadType>()
    data class Uploaded<FileLocalType : FileLocal, FileUploadType : FileUpload>(
        val fileLocal: FileLocalType?,
        val fileUpload: FileUploadType,
    ) : FileSource<FileLocalType, FileUploadType>()
}

sealed interface MessageId {
    val stableId: Uuid
    val clientId: Uuid?
    val remoteId: Uuid?

    data class Local(override val clientId: Uuid) : MessageId {
        override val stableId: Uuid = clientId
        override val remoteId: Uuid? = null
    }
    data class Remote internal constructor(override val clientId: Uuid?, override val remoteId: Uuid) : MessageId {
        override val stableId: Uuid = clientId ?: remoteId
    }
}

sealed interface Message {
    val message: BaseMessage
    fun modify(status: SendStatus = message.sendStatus): Message

    data class Text(override val message: BaseMessage, val text: String) : Message {
        override fun modify(status: SendStatus): Message {
            return copy(message = message.copy(sendStatus = status))
        }
        companion object
    }

    sealed class Media<FileLocalType : FileLocal, FileUploadType : FileUpload> : Message {

        abstract val mediaSource: FileSource<FileLocalType, FileUploadType>

        data class Image(
            override val message: BaseMessage,
            override val mediaSource: FileSource<FileLocal.Image, FileUpload.Image>
        ) : Media<FileLocal.Image, FileUpload.Image>() {
            val stableUri: Uri = when (mediaSource) {
                is FileSource.Local -> mediaSource.fileLocal.uri
                is FileSource.Uploaded -> mediaSource.fileLocal?.uri ?: mediaSource.fileUpload.fileUpload.url.url
            }
            override fun modify(status: SendStatus): Message {
                return copy(message = message.copy(sendStatus = status))
            }
            fun modify(mediaSource: FileSource<FileLocal.Image, FileUpload.Image>): Image {
                return copy(mediaSource = mediaSource)
            }
            companion object
        }

        data class Document(
            override val message: BaseMessage,
            override val mediaSource: FileSource<FileLocal.Document, FileUpload.Document>
        ) : Media<FileLocal.Document, FileUpload.Document>() {
            val uri: Uri = when (mediaSource) {
                is FileSource.Local -> mediaSource.fileLocal.uri
                is FileSource.Uploaded -> mediaSource.fileUpload.fileUpload.url.url
            }
            val mimeType: MimeType = when (mediaSource) {
                is FileSource.Local -> mediaSource.fileLocal.mimeType
                is FileSource.Uploaded -> mediaSource.fileUpload.fileUpload.mimeType
            }
            val documentName: String? = when (mediaSource) {
                is FileSource.Local -> mediaSource.fileLocal.documentName
                is FileSource.Uploaded -> mediaSource.fileUpload.fileUpload.fileName
            }
            val thumbnailUri: Uri? = (mediaSource as? FileSource.Uploaded)?.fileUpload?.thumbnail?.fileUpload?.url?.url
            override fun modify(status: SendStatus): Message {
                return copy(message = message.copy(sendStatus = status))
            }
            companion object
        }
    }

    data class Deleted(override val message: BaseMessage) : Message {
        override fun modify(status: SendStatus): Message {
            return copy(message = message.copy(sendStatus = status))
        }
        companion object
    }
}

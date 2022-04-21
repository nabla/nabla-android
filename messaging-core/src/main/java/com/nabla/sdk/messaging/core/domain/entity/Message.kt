package com.nabla.sdk.messaging.core.domain.entity

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.domain.entity.FileUpload
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Uri
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal data class BaseMessage(
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
    val clientId: Uuid?
    val remoteId: Uuid?

    val stableId: Uuid

    data class Local internal constructor(override val clientId: Uuid) : MessageId {
        override val remoteId: Uuid? = null
        override val stableId: Uuid = clientId
    }
    data class Remote internal constructor(override val clientId: Uuid?, override val remoteId: Uuid) : MessageId {
        override val stableId: Uuid = clientId ?: remoteId
    }

    companion object {
        internal fun new(): MessageId = Local(uuid4())
    }
}

sealed class Message {
    internal abstract val baseMessage: BaseMessage
    internal abstract fun modify(status: SendStatus): Message

    val id: MessageId get() = baseMessage.id
    val sentAt: Instant get() = baseMessage.sentAt
    val sender: MessageSender get() = baseMessage.sender
    val sendStatus: SendStatus get() = baseMessage.sendStatus
    val conversationId: ConversationId get() = baseMessage.conversationId

    data class Text internal constructor(override val baseMessage: BaseMessage, val text: String) : Message() {
        override fun modify(status: SendStatus): Message {
            return copy(baseMessage = baseMessage.copy(sendStatus = status))
        }
        companion object {
            fun new(
                conversationId: ConversationId,
                text: String,
            ) = Text(
                BaseMessage(MessageId.new(), Clock.System.now(), MessageSender.Patient, SendStatus.ToBeSent, conversationId),
                text,
            )
        }
    }

    sealed class Media<FileLocalType : FileLocal, FileUploadType : FileUpload> : Message() {

        internal abstract val mediaSource: FileSource<FileLocalType, FileUploadType>

        data class Image internal constructor(
            override val baseMessage: BaseMessage,
            override val mediaSource: FileSource<FileLocal.Image, FileUpload.Image>
        ) : Media<FileLocal.Image, FileUpload.Image>() {
            val stableUri: Uri = when (mediaSource) {
                is FileSource.Local -> mediaSource.fileLocal.uri
                is FileSource.Uploaded -> mediaSource.fileLocal?.uri ?: mediaSource.fileUpload.fileUpload.url.url
            }
            override fun modify(status: SendStatus): Message {
                return copy(baseMessage = baseMessage.copy(sendStatus = status))
            }
            internal fun modify(mediaSource: FileSource<FileLocal.Image, FileUpload.Image>): Image {
                return copy(mediaSource = mediaSource)
            }
            companion object {
                fun new(
                    conversationId: ConversationId,
                    mediaSource: FileSource<FileLocal.Image, FileUpload.Image>,
                ) = Image(
                    BaseMessage(MessageId.new(), Clock.System.now(), MessageSender.Patient, SendStatus.ToBeSent, conversationId),
                    mediaSource
                )
            }
        }

        data class Document internal constructor(
            override val baseMessage: BaseMessage,
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
                return copy(baseMessage = baseMessage.copy(sendStatus = status))
            }

            companion object {
                fun new(
                    conversationId: ConversationId,
                    mediaSource: FileSource<FileLocal.Document, FileUpload.Document>,
                ) = Document(
                    BaseMessage(MessageId.new(), Clock.System.now(), MessageSender.Patient, SendStatus.ToBeSent, conversationId),
                    mediaSource,
                )
            }
        }
    }

    data class Deleted internal constructor(override val baseMessage: BaseMessage) : Message() {
        override fun modify(status: SendStatus): Message {
            return copy(baseMessage = baseMessage.copy(sendStatus = status))
        }

        companion object
    }

    companion object
}

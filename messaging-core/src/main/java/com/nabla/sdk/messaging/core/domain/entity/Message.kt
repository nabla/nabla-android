package com.nabla.sdk.messaging.core.domain.entity

import androidx.annotation.VisibleForTesting
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.FileUpload
import com.nabla.sdk.core.domain.entity.InternalException
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.messaging.core.domain.entity.MessageId.Local
import com.nabla.sdk.messaging.core.domain.entity.MessageId.Remote
import kotlinx.datetime.Instant

@VisibleForTesting
public data class BaseMessage(
    val id: MessageId,
    val createdAt: Instant,
    val author: MessageAuthor,
    val sendStatus: SendStatus,
    val replyTo: Message?,
)

/**
 * Url and metadata for a local device-hosted file.
 */
public sealed interface FileLocal {
    /**
     * Local device-hosted file uri. This is typically provided by the OS file providers.
     */
    public val uri: Uri
    public val mimeType: MimeType
    public val fileName: String?

    public data class Image(
        override val uri: Uri,
        override val fileName: String?,
        override val mimeType: MimeType.Image,
    ) : FileLocal {
        public companion object
    }

    public data class Video(
        override val uri: Uri,
        override val fileName: String?,
        override val mimeType: MimeType.Video,
    ) : FileLocal {
        public companion object
    }

    public data class Document(
        override val uri: Uri,
        override val fileName: String?,
        override val mimeType: MimeType,
    ) : FileLocal

    /**
     * @param estimatedDurationMs best-effort estimation of the audio file's duration in milliseconds.
     */
    public data class Audio(
        override val uri: Uri,
        override val fileName: String?,
        override val mimeType: MimeType.Audio,
        val estimatedDurationMs: Long,
    ) : FileLocal
}

public sealed class FileSource<FileLocalType : FileLocal, FileUploadType : FileUpload> {
    public data class Local<FileLocalType : FileLocal, FileUploadType : FileUpload>(
        val fileLocal: FileLocalType,
    ) : FileSource<FileLocalType, FileUploadType>() {
        public companion object
    }

    public data class Uploaded<FileLocalType : FileLocal, FileUploadType : FileUpload>(
        val fileLocal: FileLocalType?,
        val fileUpload: FileUploadType,
    ) : FileSource<FileLocalType, FileUploadType>() {
        public companion object
    }
}

/**
 * Identifier for a message, with an optimistic-friendly architecture.
 *
 * If the message is not yet sent (e.g. is sending or failed) then it only has
 * a client-made [clientId] identifier, this state is represented with [Local].
 *
 * If the message is sent and exists server-side, it then necessarily has a server-made identifier
 * called [remoteId] and maybe also a client-made [clientId]. This state is represented with [Remote].
 *
 * Tip: Privilege using [stableId] if you want an identifier that remains the same before, during and after
 * sending a message. Typical use case is glitch-free optimistic sending.
 */
public sealed interface MessageId {
    public val clientId: Uuid?
    public val remoteId: Uuid?

    public val stableId: Uuid

    public data class Local @VisibleForTesting public constructor(override val clientId: Uuid) : MessageId {
        override val remoteId: Uuid? = null
        override val stableId: Uuid = clientId
    }

    public data class Remote @VisibleForTesting public constructor(
        override val clientId: Uuid?,
        override val remoteId: Uuid,
    ) : MessageId {
        override val stableId: Uuid = clientId ?: remoteId
    }

    @NablaInternal
    public fun requireLocal(): Local {
        return this as? Local ?: throw InternalException(
            IllegalStateException("Require local but $this is ${javaClass.simpleName}")
        )
    }
}

public sealed class Message : ConversationItem {
    @NablaInternal
    public abstract val baseMessage: BaseMessage
    @NablaInternal
    public abstract fun modify(status: SendStatus): Message

    public val id: MessageId get() = baseMessage.id
    public val sentAt: Instant get() = baseMessage.createdAt
    public val author: MessageAuthor get() = baseMessage.author
    public val sendStatus: SendStatus get() = baseMessage.sendStatus

    /**
     * the message that this message is a reply to, or null if this is a root message.
     */
    public val replyTo: Message? get() = baseMessage.replyTo

    override val createdAt: Instant get() = baseMessage.createdAt

    public data class Text @VisibleForTesting public constructor(
        override val baseMessage: BaseMessage,
        val text: String,
    ) : Message() {
        override fun modify(status: SendStatus): Message {
            return copy(baseMessage = baseMessage.copy(sendStatus = status))
        }

        @VisibleForTesting
        public companion object
    }

    public sealed class Media<FileLocalType : FileLocal, FileUploadType : FileUpload> : Message() {

        internal abstract val mediaSource: FileSource<FileLocalType, FileUploadType>

        public val stableUri: Uri
            get() = when (val mediaSource = mediaSource) {
                is FileSource.Local -> mediaSource.fileLocal.uri
                is FileSource.Uploaded ->
                    mediaSource.fileLocal?.uri
                        ?: mediaSource.fileUpload.fileUpload.url.url
            }

        public val mimeType: MimeType
            get() = when (val mediaSource = mediaSource) {
                is FileSource.Local -> mediaSource.fileLocal.mimeType
                is FileSource.Uploaded -> mediaSource.fileUpload.fileUpload.mimeType
            }

        public val fileName: String?
            get() = when (val mediaSource = mediaSource) {
                is FileSource.Local -> mediaSource.fileLocal.fileName
                is FileSource.Uploaded -> mediaSource.fileUpload.fileUpload.fileName
            }

        public data class Image @VisibleForTesting public constructor(
            override val baseMessage: BaseMessage,
            override val mediaSource: FileSource<FileLocal.Image, FileUpload.Image>,
        ) : Media<FileLocal.Image, FileUpload.Image>() {

            override fun modify(status: SendStatus): Message =
                copy(baseMessage = baseMessage.copy(sendStatus = status))

            internal fun modify(mediaSource: FileSource<FileLocal.Image, FileUpload.Image>): Image =
                copy(mediaSource = mediaSource)

            @VisibleForTesting
            public companion object
        }

        public data class Video @VisibleForTesting public constructor(
            override val baseMessage: BaseMessage,
            override val mediaSource: FileSource<FileLocal.Video, FileUpload.Video>,
        ) : Media<FileLocal.Video, FileUpload.Video>() {

            override fun modify(status: SendStatus): Message =
                copy(baseMessage = baseMessage.copy(sendStatus = status))

            internal fun modify(mediaSource: FileSource<FileLocal.Video, FileUpload.Video>): Video =
                copy(mediaSource = mediaSource)

            @VisibleForTesting
            public companion object
        }

        public data class Document @VisibleForTesting public constructor(
            override val baseMessage: BaseMessage,
            override val mediaSource: FileSource<FileLocal.Document, FileUpload.Document>,
        ) : Media<FileLocal.Document, FileUpload.Document>() {

            val thumbnailUri: Uri? =
                (mediaSource as? FileSource.Uploaded)?.fileUpload?.thumbnail?.fileUpload?.url?.url

            override fun modify(status: SendStatus): Message =
                copy(baseMessage = baseMessage.copy(sendStatus = status))

            @VisibleForTesting
            public companion object
        }

        public data class Audio @VisibleForTesting public constructor(
            override val baseMessage: BaseMessage,
            override val mediaSource: FileSource<FileLocal.Audio, FileUpload.Audio>,
        ) : Media<FileLocal.Audio, FileUpload.Audio>() {

            val durationMs: Long? = when (mediaSource) {
                is FileSource.Local -> mediaSource.fileLocal.estimatedDurationMs
                is FileSource.Uploaded -> mediaSource.fileUpload.durationMs
            }

            override fun modify(status: SendStatus): Message =
                copy(baseMessage = baseMessage.copy(sendStatus = status))

            @VisibleForTesting
            public companion object
        }
    }

    public data class LivekitRoom(
        override val baseMessage: BaseMessage,
        val livekitRoomId: Uuid,
        val livekitRoomStatus: LivekitRoomStatus,
    ) : Message() {
        override fun modify(status: SendStatus): Message {
            return copy(baseMessage = baseMessage.copy(sendStatus = status))
        }
    }

    public data class Deleted @VisibleForTesting public constructor(override val baseMessage: BaseMessage) : Message() {
        override fun modify(status: SendStatus): Message {
            return copy(baseMessage = baseMessage.copy(sendStatus = status))
        }

        public companion object
    }

    public companion object
}

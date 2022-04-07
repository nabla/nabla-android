package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.FileUpload
import com.nabla.sdk.core.domain.entity.Id
import kotlinx.datetime.Instant

data class BaseMessage(
    val id: MessageId,
    val sentAt: Instant?,
    val sender: MessageSender,
    val status: MessageStatus
)

sealed class MessageId {
    abstract val stableId: Id
    abstract val clientId: Id?
    abstract val remoteId: Id?

    data class Local(override val clientId: Id) : MessageId() {
        override val stableId: Id = clientId
        override val remoteId: Id? = null
    }
    data class Remote(override val clientId: Id?, override val remoteId: Id) : MessageId() {
        override val stableId: Id = clientId ?: remoteId
    }
}

sealed class Message {
    abstract val message: BaseMessage
    abstract fun modify(status: MessageStatus = message.status): Message

    data class Text(override val message: BaseMessage, val text: String) : Message() {
        override fun modify(status: MessageStatus): Message {
            return copy(message = message.copy(status = status))
        }
        companion object
    }

    sealed class Media : Message() {

        data class Image(
            override val message: BaseMessage,
            val image: FileUpload.Image,
        ) : Media() {
            override fun modify(status: MessageStatus): Message {
                return copy(message = message.copy(status = status))
            }
            companion object
        }

        data class Document(
            override val message: BaseMessage,
            val document: FileUpload.Document
        ) : Media() {
            override fun modify(status: MessageStatus): Message {
                return copy(message = message.copy(status = status))
            }
            companion object
        }
    }

    data class Deleted(override val message: BaseMessage) : Message() {
        override fun modify(status: MessageStatus): Message {
            return copy(message = message.copy(status = status))
        }
        companion object
    }
}

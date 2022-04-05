package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.FileUpload
import com.nabla.sdk.core.domain.entity.Id
import kotlinx.datetime.Instant

data class BaseMessage(
    val localId: Id?,
    val remoteId: Id?,
    val sentAt: Instant?,
    val sender: MessageSender,
    val status: MessageStatus?
)

sealed class Message {
    abstract val message: BaseMessage

    data class Text(override val message: BaseMessage, val text: String) : Message() {
        companion object
    }

    sealed class Media : Message() {

        data class Image(
            override val message: BaseMessage,
            val image: FileUpload.Image,
        ) : Media() {
            companion object
        }

        data class Document(
            override val message: BaseMessage,
            val document: FileUpload.Document
        ) : Media() {
            companion object
        }
    }

    data class Deleted(override val message: BaseMessage) : Message() {
        companion object
    }
}

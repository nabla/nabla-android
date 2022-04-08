package com.nabla.sdk.messaging.core.domain.entity

sealed interface MessageStatus {
    object Sending : MessageStatus
    object Sent : MessageStatus
    object Read : MessageStatus
    object ErrorSending : MessageStatus

    val isSent: Boolean
        get() = this is MessageStatus.Sent || this is MessageStatus.Read
}

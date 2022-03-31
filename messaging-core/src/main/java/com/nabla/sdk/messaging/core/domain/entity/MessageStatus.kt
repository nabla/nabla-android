package com.nabla.sdk.messaging.core.domain.entity

sealed interface MessageStatus {
    object Sending : MessageStatus
    object Sent : MessageStatus
    object Read : MessageStatus
    object ErrorSending : MessageStatus
}

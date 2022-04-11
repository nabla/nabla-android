package com.nabla.sdk.messaging.core.domain.entity

sealed interface SendStatus {
    object ToBeSent : SendStatus
    object Sending : SendStatus
    object Sent : SendStatus
    object ErrorSending : SendStatus
}

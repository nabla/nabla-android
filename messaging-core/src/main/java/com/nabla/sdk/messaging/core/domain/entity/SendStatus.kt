package com.nabla.sdk.messaging.core.domain.entity

public sealed interface SendStatus {
    public object Sending : SendStatus
    public object Sent : SendStatus
    public object ErrorSending : SendStatus
}

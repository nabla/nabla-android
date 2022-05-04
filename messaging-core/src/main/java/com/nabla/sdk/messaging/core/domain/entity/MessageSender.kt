package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.User

public sealed interface MessageSender {
    @JvmInline
    public value class Provider(public val provider: User.Provider) : MessageSender
    public object Patient : MessageSender
    public object System : MessageSender
    public object DeletedProvider : MessageSender
    public object Unknown : MessageSender
}

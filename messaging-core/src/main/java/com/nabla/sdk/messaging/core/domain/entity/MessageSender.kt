package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.User

sealed interface MessageSender {
    @JvmInline
    value class Provider(val provider: User.Provider) : MessageSender
    object Patient : MessageSender
    object System : MessageSender
    object Unknown : MessageSender
}

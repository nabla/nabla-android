package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.SystemUser

public sealed interface MessageAuthor {
    @JvmInline
    public value class Provider(public val provider: CoreProvider) : MessageAuthor
    public object Patient : MessageAuthor
    @JvmInline
    public value class System(public val system: SystemUser) : MessageAuthor
    public object DeletedProvider : MessageAuthor
    public object Unknown : MessageAuthor
}

private typealias CoreProvider = com.nabla.sdk.core.domain.entity.Provider

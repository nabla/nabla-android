package com.nabla.sdk.messaging.core.domain.entity

import kotlinx.datetime.Instant

public sealed interface ConversationItem {
    public val createdAt: Instant
}

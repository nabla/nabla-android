package com.nabla.sdk.core.domain.entity

import com.nabla.sdk.messaging.core.domain.entity.ConversationWithMessages

internal data class PaginatedConversationWithMessages(
    val conversationWithMessages: ConversationWithMessages,
    val hasMore: Boolean,
)

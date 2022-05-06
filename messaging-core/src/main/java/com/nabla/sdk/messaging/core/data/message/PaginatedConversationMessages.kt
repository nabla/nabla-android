package com.nabla.sdk.messaging.core.data.message

import com.nabla.sdk.messaging.core.domain.entity.ConversationMessages

internal data class PaginatedConversationMessages(
    val conversationMessages: ConversationMessages,
    val hasMore: Boolean,
)

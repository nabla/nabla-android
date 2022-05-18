package com.nabla.sdk.messaging.core.data.message

import com.nabla.sdk.messaging.core.domain.entity.ConversationItems

internal data class PaginatedConversationItems(
    val conversationItems: ConversationItems,
    val hasMore: Boolean,
)

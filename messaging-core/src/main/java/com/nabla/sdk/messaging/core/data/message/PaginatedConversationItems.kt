package com.nabla.sdk.messaging.core.data.message

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.messaging.core.domain.entity.ConversationItems

@NablaInternal
public data class PaginatedConversationItems(
    val conversationItems: ConversationItems,
    val hasMore: Boolean,
)

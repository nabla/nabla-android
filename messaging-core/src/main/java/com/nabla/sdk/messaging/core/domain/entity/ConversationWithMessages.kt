package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.PaginatedList

data class ConversationWithMessages(
    val conversation: Conversation,
    val messages: PaginatedList<Message>,
) {
    companion object
}

package com.nabla.sdk.messaging.core.domain.entity

public data class ConversationItems(
    val conversationId: ConversationId,
    val items: List<ConversationItem>,
) {
    public companion object
}

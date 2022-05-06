package com.nabla.sdk.messaging.core.domain.entity

public data class ConversationMessages(
    val conversationId: ConversationId,
    val messages: List<Message>,
) {
    public companion object
}

package com.nabla.sdk.messaging.core.domain.entity

public data class ConversationWithMessages(
    val conversation: Conversation,
    val messages: List<Message>,
) {
    public companion object
}

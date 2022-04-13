package com.nabla.sdk.messaging.core.domain.entity

data class ConversationWithMessages(
    val conversation: Conversation,
    val messages: List<Message>,
) {
    companion object
}

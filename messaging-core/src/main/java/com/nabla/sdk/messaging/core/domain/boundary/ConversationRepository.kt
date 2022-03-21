package com.nabla.sdk.messaging.core.domain.boundary

interface ConversationRepository {
    suspend fun createConversation()
}

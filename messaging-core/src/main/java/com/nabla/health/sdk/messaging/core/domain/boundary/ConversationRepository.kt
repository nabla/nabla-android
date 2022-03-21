package com.nabla.health.sdk.messaging.core.domain.boundary

interface ConversationRepository {
    suspend fun createConversation()
}

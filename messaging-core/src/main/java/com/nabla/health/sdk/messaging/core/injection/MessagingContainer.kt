package com.nabla.health.sdk.messaging.core.injection

import com.nabla.health.sdk.messaging.core.data.ConversationRepositoryImpl
import com.nabla.health.sdk.messaging.core.domain.boundary.ConversationRepository

class MessagingContainer {
    val conversationRepository: ConversationRepository = ConversationRepositoryImpl()
}

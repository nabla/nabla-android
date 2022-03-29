package com.nabla.sdk.messaging.core.injection

import com.nabla.sdk.messaging.core.data.ConversationRepositoryImpl
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository

class MessagingContainer {
    val conversationRepository: ConversationRepository = ConversationRepositoryImpl()
}

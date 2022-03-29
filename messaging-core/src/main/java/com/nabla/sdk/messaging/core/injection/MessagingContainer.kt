package com.nabla.sdk.messaging.core.injection

import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository

internal class MessagingContainer(val conversationRepository: ConversationRepository)

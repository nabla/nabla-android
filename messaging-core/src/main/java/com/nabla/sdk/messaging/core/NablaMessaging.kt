package com.nabla.sdk.messaging.core

import com.nabla.sdk.core.NablaCore
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.boundary.MessageRepository
import com.nabla.sdk.messaging.core.injection.MessagingContainer

class NablaMessaging private constructor(coreContainer: CoreContainer) {

    private val messagingContainer = MessagingContainer(
        coreContainer.logger,
        coreContainer.apolloClient,
        coreContainer.fileUploadRepository
    )

    val conversationRepository: ConversationRepository by lazy { messagingContainer.conversationRepository }
    val messageRepository: MessageRepository by lazy { messagingContainer.messageRepository }

    companion object {
        val instance = NablaMessaging(NablaCore.instance.coreContainer)
    }
}

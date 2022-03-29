package com.nabla.sdk.messaging.ui

import com.nabla.sdk.messaging.core.NablaMessaging
import com.nabla.sdk.messaging.ui.scene.ConversationListViewModel

fun NablaMessaging.createConversationListViewModel(): ConversationListViewModel {
    return ConversationListViewModel(this.conversationRepository)
}

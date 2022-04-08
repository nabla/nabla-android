package com.nabla.sdk.messaging.ui.injection

import com.nabla.sdk.messaging.core.NablaMessaging
import com.nabla.sdk.messaging.core.data.ConversationRepositoryMock
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListViewModel

class NablaMessagingUi(
    private val nablaMessaging: NablaMessaging,
) {
    fun createConversationListViewModel(
        onConversationClicked: (conversationId: ConversationId) -> Unit,
        onErrorRetryWhen: suspend (error: Throwable, attempt: Long) -> Boolean,
    ): ConversationListViewModel {
        return ConversationListViewModel(
            ConversationRepositoryMock(),
            onConversationClicked,
            onErrorRetryWhen,
        )
    }
}

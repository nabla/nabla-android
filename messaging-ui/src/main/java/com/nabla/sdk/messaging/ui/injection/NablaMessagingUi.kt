package com.nabla.sdk.messaging.ui.injection

import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.messaging.core.NablaMessaging
import com.nabla.sdk.messaging.ui.scene.ConversationListViewModel

class NablaMessagingUi(
    private val nablaMessaging: NablaMessaging,
) {
    fun createConversationListViewModel(
        onConversationClicked: (conversationId: Id) -> Unit,
        onErrorRetryWhen: suspend (error: Throwable, attempt: Long) -> Boolean,
    ): ConversationListViewModel {
        return ConversationListViewModel(
            nablaMessaging.conversationRepository,
            onConversationClicked,
            onErrorRetryWhen,
        )
    }
}

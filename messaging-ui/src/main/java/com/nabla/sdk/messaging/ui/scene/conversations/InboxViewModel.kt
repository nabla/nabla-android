package com.nabla.sdk.messaging.ui.scene.conversations

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.ui.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

internal class InboxViewModel(
    private val messagingClient: NablaMessagingClient,
) : ViewModel() {
    private val errorAlertMutableFlow = MutableLiveFlow<ErrorAlert>()
    internal val errorAlertEventFlow: LiveFlow<ErrorAlert> = errorAlertMutableFlow

    private val openConversationMutableFlow = MutableLiveFlow<ConversationId>()
    internal val openConversationFlow: LiveFlow<ConversationId> = openConversationMutableFlow

    private val isCreatingConversationMutableFlow = MutableStateFlow(false)
    internal val isCreatingConversationFlow: StateFlow<Boolean> = isCreatingConversationMutableFlow

    internal fun createConversation() {
        viewModelScope.launch {
            isCreatingConversationMutableFlow.emit(true)

            messagingClient.createConversation()
                .onFailure { error ->
                    messagingClient.logger.warn("Error while creating conversation", error)
                    errorAlertMutableFlow.emit(ErrorAlert.CreatingConversation)
                }
                .onSuccess { conversation ->
                    openConversationMutableFlow.emit(conversation.id)
                }

            isCreatingConversationMutableFlow.emit(false)
        }
    }

    internal sealed class ErrorAlert(@StringRes val errorMessageRes: Int) {
        object CreatingConversation : ErrorAlert(R.string.nabla_error_message_conversation_creation)
    }
}

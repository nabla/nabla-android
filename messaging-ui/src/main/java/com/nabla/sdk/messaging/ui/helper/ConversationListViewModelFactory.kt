package com.nabla.sdk.messaging.ui.helper

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.nabla.sdk.messaging.core.NablaMessaging
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListViewModel

class ConversationListViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val onConversationClicked: (conversationId: ConversationId) -> Unit = { _ ->
        // TODO default behavior
    },
    private val nablaMessaging: NablaMessaging = NablaMessaging.getInstance(),
) : AbstractSavedStateViewModelFactory(owner, null) {
    override fun <T : ViewModel> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T {
        require(modelClass == ConversationListViewModel::class.java) {
            "expected ViewModel to be ConversationListViewModel"
        }

        @Suppress("UNCHECKED_CAST")
        return ConversationListViewModel(
            nablaMessaging,
            onConversationClicked,
        ) as T
    }
}

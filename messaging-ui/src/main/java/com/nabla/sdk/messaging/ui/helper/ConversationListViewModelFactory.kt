package com.nabla.sdk.messaging.ui.helper

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.messagingClient
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListViewModel

public class ConversationListViewModelFactory(
    owner: SavedStateRegistryOwner,
    private val messagingClient: NablaMessagingClient = NablaClient.getInstance().messagingClient,
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
            messagingClient,
        ) as T
    }
}

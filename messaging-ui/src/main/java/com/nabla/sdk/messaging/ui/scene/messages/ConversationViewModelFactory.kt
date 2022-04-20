package com.nabla.sdk.messaging.ui.scene.messages

import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.nabla.sdk.messaging.core.NablaMessaging
import com.nabla.sdk.messaging.core.domain.entity.ConversationId

class ConversationViewModelFactory(
    owner: SavedStateRegistryOwner,
    conversationId: ConversationId,
    private val nablaMessaging: NablaMessaging,
    private val onErrorCallback: (message: String, Throwable) -> Unit,
) : AbstractSavedStateViewModelFactory(owner, ConversationFragment.newArgsBundle(conversationId)) {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
        return ConversationViewModel(
            nablaMessaging = nablaMessaging,
            onErrorCallback = onErrorCallback,
            savedStateHandle = handle,
        ) as T
    }
}

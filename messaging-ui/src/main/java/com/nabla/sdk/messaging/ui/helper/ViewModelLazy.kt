package com.nabla.sdk.messaging.ui.helper

import android.os.Bundle
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.savedstate.SavedStateRegistryOwner
import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.messaging.core.NablaMessaging
import com.nabla.sdk.messaging.ui.injection.NablaMessagingUi
import com.nabla.sdk.messaging.ui.scene.ConversationListViewModel

class ConversationListViewModelFactory(
    owner: SavedStateRegistryOwner,
    defaultArgs: Bundle? = null,
    private val onConversationClicked: (conversationId: Id) -> Unit = { _ ->
        // TODO default behavior
    },
    private val onErrorRetryWhen: suspend (error: Throwable, attempt: Long) -> Boolean = { _, _ -> false },
    private val nablaMessagingUi: NablaMessagingUi = NablaMessagingUi(NablaMessaging.instance),
) : AbstractSavedStateViewModelFactory(owner, defaultArgs) {
    override fun <T : ViewModel?> create(
        key: String,
        modelClass: Class<T>,
        handle: SavedStateHandle,
    ): T {
        require(modelClass == ConversationListViewModel::class.java) {
            "expected ViewModel to be ConversationListViewModel"
        }

        @Suppress("UNCHECKED_CAST")
        return nablaMessagingUi.createConversationListViewModel(
            onConversationClicked,
            onErrorRetryWhen,
        ) as T
    }
}

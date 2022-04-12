package com.nabla.sdk.messaging.ui.scene.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.messaging.core.NablaMessaging
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationListViewModel(
    private val nablaMessaging: NablaMessaging,
    internal val onConversationClicked: (conversationId: ConversationId) -> Unit,
    private val onErrorRetryWhen: suspend (error: Throwable, attempt: Long) -> Boolean,
) : ViewModel() {

    internal val stateFlow: StateFlow<State> =
        nablaMessaging.watchConversations()
            .map { conversations ->
                State.Loaded(
                    conversations.items.map { it.toUiModel() } +
                        if (conversations.hasMore) listOf(ItemUiModel.Loading) else emptyList()
                ).eraseType()
            }
            .retryWhen { cause, attempt ->
                onErrorRetryWhen(cause, attempt).also { retry ->
                    emit(if (retry) State.Loading else State.Hidden)
                }
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = State.Loading)

    internal fun onListReachedBottom() {
        viewModelScope.launch {
            runCatchingCancellable {
                nablaMessaging.loadMoreConversations()
            }.onFailure {
                // TODO
            }
        }
    }

    internal sealed interface State {
        object Loading : State
        object Hidden : State
        data class Loaded(
            val items: List<ItemUiModel>,
        ) : State

        fun eraseType() = this
    }
}

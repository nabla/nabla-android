package com.nabla.sdk.messaging.ui.scene.conversations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationListViewModel(
    private val conversationRepository: ConversationRepository,
    internal val onConversationClicked: (conversationId: ConversationId) -> Unit,
    internal val onErrorRetryWhen: suspend (error: Throwable, attempt: Long) -> Boolean,
) : ViewModel() {

    val stateFlow: StateFlow<State> =
        conversationRepository.watchConversations()
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

    fun createConversation() {
        viewModelScope.launch {
            runCatchingCancellable {
                conversationRepository.createConversation()
            }.onFailure {
                // TODO
            }
        }
    }

    fun onListReachedBottom() {
        viewModelScope.launch {
            runCatchingCancellable {
                conversationRepository.loadMoreConversations()
            }.onFailure {
                // TODO
            }
        }
    }

    sealed interface State {
        object Loading : State
        object Hidden : State
        data class Loaded(
            val items: List<ItemUiModel>,
        ) : State

        fun eraseType() = this
    }
}

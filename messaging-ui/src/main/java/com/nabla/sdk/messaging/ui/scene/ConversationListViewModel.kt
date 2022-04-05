package com.nabla.sdk.messaging.ui.scene

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.domain.entity.Id
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.messaging.core.domain.boundary.ConversationRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ConversationListViewModel(
    private val conversationRepository: ConversationRepository,
    internal val onConversationClicked: (conversationId: Id) -> Unit,
    internal val onErrorRetryWhen: suspend (error: Throwable, attempt: Long) -> Boolean,
) : ViewModel() {

    val stateFlow: StateFlow<State> =
        conversationRepository.watchConversations()
            .map { conversations ->
                State.Loaded(conversations.items.map { it.toUiModel() }).eraseType()
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
            }
        }
    }

    sealed interface State {
        object Loading : State
        object Hidden : State
        data class Loaded(
            val conversations: List<ConversationItemUiModel>,
        ) : State

        fun eraseType() = this
    }
}

package com.nabla.sdk.messaging.ui.scene.conversations

import androidx.annotation.CheckResult
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.emitIn
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.asNetworkOrGeneric
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.ui.R
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

public class ConversationListViewModel(
    private val messagingClient: NablaMessagingClient,
) : ViewModel() {
    private var latestLoadMoreCallback: (@CheckResult suspend () -> Result<Unit>)? = null

    private val retryAfterErrorTriggerFlow = MutableSharedFlow<Unit>()

    private val errorAlertMutableFlow = MutableLiveFlow<ErrorAlert>()
    internal val errorAlertEventFlow: LiveFlow<ErrorAlert> = errorAlertMutableFlow

    internal val stateFlow: StateFlow<State> =
        messagingClient.watchConversations()
            .map { result ->
                latestLoadMoreCallback = result.loadMore

                if (result.content.isEmpty()) {
                    State.Empty
                } else {
                    State.Loaded(
                        items = result.content.map { it.toUiModel() } + if (result.loadMore != null) listOf(ItemUiModel.Loading) else emptyList(),
                    )
                }
            }
            .retryWhen { cause, _ ->
                messagingClient.logger.warn(
                    domain = LOGGING_DOMAIN,
                    message = "Failed to fetch conversation list",
                    error = cause,
                )

                emit(State.Error(cause.asNetworkOrGeneric))

                retryAfterErrorTriggerFlow.first()
                emit(State.Loading)
                true
            }
            .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = State.Loading)

    internal fun onRetryClicked() {
        retryAfterErrorTriggerFlow.emitIn(viewModelScope, Unit)
    }

    internal fun onListReachedBottom() {
        val loadMore = latestLoadMoreCallback ?: return

        viewModelScope.launch {
            loadMore()
                .onFailure { error ->
                    messagingClient.logger.warn(
                        domain = LOGGING_DOMAIN,
                        message = "Error while loading more conversations",
                        error = error,
                    )
                    errorAlertMutableFlow.emit(ErrorAlert.LoadingMoreConversations)
                }
        }
    }

    internal sealed class ErrorAlert(@StringRes val errorMessageRes: Int) {
        object LoadingMoreConversations : ErrorAlert(R.string.nabla_error_message_conversations_loading_more)
    }

    internal sealed interface State {
        object Loading : State
        data class Error(val errorUiModel: ErrorUiModel) : State
        object Empty : State
        data class Loaded(
            val items: List<ItemUiModel>,
        ) : State
    }

    private companion object {
        private const val LOGGING_DOMAIN = "UI-ConversationsList"
    }
}

package com.nabla.sdk.messaging.ui.scene.conversations

import androidx.annotation.CheckResult
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.emitIn
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.messaging.core.NablaMessaging
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

public class ConversationListViewModel(
    private val nablaMessaging: NablaMessaging,
) : ViewModel() {
    private var latestLoadMoreCallback: (@CheckResult suspend () -> Result<Unit>)? = null

    private val retryAfterErrorTriggerFlow = MutableSharedFlow<Unit>()

    private val errorAlertMutableFlow = MutableLiveFlow<ErrorAlert>()
    internal val errorAlertEventFlow: LiveFlow<ErrorAlert> = errorAlertMutableFlow

    internal val stateFlow: StateFlow<State> =
        nablaMessaging.watchConversations()
            .map { result ->
                latestLoadMoreCallback = result.loadMore

                State.Loaded(
                    items = result.content.map { it.toUiModel() } + if (result.loadMore != null) listOf(ItemUiModel.Loading) else emptyList(),
                ).eraseType()
            }
            .retryWhen { cause, _ ->
                nablaMessaging.logger.error("Failed to fetch conversation list", cause, tag = LOGGING_TAG)

                emit(
                    State.Error(if (cause is NablaException.Network) ErrorUiModel.Network else ErrorUiModel.Generic)
                )

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
                    nablaMessaging.logger.error("Error while loading more conversations", error, tag = LOGGING_TAG)
                    errorAlertMutableFlow.emit(ErrorAlert.LoadingMoreConversations(error))
                }
        }
    }

    internal sealed interface ErrorAlert {
        data class LoadingMoreConversations(val throwable: Throwable) : ErrorAlert
    }

    internal sealed interface State {
        object Loading : State
        data class Error(val errorUiModel: ErrorUiModel) : State
        data class Loaded(
            val items: List<ItemUiModel>,
        ) : State

        fun eraseType() = this
    }

    private companion object {
        private val LOGGING_TAG = Logger.asSdkTag("UI-ConversationsList")
    }
}

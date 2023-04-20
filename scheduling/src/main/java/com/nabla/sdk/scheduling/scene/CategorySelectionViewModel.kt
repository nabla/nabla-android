package com.nabla.sdk.scheduling.scene

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.ui.helpers.FlowCollectorExtension.emitIn
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.ErrorUiModel.Companion.asNetworkOrGeneric
import com.nabla.sdk.scheduling.SCHEDULING_DOMAIN
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategory
import com.nabla.sdk.scheduling.schedulingPrivateClient
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn

internal class CategorySelectionViewModel(private val nablaClient: NablaClient) : ViewModel() {
    private val retryTriggerFlow = MutableSharedFlow<Unit>()

    internal val stateFlow: StateFlow<State> = flow {
        val categories = nablaClient.schedulingPrivateClient.getAppointmentCategories().getOrThrow()

        emit(if (categories.isEmpty()) State.Empty else State.Loaded(categories))
    }.retryWhen { cause, _ ->
        nablaClient.coreContainer.logger.warn(
            message = "failed to get appointment categories",
            error = cause,
            domain = Logger.SCHEDULING_DOMAIN.UI,
        )
        emit(State.Error(cause.asNetworkOrGeneric))
        retryTriggerFlow.first()
        emit(State.Loading)
        true
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    fun onClickRetry() {
        retryTriggerFlow.emitIn(viewModelScope, Unit)
    }

    internal sealed interface State {
        object Loading : State
        object Empty : State
        data class Error(val errorUiModel: ErrorUiModel) : State
        data class Loaded(val items: List<AppointmentCategory>) : State
    }
}

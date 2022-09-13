package com.nabla.sdk.scheduling.scene

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.ServerException
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.emitIn
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.asNetworkOrGeneric
import com.nabla.sdk.scheduling.SCHEDULING_DOMAIN
import com.nabla.sdk.scheduling.domain.entity.CategoryId
import com.nabla.sdk.scheduling.schedulingInternalModule
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import java.util.UUID

internal class AppointmentConfirmationViewModel(
    private val categoryId: CategoryId,
    private val providerId: UUID,
    private val slot: Instant,
    private val nablaClient: NablaClient,
) : ViewModel() {
    private val retryTriggerFlow = MutableSharedFlow<Unit>()

    private var firstConsentGrantedFlow = MutableStateFlow(false)
    private var secondConsentGrantedFlow = MutableStateFlow(false)

    private var isSubmittingFlow = MutableStateFlow(false)

    private val eventsMutableFlow = MutableLiveFlow<Event>()
    val eventsFlow: LiveFlow<Event> = eventsMutableFlow

    val stateFlow: StateFlow<State> = flow {
        val provider = nablaClient.coreContainer.providerRepository.getProvider(providerId)
        emit(State.Loaded(provider, slot))
    }
        .combine(isSubmittingFlow) { state, isSubmitting ->
            if (isSubmitting) State.Loading else state
        }
        .retryWhen { cause, _ ->
            nablaClient.coreContainer.logger.warn(
                message = "failed to get appointment details for confirmation",
                error = cause,
                domain = Logger.SCHEDULING_DOMAIN.UI
            )
            emit(State.Error(cause.asNetworkOrGeneric))
            retryTriggerFlow.first()
            emit(State.Loading)
            true
        }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    val canSubmitFlow = combine(
        firstConsentGrantedFlow,
        secondConsentGrantedFlow,
        stateFlow,
    ) { firstConsent, secondConsent, state ->
        firstConsent && secondConsent && state is State.Loaded
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun onConfirmClicked() {
        if (!canSubmitFlow.value) return

        viewModelScope.launch {
            isSubmittingFlow.value = true
            nablaClient.schedulingInternalModule.scheduleAppointment(categoryId, providerId, slot)
                .onFailure { throwable ->
                    nablaClient.coreContainer.logger.warn("failed scheduling appointment", throwable, domain = Logger.SCHEDULING_DOMAIN.UI)

                    eventsMutableFlow.emit(
                        Event.FailedSubmitting(
                            if (throwable is ServerException) {
                                throwable.serverMessage
                            } else {
                                nablaClient.coreContainer.stringResolver.resolve(throwable.asNetworkOrGeneric.titleRes)
                            }
                        )
                    )
                    isSubmittingFlow.value = false
                }.onSuccess {
                    eventsMutableFlow.emit(Event.Finish)
                }
        }
    }

    fun onClickRetry() {
        retryTriggerFlow.emitIn(viewModelScope, Unit)
    }

    fun onFirstConsentChecked(checked: Boolean) {
        firstConsentGrantedFlow.value = checked
    }

    fun onSecondConsentChecked(checked: Boolean) {
        secondConsentGrantedFlow.value = checked
    }

    sealed interface State {
        object Loading : State
        data class Error(val errorUiModel: ErrorUiModel) : State
        data class Loaded(
            val provider: Provider,
            val slot: Instant,
        ) : State
    }

    sealed interface Event {
        object Finish : Event
        data class FailedSubmitting(val errorMessage: String) : Event
    }
}

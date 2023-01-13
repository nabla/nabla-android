package com.nabla.sdk.scheduling.scene

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.ServerException
import com.nabla.sdk.core.domain.entity.StringOrRes
import com.nabla.sdk.core.domain.entity.asStringOrRes
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.emitIn
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.asNetworkOrGeneric
import com.nabla.sdk.scheduling.SCHEDULING_DOMAIN
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategoryId
import com.nabla.sdk.scheduling.domain.entity.AppointmentConfirmationConsents
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.schedulingInternalModule
import kotlinx.coroutines.async
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
    private val locationType: AppointmentLocationType,
    private val categoryId: AppointmentCategoryId,
    private val providerId: UUID,
    private val slot: Instant,
    private val nablaClient: NablaClient,
) : ViewModel() {
    private val retryTriggerFlow = MutableSharedFlow<Unit>()

    private val consentsGrantedFlow = MutableStateFlow(emptyList<Boolean>())

    private var isSubmittingFlow = MutableStateFlow(false)

    private val eventsMutableFlow = MutableLiveFlow<Event>()
    val eventsFlow: LiveFlow<Event> = eventsMutableFlow

    val stateFlow: StateFlow<State> = flow {
        val asyncConsents = viewModelScope.async {
            nablaClient.schedulingInternalModule.getAppointmentConfirmationConsents(locationType)
                .getOrThrow()
        }
        val provider = viewModelScope.async {
            nablaClient.coreContainer.providerRepository.getProvider(providerId)
        }
        val consents = asyncConsents.await()
        consentsGrantedFlow.value = List(consents.htmlConsents.size) {
            false
        }
        emit(State.Loaded(provider.await(), slot, consents))
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
        consentsGrantedFlow,
        stateFlow,
    ) { consents, state ->
        consents.all { it } && state is State.Loaded
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun onConfirmClicked() {
        if (!canSubmitFlow.value) return

        viewModelScope.launch {
            isSubmittingFlow.value = true
            nablaClient.schedulingInternalModule.scheduleAppointment(locationType, categoryId, providerId, slot)
                .onFailure { throwable ->
                    nablaClient.coreContainer.logger.warn("failed scheduling appointment", throwable, domain = Logger.SCHEDULING_DOMAIN.UI)

                    eventsMutableFlow.emit(
                        Event.FailedSubmitting(
                            if (throwable is ServerException) {
                                throwable.serverMessage.asStringOrRes()
                            } else {
                                throwable.asNetworkOrGeneric.title
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

    fun onConsentChecked(index: Int, checked: Boolean) {
        consentsGrantedFlow.value = consentsGrantedFlow.value.toMutableList().apply {
            this[index] = checked
        }.toList()
    }

    sealed interface State {
        object Loading : State
        data class Error(val errorUiModel: ErrorUiModel) : State
        data class Loaded(
            val provider: Provider,
            val slot: Instant,
            val consents: AppointmentConfirmationConsents,
        ) : State
    }

    sealed interface Event {
        object Finish : Event
        data class FailedSubmitting(val errorMessage: StringOrRes) : Event
    }
}

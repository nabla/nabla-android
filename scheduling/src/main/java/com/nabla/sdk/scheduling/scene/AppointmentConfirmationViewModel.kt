package com.nabla.sdk.scheduling.scene

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.ServerException
import com.nabla.sdk.core.domain.entity.StringOrRes
import com.nabla.sdk.core.domain.entity.StringOrRes.Res
import com.nabla.sdk.core.domain.entity.asStringOrRes
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.emitIn
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.asNetworkOrGeneric
import com.nabla.sdk.scheduling.PaymentActivityContract
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.SCHEDULING_DOMAIN
import com.nabla.sdk.scheduling.domain.entity.Address
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.domain.entity.AppointmentState
import com.nabla.sdk.scheduling.domain.entity.MissingPaymentStep
import com.nabla.sdk.scheduling.domain.entity.PendingAppointment
import com.nabla.sdk.scheduling.domain.entity.Price
import com.nabla.sdk.scheduling.domain.entity.address
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

internal class AppointmentConfirmationViewModel(
    private val locationType: AppointmentLocationType,
    private val slot: Instant,
    private val pendingAppointmentId: AppointmentId,
    private val paymentStepRegistered: Boolean,
    private val nablaClient: NablaClient,
    private val handle: SavedStateHandle,
) : ViewModel() {
    private val retryTriggerFlow = MutableSharedFlow<Unit>()

    private val schedulingClient get() = nablaClient.schedulingInternalModule

    private val consentsGrantedFlow = handle.getStateFlow<Array<Boolean>>(CONSENTS_CHECKED_ARRAY_KEY, emptyArray())

    private var consentsGranted: Map<Int, Boolean>
        get() = consentsGrantedFlow.value
            .mapIndexed { index, value -> index to value }.toMap()
            .withDefault { false }
        set(value) {
            if (value != consentsGranted) {
                handle[CONSENTS_CHECKED_ARRAY_KEY] =
                    Array(value.keys.maxOrNull()?.plus(1) ?: 0) { value[it] ?: false }
            }
        }

    private var isLoadingFlow = MutableStateFlow(false)

    private val eventsMutableFlow = MutableLiveFlow<Event>()
    val eventsFlow: LiveFlow<Event> = eventsMutableFlow

    val stateFlow: StateFlow<State> = combine(
        flow { emit(nablaClient.schedulingInternalModule.getAppointmentConfirmationConsents(locationType).getOrThrow()) },
        flow { emit(nablaClient.schedulingInternalModule.getAppointment(pendingAppointmentId).getOrThrow()) },
        consentsGrantedFlow,
        isLoadingFlow,
    ) { consents, appointment, grants, isLoading ->
        if (isLoading) State.Loading else {
            State.Loaded(
                appointment.provider,
                slot,
                appointment.location.address,
                consents.htmlConsents.mapIndexed { index, consent ->
                    consent to grants.getOrElse(index) { false }
                },
            )
        }
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
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    val canSubmitFlow = combine(
        consentsGrantedFlow,
        stateFlow,
    ) { consents, state ->
        consents.all { it } && state is State.Loaded
    }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    fun onCtaClicked() {
        if (!canSubmitFlow.value) return

        viewModelScope.launch {
            isLoadingFlow.value = true
            schedulingClient.getAppointment(pendingAppointmentId)
                .onFailure { throwable ->
                    logAndShowErrorMessage(throwable, "failed fetching pending appointment")
                    isLoadingFlow.value = false
                }.onSuccess { appointment ->
                    logDebug("pending appointment fetched, starting payment if necessary.")
                    startPaymentIfNecessary(appointment)
                    isLoadingFlow.value = false
                }
        }
    }

    private fun startPaymentIfNecessary(appointment: Appointment) {
        if (appointment.state is AppointmentState.Pending && appointment.state.requiredPrice != null) {
            logDebug("Pending appointment requiring payment")
            if (paymentStepRegistered) {
                logDebug("Triggering payment step")
                eventsMutableFlow.emitIn(viewModelScope, Event.StartPayment(appointment.asPendingAppointment(appointment.state.requiredPrice)))
            } else {
                viewModelScope.launch {
                    logAndShowErrorMessage(MissingPaymentStep(), "failed fetching pending appointment")
                }
            }
        } else {
            logDebug("Appointment not pending or has no requirements. state: ${appointment.state}")
            schedulePendingAppointment(appointment.id)
        }
    }

    fun onPaymentFinished(result: PaymentActivityContract.Result) {
        when (result) {
            PaymentActivityContract.Result.Succeeded -> {
                schedulePendingAppointment(pendingAppointmentId)
            }
            PaymentActivityContract.Result.ShouldRetry -> {
                eventsMutableFlow.emitIn(viewModelScope, Event.ShowMessage(Res(R.string.nabla_scheduling_payment_error)))
                nablaClient.coreContainer.logger.info("Payment failed/cancelled — user can try again", domain = Logger.SCHEDULING_DOMAIN.UI)
            }
        }
    }

    private fun schedulePendingAppointment(pendingAppointmentId: AppointmentId) {
        logDebug("confirming the pending appointment.")
        viewModelScope.launch {
            isLoadingFlow.value = true
            schedulingClient.schedulePendingAppointment(pendingAppointmentId)
                .onFailure { throwable ->
                    logAndShowErrorMessage(throwable, "failed scheduling appointment")
                    isLoadingFlow.value = false
                }.onSuccess {
                    eventsMutableFlow.emit(Event.Finish)
                    logDebug("pending appointment confirmed by Nabla's backend, finishing.")
                }
        }
    }

    private suspend fun logAndShowErrorMessage(throwable: Throwable, logMessage: String) {
        nablaClient.coreContainer.logger.warn(logMessage, throwable, domain = Logger.SCHEDULING_DOMAIN.UI)
        eventsMutableFlow.emit(
            Event.ShowMessage(
                if (throwable is ServerException) {
                    throwable.serverMessage.asStringOrRes()
                } else {
                    throwable.asNetworkOrGeneric.title
                }
            )
        )
    }

    fun onClickRetry() {
        retryTriggerFlow.emitIn(viewModelScope, Unit)
    }

    fun onConsentChecked(index: Int, checked: Boolean) {
        consentsGranted = consentsGranted + (index to checked)
    }

    private fun logDebug(message: String) {
        nablaClient.coreContainer.logger.debug("AppointmentConfirmationViewModel $message", domain = Logger.SCHEDULING_DOMAIN.UI)
    }

    private fun Appointment.asPendingAppointment(price: Price) = PendingAppointment(
        id = id,
        provider = provider,
        scheduledAt = scheduledAt,
        location = location,
        price = price,
    )

    sealed interface State {
        object Loading : State
        data class Error(val errorUiModel: ErrorUiModel) : State
        data class Loaded(
            val provider: Provider,
            val slot: Instant,
            val address: Address?,
            val htmlConsents: List<Pair<String, Boolean>>,
        ) : State
    }

    sealed interface Event {
        object Finish : Event
        data class ShowMessage(val message: StringOrRes) : Event
        data class StartPayment(val pendingAppointment: PendingAppointment) : Event
    }

    companion object {
        private const val CONSENTS_CHECKED_ARRAY_KEY = "consents_checked_array_key"
    }
}

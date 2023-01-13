package com.nabla.sdk.scheduling.scene.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.StringOrRes
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.emitIn
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.asNetworkOrGeneric
import com.nabla.sdk.scheduling.SCHEDULING_DOMAIN
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
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

internal class AppointmentDetailsViewModel(
    private val appointmentId: AppointmentId,
    private val nablaClient: NablaClient,
) : ViewModel() {
    private val retryTriggerFlow = MutableSharedFlow<Unit>()

    private val eventsMutableFlow = MutableLiveFlow<Event>()
    private val isCancellingFlow = MutableStateFlow(false)

    val eventsFlow: LiveFlow<Event> = eventsMutableFlow
    val stateFlow: StateFlow<State> = flow<State> {
        val appointment =
            nablaClient.schedulingInternalModule.getAppointment(appointmentId).getOrThrow()
        emit(State.Loaded(appointment))
    }.combine(isCancellingFlow) { state, isCancelling ->
        if (isCancelling) State.Loading(state.appointment) else state
    }.retryWhen { cause, _ ->
        nablaClient.coreContainer.logger.warn(
            message = "failed to get appointment details",
            error = cause,
            domain = Logger.SCHEDULING_DOMAIN.UI
        )
        emit(State.Error(cause.asNetworkOrGeneric))
        retryTriggerFlow.first()
        emit(State.Loading(null))
        true
    }.stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading(null))

    fun onClickRetry() {
        retryTriggerFlow.emitIn(viewModelScope, Unit)
    }

    fun onCancelClicked() {
        viewModelScope.launch {
            isCancellingFlow.value = true
            nablaClient.schedulingInternalModule.cancelAppointment(appointmentId)
                .onFailure { throwable ->
                    nablaClient.coreContainer.logger.warn(
                        "failed cancelling appointment",
                        throwable,
                        domain = Logger.SCHEDULING_DOMAIN.UI
                    )

                    eventsMutableFlow.emit(
                        Event.FailedCancelling(
                            throwable.asNetworkOrGeneric.title
                        )
                    )
                }.onSuccess {
                    eventsMutableFlow.emit(Event.AppointmentCancelled)
                }
            isCancellingFlow.value = false
        }
    }

    sealed interface State {
        val appointment: Appointment?

        data class Loading(override val appointment: Appointment?) : State
        data class Error(val errorUiModel: ErrorUiModel) : State {
            override val appointment: Appointment? = null
        }

        data class Loaded(
            override val appointment: Appointment,
        ) : State
    }

    sealed class Event {
        data class FailedCancelling(val message: StringOrRes) : Event()
        object AppointmentCancelled : Event()
    }
}

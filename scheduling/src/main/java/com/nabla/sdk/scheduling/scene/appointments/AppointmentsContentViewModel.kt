package com.nabla.sdk.scheduling.scene.appointments

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.LivekitRoom
import com.nabla.sdk.core.domain.entity.LivekitRoomStatus
import com.nabla.sdk.core.domain.entity.ServerException
import com.nabla.sdk.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.emitIn
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.asNetworkOrGeneric
import com.nabla.sdk.scheduling.SCHEDULING_DOMAIN
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.schedulingInternalModule
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

internal class AppointmentsContentViewModel(
    private val nablaClient: NablaClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val appointmentType: AppointmentType = savedStateHandle.get(APPOINTMENT_TYPE_ARG) ?: error("No appointment type specified")

    private val schedulingClient = nablaClient.schedulingInternalModule

    private val retryTriggerFlow = MutableSharedFlow<Unit>()

    private var loadMoreCallback: (suspend () -> Result<Unit>)? = null

    private val eventsMutableFlow = MutableLiveFlow<Event>()
    val eventsFlow: LiveFlow<Event> = eventsMutableFlow

    private val currentCallFlow = nablaClient.coreContainer.videoCallModule?.watchCurrentVideoCall() ?: flowOf(null)

    internal val stateFlow: StateFlow<State> = when (appointmentType) {
        AppointmentType.PAST -> schedulingClient.watchPastAppointments()
        AppointmentType.UPCOMING -> schedulingClient.watchUpcomingAppointments()
    }
        .reTriggerWhenNextAppointmentIsSoon()
        .combine(currentCallFlow) { appointments, currentCall ->
            loadMoreCallback = appointments.loadMore
            val items = appointments.content.map { it.toUiModel(currentCall?.id) }

            if (items.isEmpty()) {
                State.Empty(appointmentType.emptyStateRes)
            } else State.Loaded(items)
        }
        .retryWhen { cause, _ ->
            nablaClient.coreContainer.logger.warn(
                domain = Logger.SCHEDULING_DOMAIN.UI,
                message = "Failed to fetch $appointmentType appointments list",
                error = cause,
            )

            emit(State.Error(cause.asNetworkOrGeneric))

            retryTriggerFlow.first()
            emit(State.Loading)
            true
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, State.Loading)

    fun onCancelClicked(upcoming: ItemUiModel.AppointmentUiModel.Upcoming) {
        viewModelScope.launch {
            nablaClient.schedulingInternalModule.cancelAppointment(upcoming.id)
                .onFailure { throwable ->
                    nablaClient.coreContainer.logger.warn("failed cancelling appointment", throwable, domain = Logger.SCHEDULING_DOMAIN.UI)

                    eventsMutableFlow.emit(
                        Event.FailedCancelling(
                            if (throwable is ServerException) {
                                throwable.serverMessage
                            } else {
                                nablaClient.coreContainer.stringResolver.resolve(throwable.asNetworkOrGeneric.titleRes)
                            }
                        )
                    )
                }
        }
    }

    fun onJoinClicked(room: LivekitRoom, roomStatus: LivekitRoomStatus.Open) {
        val videoCallModule = nablaClient.coreContainer.videoCallModule ?: run {
            nablaClient.coreContainer.logger.error("You need to add the video-call module to be able to join a call.", domain = Logger.SCHEDULING_DOMAIN.UI)
            return
        }

        videoCallModule.openVideoCall(
            nablaClient.coreContainer.configuration.context,
            roomStatus.url,
            room.id.toString(),
            roomStatus.token,
        )
    }

    fun onRetryClicked() {
        retryTriggerFlow.emitIn(viewModelScope, Unit)
    }

    fun onListReachedBottom() {
        viewModelScope.launch {
            loadMoreCallback?.invoke()?.onFailure { throwable ->
                nablaClient.coreContainer.logger.warn("failed loading more appointments", throwable, domain = Logger.SCHEDULING_DOMAIN.UI)
                eventsMutableFlow.emit(Event.FailedPagination)
            }
        }
    }

    internal sealed interface State {

        object Loading : State
        data class Empty(@StringRes val placeholderTextRes: Int) : State

        data class Error(
            val errorUiModel: ErrorUiModel,
        ) : State

        data class Loaded(
            val items: List<ItemUiModel>,
        ) : State
    }

    sealed interface Event {
        data class FailedCancelling(val errorMessage: String) : Event
        object FailedPagination : Event
    }

    companion object {
        const val APPOINTMENT_TYPE_ARG = "appointment_type_arg"
    }
}

/**
 * Emits immediately then re-emits each time an upcoming (but not soon) appointment becomes soon.
 */
private fun Flow<WatchPaginatedResponse<List<Appointment>>>.reTriggerWhenNextAppointmentIsSoon() = transformLatest { appointments ->
    emit(appointments)
    val notSoonAppointments = appointments.content
        .filterIsInstance<Appointment.Upcoming>()
        .filter { !isAppointmentSoon(it.scheduledAt) }
        .toMutableList()

    while (notSoonAppointments.isNotEmpty()) {
        val nextNotSoonAppointment = notSoonAppointments
            .minByOrNull { it.scheduledAt } ?: break

        notSoonAppointments.remove(nextNotSoonAppointment)
        delay(nextNotSoonAppointment.scheduledAt.minus(SOON_CONVERSATION_THRESHOLD).minus(Clock.System.now()))
        emit(appointments)
    }
}

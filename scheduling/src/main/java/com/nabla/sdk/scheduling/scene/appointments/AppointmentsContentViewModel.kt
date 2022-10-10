package com.nabla.sdk.scheduling.scene.appointments

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.Configuration
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.StringResolver
import com.nabla.sdk.core.domain.boundary.VideoCallModule
import com.nabla.sdk.core.domain.entity.ServerException
import com.nabla.sdk.core.domain.entity.VideoCallRoom
import com.nabla.sdk.core.domain.entity.VideoCallRoomStatus
import com.nabla.sdk.core.domain.entity.WatchPaginatedResponse
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.emitIn
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.asNetworkOrGeneric
import com.nabla.sdk.scheduling.SCHEDULING_DOMAIN
import com.nabla.sdk.scheduling.SchedulingInternalModule
import com.nabla.sdk.scheduling.domain.entity.Appointment
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
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

internal class AppointmentsContentViewModel(
    private val schedulingClient: SchedulingInternalModule,
    private val videoCallModule: VideoCallModule?,
    private val logger: Logger,
    private val configuration: Configuration,
    private val stringResolver: StringResolver,
    clock: Clock,
    delayCoroutineContext: CoroutineContext = EmptyCoroutineContext,
    private val appointmentType: AppointmentType,
) : ViewModel() {
    private val retryTriggerFlow = MutableSharedFlow<Unit>()

    private var loadMoreCallback: (suspend () -> Result<Unit>)? = null

    private val eventsMutableFlow = MutableLiveFlow<Event>()
    val eventsFlow: LiveFlow<Event> = eventsMutableFlow

    private val currentCallFlow = videoCallModule?.watchCurrentVideoCall() ?: flowOf(null)

    internal val stateFlow: StateFlow<State> = when (appointmentType) {
        AppointmentType.PAST -> schedulingClient.watchPastAppointments()
        AppointmentType.UPCOMING -> schedulingClient.watchUpcomingAppointments()
    }
        .reTriggerWhenNextAppointmentIsSoon(clock, delayCoroutineContext)
        .combine(currentCallFlow) { appointments, currentCall ->
            loadMoreCallback = appointments.loadMore
            val items = appointments.content.map { it.toUiModel(clock, currentCall?.id) }

            if (items.isEmpty()) {
                State.Empty(appointmentType.emptyStateRes)
            } else State.Loaded(items)
        }
        .retryWhen { cause, _ ->
            logger.warn(
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
            schedulingClient.cancelAppointment(upcoming.id)
                .onFailure { throwable ->
                    logger.warn("failed cancelling appointment", throwable, domain = Logger.SCHEDULING_DOMAIN.UI)

                    eventsMutableFlow.emit(
                        Event.FailedCancelling(
                            if (throwable is ServerException) {
                                throwable.serverMessage
                            } else {
                                stringResolver.resolve(throwable.asNetworkOrGeneric.titleRes)
                            }
                        )
                    )
                }
        }
    }

    fun onJoinClicked(room: VideoCallRoom, roomStatus: VideoCallRoomStatus.Open) {
        val videoCallModule = videoCallModule ?: run {
            logger.error("You need to add the video-call module to be able to join a call.", domain = Logger.SCHEDULING_DOMAIN.UI)
            return
        }

        videoCallModule.openVideoCall(
            configuration.context,
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
                logger.warn("failed loading more appointments", throwable, domain = Logger.SCHEDULING_DOMAIN.UI)
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
private fun Flow<WatchPaginatedResponse<List<Appointment>>>.reTriggerWhenNextAppointmentIsSoon(
    clock: Clock,
    delayCoroutineContext: CoroutineContext,
) = transformLatest { appointments ->
    emit(appointments)
    val notSoonAppointments = appointments.content
        .filterIsInstance<Appointment.Upcoming>()
        .filter { !clock.isAppointmentSoon(it.scheduledAt) }
        .toMutableList()

    while (notSoonAppointments.isNotEmpty()) {
        val nextNotSoonAppointment = notSoonAppointments
            .minByOrNull { it.scheduledAt } ?: break

        notSoonAppointments.remove(nextNotSoonAppointment)
        withContext(delayCoroutineContext) {
            delay(nextNotSoonAppointment.scheduledAt - SOON_CONVERSATION_THRESHOLD - clock.now())
        }
        emit(appointments)
    }
}

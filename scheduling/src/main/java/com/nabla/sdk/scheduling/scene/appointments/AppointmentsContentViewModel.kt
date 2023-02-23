package com.nabla.sdk.scheduling.scene.appointments

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.Configuration
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.VideoCallInternalClient
import com.nabla.sdk.core.domain.entity.PaginatedContent
import com.nabla.sdk.core.domain.entity.Response
import com.nabla.sdk.core.domain.entity.ServerException
import com.nabla.sdk.core.domain.entity.StringOrRes
import com.nabla.sdk.core.domain.entity.StringOrRes.Companion.asStringOrRes
import com.nabla.sdk.core.domain.entity.VideoCallRoom
import com.nabla.sdk.core.domain.entity.VideoCallRoomStatus
import com.nabla.sdk.core.ui.helpers.FlowCollectorExtension.emitIn
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.ErrorUiModel.Companion.asNetworkOrGeneric
import com.nabla.sdk.scheduling.SCHEDULING_DOMAIN
import com.nabla.sdk.scheduling.SchedulingPrivateClient
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentState
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
    private val schedulingPrivateClient: SchedulingPrivateClient,
    private val videoCallInternalClient: VideoCallInternalClient?,
    private val logger: Logger,
    private val configuration: Configuration,
    clock: Clock,
    delayCoroutineContext: CoroutineContext = EmptyCoroutineContext,
    private val appointmentType: AppointmentType,
) : ViewModel() {
    private val retryTriggerFlow = MutableSharedFlow<Unit>()

    private var loadMoreCallback: (suspend () -> Result<Unit>)? = null

    private val eventsMutableFlow = MutableLiveFlow<Event>()
    val eventsFlow: LiveFlow<Event> = eventsMutableFlow

    private val currentCallFlow = videoCallInternalClient?.watchCurrentVideoCall() ?: flowOf(null)

    internal val stateFlow: StateFlow<State> = when (appointmentType) {
        AppointmentType.PAST -> schedulingPrivateClient.watchPastAppointments()
        AppointmentType.UPCOMING -> schedulingPrivateClient.watchUpcomingAppointments()
    }
        .reTriggerWhenNextAppointmentIsSoon(clock, delayCoroutineContext)
        .combine(currentCallFlow) { response, currentCall ->
            loadMoreCallback = response.data.loadMore
            val items = response.data.content.map { it.toUiModel(clock, currentCall?.id) }

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
            schedulingPrivateClient.cancelAppointment(upcoming.id)
                .onFailure { throwable ->
                    logger.warn("failed cancelling appointment", throwable, domain = Logger.SCHEDULING_DOMAIN.UI)

                    eventsMutableFlow.emit(
                        Event.FailedCancelling(
                            if (throwable is ServerException) {
                                throwable.serverMessage.asStringOrRes()
                            } else {
                                throwable.asNetworkOrGeneric.title
                            }
                        )
                    )
                }
        }
    }

    fun onJoinClicked(room: VideoCallRoom, roomStatus: VideoCallRoomStatus.Open) {
        if (videoCallInternalClient == null) {
            logger.error("You need to add the video-call module to be able to join a call.", domain = Logger.SCHEDULING_DOMAIN.UI)
            return
        }
        videoCallInternalClient.openVideoCall(
            configuration.context,
            roomStatus.url,
            room.id.toString(),
            roomStatus.token,
        )
    }

    fun onRetryClicked() {
        retryTriggerFlow.emitIn(viewModelScope, Unit)
    }

    fun onFailedToOpenExternalLink(throwable: Throwable) {
        logger.error("Failed to open external video call link", throwable, domain = Logger.SCHEDULING_DOMAIN.UI)
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
        data class FailedCancelling(val errorMessage: StringOrRes) : Event
        object FailedPagination : Event
    }

    companion object {
        const val APPOINTMENT_TYPE_ARG = "appointment_type_arg"
    }
}

/**
 * Emits immediately then re-emits each time an upcoming (but not soon) appointment becomes soon.
 */
private fun Flow<Response<PaginatedContent<List<Appointment>>>>.reTriggerWhenNextAppointmentIsSoon(
    clock: Clock,
    delayCoroutineContext: CoroutineContext,
) = transformLatest { response ->
    emit(response)
    val notSoonAppointments = response.data.content
        .filter { it.state == AppointmentState.Upcoming }
        .filter { !clock.isAppointmentSoon(it.scheduledAt) }
        .toMutableList()

    while (notSoonAppointments.isNotEmpty()) {
        val nextNotSoonAppointment = notSoonAppointments
            .minByOrNull { it.scheduledAt } ?: break

        notSoonAppointments.remove(nextNotSoonAppointment)
        withContext(delayCoroutineContext) {
            delay(nextNotSoonAppointment.scheduledAt - SOON_CONVERSATION_THRESHOLD - clock.now())
        }
        emit(response)
    }
}

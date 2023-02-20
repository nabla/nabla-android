package com.nabla.sdk.scheduling.scene.slots

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.PaginatedContent
import com.nabla.sdk.core.domain.entity.ServerException
import com.nabla.sdk.core.domain.entity.StringOrRes
import com.nabla.sdk.core.domain.entity.StringOrRes.Res
import com.nabla.sdk.core.domain.entity.asStringOrRes
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.emitIn
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.asNetworkOrGeneric
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.SCHEDULING_DOMAIN
import com.nabla.sdk.scheduling.SchedulingPrivateClient
import com.nabla.sdk.scheduling.domain.entity.Address
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategoryId
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlotLocation
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

internal class TimeSlotsViewModel(
    private val locationType: AppointmentLocationType,
    private val categoryId: AppointmentCategoryId,
    private val schedulingPrivateClient: SchedulingPrivateClient,
    private val logger: Logger,
    private val handle: SavedStateHandle,
) : ViewModel() {
    private var latestLoadMoreCallback: (suspend () -> Result<Unit>)? = null
    private var lastReceivedSlots: List<AvailabilitySlot>? = null

    private var pendingAppointmentId: AppointmentId?
        get() = handle.get<String>(PENDING_APPOINTMENT_UUID)?.let { AppointmentId(Uuid.fromString(it)) }
        set(value) = handle.set(PENDING_APPOINTMENT_UUID, value?.uuid?.toString())

    private var pendingAppointmentStartAt: Instant?
        get() = handle.get<Long>(PENDING_APPOINTMENT_INSTANT)?.let { Instant.fromEpochMilliseconds(it) }
        set(value) = handle.set(PENDING_APPOINTMENT_INSTANT, value?.toEpochMilliseconds())

    private val retryTriggerFlow = MutableSharedFlow<Unit>()

    private val expandedDaysPositionsFlow = MutableStateFlow(setOf(0))
    private val selectedSlotFlow = MutableStateFlow<Instant?>(null)
    private val isSubmittingFlow = MutableStateFlow(false)

    private val eventsMutableFlow = MutableLiveFlow<Event>()
    val eventsFlow: LiveFlow<Event> = eventsMutableFlow

    val stateFlow: StateFlow<State> = combine(
        schedulingPrivateClient.watchAvailabilitySlots(locationType, categoryId).onEach { cacheLastResponse(it) },
        expandedDaysPositionsFlow,
        selectedSlotFlow,
        isSubmittingFlow,
        ::mapToLoadedState,
    ).retryWhen { cause, _ ->
        logger.warn(
            message = "failed to get availability slots",
            error = cause,
            domain = Logger.SCHEDULING_DOMAIN.UI,
        )
        emit(State.Error(cause.asNetworkOrGeneric))
        retryTriggerFlow.first()
        emit(State.Loading)
        true
    }.stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = State.Loading)

    private fun cacheLastResponse(response: PaginatedContent<List<AvailabilitySlot>>) {
        latestLoadMoreCallback = response.loadMore
        lastReceivedSlots = response.content
    }

    private fun mapToLoadedState(
        availabilitySlots: PaginatedContent<List<AvailabilitySlot>>,
        expandedDaysPositions: Set<Int>,
        selectedSlot: Instant?,
        isSubmitting: Boolean,
    ): State {
        if (isSubmitting) return State.Loading

        val uiItems = mutableListOf<TimeSlotsUiItem>()

        uiItems.addAll(
            availabilitySlots.content
                .groupBy { it.startAt.toLocalDateTime(TimeZone.currentSystemDefault()).date }
                .map { (day, slots) -> day to slots }
                .mapIndexed { index, value ->
                    val (day, slots) = value
                    TimeSlotsUiItem.DaySlots(
                        localDate = day,
                        expansionState = if (index in expandedDaysPositions) {
                            TimeSlotsUiItem.DaySlots.ExpansionState.Expanded(
                                slots = slots.map { TimeSlotsUiItem.DaySlots.Slot(it.startAt, isSelected = it.startAt == selectedSlot) },
                            )
                        } else {
                            TimeSlotsUiItem.DaySlots.ExpansionState.Collapsed(slotsCount = slots.size)
                        }
                    )
                }
        )

        if (availabilitySlots.loadMore != null) {
            uiItems.add(TimeSlotsUiItem.Loading)
        }
        return if (uiItems.isEmpty()) {
            State.Empty
        } else State.Loaded(uiItems, canSubmit = selectedSlot != null)
    }

    fun onClickRetry() {
        retryTriggerFlow.emitIn(viewModelScope, Unit)
    }

    fun onDaySlotsClicked(position: Int) {
        toggleDayExpansion(position)
    }

    private fun toggleDayExpansion(position: Int) {
        val current = expandedDaysPositionsFlow.value
        expandedDaysPositionsFlow.value = if (current.contains(position)) {
            current - position
        } else {
            current + position
        }
    }

    fun onSlotClicked(slotTime: Instant) {
        if (!selectedSlotFlow.compareAndSet(slotTime, null)) {
            selectedSlotFlow.value = slotTime
        }
    }

    fun onListReachedBottom() {
        val loadMore = latestLoadMoreCallback ?: return

        viewModelScope.launch {
            loadMore()
                .onFailure { error ->
                    logger.warn(
                        domain = Logger.SCHEDULING_DOMAIN.UI,
                        message = "Error while loading more availability slots",
                        error = error,
                    )
                    eventsMutableFlow.emit(Event.ShowMessage(Res(R.string.nabla_scheduling_time_slot_pagination_error)))
                }
        }
    }

    fun onConfirmClicked() {
        val selectedSlotAsInstant = selectedSlotFlow.value ?: return
        val selectedSlot = lastReceivedSlots?.firstOrNull { it.startAt == selectedSlotAsInstant } ?: return
        val providerId = selectedSlot.providerId
        val address = (selectedSlot.location as? AvailabilitySlotLocation.Physical)?.address
        logDebug("confirm clicked with slot: $selectedSlotAsInstant")

        viewModelScope.launch {
            isSubmittingFlow.value = true
            runCatchingCancellable {
                val appointmentId = pendingAppointmentId
                    ?.also { logDebug("not null pendingAppointmentId") }
                    ?.takeIf { (pendingAppointmentStartAt == selectedSlotAsInstant) }
                    ?.also { logDebug("re-using the pendingAppointmentId because same slot") }
                    ?: schedulingPrivateClient.createPendingAppointment(locationType, categoryId, providerId, selectedSlot.startAt)
                        .onSuccess {
                            logDebug("created new pending appointment")
                            pendingAppointmentId = it.id
                            pendingAppointmentStartAt = it.scheduledAt
                        }
                        .getOrThrow().id

                logDebug("opening confirmation screen for pending appointment $pendingAppointmentId")
                eventsMutableFlow.emitIn(
                    viewModelScope,
                    Event.GoToConfirmation(
                        locationType = locationType,
                        providerId = providerId,
                        slot = selectedSlot.startAt,
                        address = address,
                        pendingAppointmentId = appointmentId,
                    )
                )
            }.onFailure { logAndShowErrorMessage(it, "failed creating/reusing pending appointment to open confirmation") }
            isSubmittingFlow.value = false
        }
    }

    private fun logAndShowErrorMessage(throwable: Throwable, logMessage: String) {
        logger.warn(logMessage, throwable, domain = Logger.SCHEDULING_DOMAIN.UI)
        eventsMutableFlow.emitIn(
            viewModelScope,
            Event.ShowMessage(
                if (throwable is ServerException) {
                    throwable.serverMessage.asStringOrRes()
                } else throwable.asNetworkOrGeneric.title
            )
        )
    }

    private fun logDebug(message: String) {
        logger.debug("TimeSlotsViewModel $message", domain = Logger.SCHEDULING_DOMAIN.UI)
    }

    sealed interface State {
        object Loading : State
        object Empty : State
        data class Error(val errorUiModel: ErrorUiModel) : State
        data class Loaded(val items: List<TimeSlotsUiItem>, val canSubmit: Boolean) : State
    }

    sealed interface Event {
        data class GoToConfirmation(
            val locationType: AppointmentLocationType,
            val providerId: Uuid,
            val slot: Instant,
            val address: Address?,
            val pendingAppointmentId: AppointmentId,
        ) : Event

        data class ShowMessage(val message: StringOrRes) : Event
    }

    companion object {
        private const val PENDING_APPOINTMENT_UUID = "PENDING_APPOINTMENT_UUID"
        private const val PENDING_APPOINTMENT_INSTANT = "PENDING_APPOINTMENT_INSTANT"
    }
}

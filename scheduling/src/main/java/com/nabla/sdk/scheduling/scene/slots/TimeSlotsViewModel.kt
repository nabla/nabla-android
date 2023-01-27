package com.nabla.sdk.scheduling.scene.slots

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.PaginatedContent
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.emitIn
import com.nabla.sdk.core.ui.model.ErrorUiModel
import com.nabla.sdk.core.ui.model.asNetworkOrGeneric
import com.nabla.sdk.scheduling.R
import com.nabla.sdk.scheduling.SCHEDULING_DOMAIN
import com.nabla.sdk.scheduling.SchedulingInternalModule
import com.nabla.sdk.scheduling.domain.entity.Address
import com.nabla.sdk.scheduling.domain.entity.AppointmentCategoryId
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
    private val schedulingModule: SchedulingInternalModule,
    private val logger: Logger,
) : ViewModel() {
    private var latestLoadMoreCallback: (suspend () -> Result<Unit>)? = null
    private var lastReceivedSlots: List<AvailabilitySlot>? = null

    private val retryTriggerFlow = MutableSharedFlow<Unit>()

    private val expandedDaysPositionsFlow = MutableStateFlow(setOf(0))
    private val selectedSlotFlow = MutableStateFlow<Instant?>(null)

    private val eventsMutableFlow = MutableLiveFlow<Event>()
    val eventsFlow: LiveFlow<Event> = eventsMutableFlow

    val stateFlow: StateFlow<State> = combine(
        schedulingModule.watchAvailabilitySlots(locationType, categoryId).onEach { cacheLastResponse(it) },
        expandedDaysPositionsFlow,
        selectedSlotFlow,
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
    ): State {
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
                    eventsMutableFlow.emit(Event.ErrorAlert.Pagination)
                }
        }
    }

    fun onConfirmClicked() {
        val selectedSlotAsInstant = selectedSlotFlow.value ?: return
        val selectedSlot = lastReceivedSlots?.firstOrNull { it.startAt == selectedSlotAsInstant } ?: return
        val providerId = selectedSlot.providerId
        val address = (selectedSlot.location as? AvailabilitySlotLocation.Physical)?.address

        eventsMutableFlow.emitIn(
            viewModelScope,
            Event.GoToConfirmation(
                locationType,
                categoryId,
                providerId,
                selectedSlot.startAt,
                address,
            )
        )
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
            val categoryId: AppointmentCategoryId,
            val providerId: Uuid,
            val slot: Instant,
            val address: Address?,
        ) : Event

        sealed class ErrorAlert(@StringRes val errorMessageRes: Int) : Event {
            object Pagination : ErrorAlert(R.string.nabla_scheduling_time_slot_pagination_error)
        }
    }
}

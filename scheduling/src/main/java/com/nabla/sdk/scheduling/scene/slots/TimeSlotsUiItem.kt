package com.nabla.sdk.scheduling.scene.slots

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

internal sealed class TimeSlotsUiItem {
    abstract val listId: String

    data class DaySlots(
        val localDate: LocalDate,
        val expansionState: ExpansionState,
    ) : TimeSlotsUiItem() {
        sealed interface ExpansionState {
            data class Collapsed(val slotsCount: Int) : ExpansionState
            data class Expanded(val slots: List<Slot>) : ExpansionState
        }

        data class Slot(
            val startAt: Instant,
            val isSelected: Boolean,
        ) {
            companion object
        }

        override val listId: String = "availabilities-per-day-$localDate"

        companion object
    }

    object Loading : TimeSlotsUiItem() {
        override val listId: String = "loading"
    }
}

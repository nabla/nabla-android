package com.nabla.sdk.scheduling.scene.slots

import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate

internal sealed class TimeSlotsUiItem {
    abstract val listId: String

    data class DaySlots(
        val localDate: LocalDate,
        val isExpanded: Boolean,
        val slots: List<Slot>,
    ) : TimeSlotsUiItem() {
        data class Slot(
            val startAt: Instant,
            val isSelected: Boolean,
        )

        override val listId: String = "availabilities-per-day-$localDate"
    }

    object Loading : TimeSlotsUiItem() {
        override val listId: String = "loading"
    }
}

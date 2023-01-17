package com.nabla.sdk.scheduling.domain.entity

import com.benasher44.uuid.Uuid
import kotlinx.datetime.Instant

internal data class AvailabilitySlot(
    val startAt: Instant,
    val providerId: Uuid,
    val location: AvailabilitySlotLocation,
) {
    companion object
}

internal sealed class AvailabilitySlotLocation {
    object Remote : AvailabilitySlotLocation()

    data class Physical(val address: Address) : AvailabilitySlotLocation()

    object Unknown : AvailabilitySlotLocation()

    companion object
}

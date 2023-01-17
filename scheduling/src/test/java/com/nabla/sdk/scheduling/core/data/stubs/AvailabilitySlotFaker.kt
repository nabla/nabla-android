package com.nabla.sdk.scheduling.core.data.stubs

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.scheduling.domain.entity.Address
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlotLocation
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal fun AvailabilitySlot.Companion.fake(
    providerId: Uuid = uuid4(),
    startAt: Instant = Clock.System.now(),
    location: AvailabilitySlotLocation = AvailabilitySlotLocation.fake(),
) = AvailabilitySlot(
    providerId = providerId,
    startAt = startAt,
    location = location,
)

internal fun AvailabilitySlotLocation.Companion.fake(): AvailabilitySlotLocation {
    return when (AppointmentLocationType.values().random()) {
        AppointmentLocationType.REMOTE -> AvailabilitySlotLocation.Remote
        AppointmentLocationType.PHYSICAL -> AvailabilitySlotLocation.Physical(Address.fake())
    }
}

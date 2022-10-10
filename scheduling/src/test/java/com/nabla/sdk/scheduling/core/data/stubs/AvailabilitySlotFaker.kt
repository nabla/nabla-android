package com.nabla.sdk.scheduling.core.data.stubs

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.scheduling.domain.entity.AvailabilitySlot
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal fun AvailabilitySlot.Companion.fake(
    providerId: Uuid = uuid4(),
    startAt: Instant = Clock.System.now(),
) = AvailabilitySlot(
    providerId = providerId,
    startAt = startAt,
)

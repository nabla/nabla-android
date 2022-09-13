package com.nabla.sdk.scheduling.domain.entity

import com.benasher44.uuid.Uuid
import kotlinx.datetime.Instant

internal data class AvailabilitySlot(
    val startAt: Instant,
    val providerId: Uuid,
)

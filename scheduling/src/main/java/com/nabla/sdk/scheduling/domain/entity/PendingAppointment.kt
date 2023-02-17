package com.nabla.sdk.scheduling.domain.entity

import com.nabla.sdk.core.domain.entity.Provider
import kotlinx.datetime.Instant

public data class PendingAppointment(
    val id: AppointmentId,
    val provider: Provider,
    val scheduledAt: Instant,
    val location: AppointmentLocation,
    val price: Price?,
) {
    public companion object
}

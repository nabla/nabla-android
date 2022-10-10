package com.nabla.sdk.scheduling.domain.entity

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.VideoCallRoom
import kotlinx.datetime.Instant

@JvmInline
public value class AppointmentId(public val uuid: Uuid)

internal sealed interface Appointment {
    val id: AppointmentId
    val provider: Provider
    val scheduledAt: Instant

    data class Upcoming(
        override val id: AppointmentId,
        override val provider: Provider,
        override val scheduledAt: Instant,
        val videoCallRoom: VideoCallRoom?,
    ) : Appointment {
        companion object
    }

    data class Finalized(
        override val id: AppointmentId,
        override val provider: Provider,
        override val scheduledAt: Instant,
    ) : Appointment {
        companion object
    }
}

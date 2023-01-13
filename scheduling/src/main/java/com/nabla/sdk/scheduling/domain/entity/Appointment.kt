package com.nabla.sdk.scheduling.domain.entity

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.VideoCallRoom
import kotlinx.datetime.Instant

@JvmInline
public value class AppointmentId(public val uuid: Uuid)

internal data class Appointment(
    val id: AppointmentId,
    val provider: Provider,
    val scheduledAt: Instant,
    val state: AppointmentState,
    val location: AppointmentLocation,
) {
    companion object
}
internal enum class AppointmentState {
    UPCOMING,
    FINALIZED,
}

internal sealed interface AppointmentLocation {
    val type: AppointmentLocationType
    val address: Address?
    data class Remote(val videoCallRoom: VideoCallRoom?) : AppointmentLocation {
        override val type = AppointmentLocationType.REMOTE
        override val address = null
    }

    data class Physical(override val address: Address) : AppointmentLocation {
        override val type = AppointmentLocationType.PHYSICAL
    }

    companion object
}

internal enum class AppointmentLocationType {
    REMOTE,
    PHYSICAL,
}

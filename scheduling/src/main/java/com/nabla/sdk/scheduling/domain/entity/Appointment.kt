package com.nabla.sdk.scheduling.domain.entity

import androidx.annotation.VisibleForTesting
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.Uri
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
internal sealed interface AppointmentState {
    object Upcoming : AppointmentState
    object Finalized : AppointmentState

    data class Pending(val requiredPrice: Price?) : AppointmentState
}

public sealed interface AppointmentLocation {
    public val type: AppointmentLocationType?

    public sealed class Remote : AppointmentLocation {
        override val type: AppointmentLocationType = AppointmentLocationType.REMOTE

        public data class External(val url: Uri) : Remote()
        public data class Nabla(val videoCallRoom: VideoCallRoom?) : Remote()
    }

    public data class Physical(val address: Address) : AppointmentLocation {
        override val type: AppointmentLocationType = AppointmentLocationType.PHYSICAL
    }

    public object Unknown : AppointmentLocation {
        override val type: AppointmentLocationType? = null
    }

    @VisibleForTesting
    public companion object
}

public enum class AppointmentLocationType {
    REMOTE,
    PHYSICAL,
}

internal val AppointmentLocation.address: Address?
    get() = (this as? AppointmentLocation.Physical)?.address

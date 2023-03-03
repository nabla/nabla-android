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
    val price: Price?,
) {
    companion object
}
internal sealed interface AppointmentState {
    object Upcoming : AppointmentState
    object Finalized : AppointmentState

    data class Pending(val requiredPrice: Price?) : AppointmentState
}

/**
 * Where an appointment is expected to happen.
 */
public sealed interface AppointmentLocation {
    public val type: AppointmentLocationType?

    /**
     * The appointment will be conducted remotely, e.g. video-call teleconsultation.
     */
    public sealed class Remote : AppointmentLocation {
        override val type: AppointmentLocationType = AppointmentLocationType.REMOTE

        /**
         * The video-call is expected to happen on an external video-consultation provider.
         *
         * @param url the link to join the call.
         */
        public data class External(val url: Uri) : Remote()

        /**
         * The video-call will be happen on Nabla's Servers using Nabla's Console or SDKs.
         */
        public data class Nabla(val videoCallRoom: VideoCallRoom?) : Remote()
    }

    /**
     * The appointment will be conducted in-person at the specified [address].
     */
    public data class Physical(val address: Address) : AppointmentLocation {
        override val type: AppointmentLocationType = AppointmentLocationType.PHYSICAL
    }

    /**
     * Server specified a location type not handled by the current SDK version.
     * If you get this then you probably need to upgrade the SDK version you're using.
     */
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

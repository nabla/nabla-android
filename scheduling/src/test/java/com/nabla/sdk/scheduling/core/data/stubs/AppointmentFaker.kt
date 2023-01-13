package com.nabla.sdk.scheduling.core.data.stubs

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.data.stubs.fake
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.VideoCallRoom
import com.nabla.sdk.core.domain.entity.VideoCallRoomStatus
import com.nabla.sdk.scheduling.domain.entity.Address
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocation
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocationType
import com.nabla.sdk.scheduling.domain.entity.AppointmentState
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.random.Random

internal fun Appointment.Companion.fake(
    id: AppointmentId = AppointmentId(uuid4()),
    provider: Provider = Provider.fake(),
    scheduledAt: Instant = Clock.System.now(),
    state: AppointmentState = AppointmentState.values().random(),
    location: AppointmentLocation = AppointmentLocation.fake(videoCallRoomIsOpen = state == AppointmentState.UPCOMING),
) = Appointment(
    id = id,
    provider = provider,
    scheduledAt = scheduledAt,
    state = state,
    location = location,
)

internal fun AppointmentLocation.Companion.fake(
    locationType: AppointmentLocationType = AppointmentLocationType.values().random(),
    videoCallRoomIsOpen: Boolean = Random.nextBoolean(),
): AppointmentLocation {
    return when (locationType) {
        AppointmentLocationType.REMOTE -> AppointmentLocation.Remote(VideoCallRoom.fake(status = VideoCallRoomStatus.fake(isOpen = videoCallRoomIsOpen)))
        AppointmentLocationType.PHYSICAL -> AppointmentLocation.Physical(Address.fake())
    }
}

internal fun Address.Companion.fake(): Address {
    return Address(
        address = "1234 Main St",
        zipCode = "Fake Zip Code",
        city = "Fake City",
        state = "Fake State",
        country = "Fake Country",
        extraDetails = "Fake Extra Details",
    )
}

internal fun VideoCallRoom.Companion.fake(
    id: Uuid = uuid4(),
    status: VideoCallRoomStatus = VideoCallRoomStatus.fake(),
) = VideoCallRoom(
    id,
    status,
)

internal fun VideoCallRoomStatus.Companion.fake(
    isOpen: Boolean = true,
) = if (isOpen) {
    VideoCallRoomStatus.Open("url", "token")
} else {
    VideoCallRoomStatus.Closed
}

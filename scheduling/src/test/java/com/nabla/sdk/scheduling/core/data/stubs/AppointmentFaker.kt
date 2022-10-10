package com.nabla.sdk.scheduling.core.data.stubs

import com.benasher44.uuid.Uuid
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.data.stubs.fake
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.VideoCallRoom
import com.nabla.sdk.core.domain.entity.VideoCallRoomStatus
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

internal fun Appointment.Upcoming.Companion.fake(
    id: AppointmentId = AppointmentId(uuid4()),
    provider: Provider = Provider.fake(),
    scheduledAt: Instant = Clock.System.now(),
    videoCallRoom: VideoCallRoom? = VideoCallRoom.fake(),
) = Appointment.Upcoming(
    id = id,
    provider = provider,
    scheduledAt = scheduledAt,
    videoCallRoom = videoCallRoom,
)

internal fun Appointment.Finalized.Companion.fake(
    id: AppointmentId = AppointmentId(uuid4()),
    provider: Provider = Provider.fake(),
    scheduledAt: Instant,
) = Appointment.Finalized(
    id = id,
    provider = provider,
    scheduledAt = scheduledAt,
)

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

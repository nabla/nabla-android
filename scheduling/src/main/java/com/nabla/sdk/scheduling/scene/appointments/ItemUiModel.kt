package com.nabla.sdk.scheduling.scene.appointments

import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.VideoCallRoom
import com.nabla.sdk.core.domain.entity.VideoCallRoomStatus
import com.nabla.sdk.scheduling.domain.entity.Appointment
import com.nabla.sdk.scheduling.domain.entity.AppointmentId
import com.nabla.sdk.scheduling.domain.entity.AppointmentLocation
import com.nabla.sdk.scheduling.domain.entity.AppointmentState
import com.nabla.sdk.scheduling.scene.appointments.ItemUiModel.AppointmentUiModel.SoonOrOngoing.CallButtonStatus
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.minutes

internal sealed class ItemUiModel(val listId: String) {

    sealed class AppointmentUiModel(listId: String) : ItemUiModel(listId) {
        abstract val id: AppointmentId
        abstract val provider: Provider
        abstract val scheduledAt: Instant

        data class Upcoming(
            override val id: AppointmentId,
            override val provider: Provider,
            override val scheduledAt: Instant,
        ) : AppointmentUiModel(id.toString())

        data class SoonOrOngoing(
            override val id: AppointmentId,
            override val provider: Provider,
            override val scheduledAt: Instant,
            val callButtonStatus: CallButtonStatus,
        ) : AppointmentUiModel(id.toString()) {
            sealed interface CallButtonStatus {
                object Absent : CallButtonStatus
                sealed interface Present : CallButtonStatus {
                    val videoCallRoom: VideoCallRoom

                    data class AsJoin(override val videoCallRoom: VideoCallRoom) : Present
                    data class AsGoBack(override val videoCallRoom: VideoCallRoom) : Present
                }
            }
        }

        data class Finalized(
            override val id: AppointmentId,
            override val provider: Provider,
            override val scheduledAt: Instant,
        ) : AppointmentUiModel(id.toString())
    }

    object Loading : ItemUiModel(listId = "loading_more")
}

internal fun Appointment.toUiModel(clock: Clock, currentCallId: Uuid?) = when (this.state) {
    AppointmentState.FINALIZED -> ItemUiModel.AppointmentUiModel.Finalized(
        id,
        provider,
        scheduledAt,
    )
    AppointmentState.UPCOMING -> if (clock.isAppointmentSoon(scheduledAt)) {
        ItemUiModel.AppointmentUiModel.SoonOrOngoing(
            id,
            provider,
            scheduledAt,
            callButtonStatus = when (location) {
                is AppointmentLocation.Physical -> CallButtonStatus.Absent
                is AppointmentLocation.Remote -> {
                    when (location.videoCallRoom?.status) {
                        is VideoCallRoomStatus.Open -> {
                            when (currentCallId) {
                                null -> CallButtonStatus.Present.AsJoin(location.videoCallRoom)
                                location.videoCallRoom.id -> CallButtonStatus.Present.AsGoBack(
                                    location.videoCallRoom
                                )
                                else -> {
                                    // there's a current call but it's not this one — don't show a join until the current call has ended.
                                    CallButtonStatus.Absent
                                }
                            }
                        }
                        is VideoCallRoomStatus.Closed, null -> {
                            CallButtonStatus.Absent
                        }
                    }
                }
            }
        )
    } else {
        ItemUiModel.AppointmentUiModel.Upcoming(
            id,
            provider,
            scheduledAt,
        )
    }
}

internal fun Clock.isAppointmentSoon(scheduledAt: Instant) = now() >= scheduledAt.minus(SOON_CONVERSATION_THRESHOLD)

internal val SOON_CONVERSATION_THRESHOLD = 10.minutes

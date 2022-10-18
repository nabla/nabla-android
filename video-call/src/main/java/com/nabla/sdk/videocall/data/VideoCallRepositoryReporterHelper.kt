package com.nabla.sdk.videocall.data

import com.nabla.sdk.core.domain.boundary.ErrorReporter
import com.nabla.sdk.videocall.VIDEO_CALL_DOMAIN
import io.livekit.android.events.RoomEvent
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.Track
import io.livekit.android.room.track.TrackPublication
import io.livekit.android.util.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

internal class VideoCallRepositoryReporterHelper(private val errorReporter: ErrorReporter) {
    fun installRoomReporter(scope: CoroutineScope, room: Room) {
        scope.launch {
            room::state.flow.collect { state: Room.State ->
                errorReporter.log(
                    "RoomState=${state.name}",
                    mapOf("roomdId" to (room.name ?: "null")),
                    ErrorReporter.VIDEO_CALL_DOMAIN
                )
            }
        }
        scope.launch {
            room.events.events.collect { event: RoomEvent ->
                errorReporter.log(
                    mapMessage(event),
                    mapOf("roomdId" to (room.name ?: "null")),
                    ErrorReporter.VIDEO_CALL_DOMAIN
                )
                when (event) {
                    is RoomEvent.Disconnected -> {
                        val error = event.error
                        if (error == null) {
                            errorReporter.reportEvent("Call ended without error")
                        } else {
                            errorReporter.reportException(error)
                        }
                    }
                    is RoomEvent.FailedToConnect -> {
                        errorReporter.reportException(event.error)
                    }
                    is RoomEvent.TrackSubscriptionFailed -> {
                        errorReporter.reportException(event.exception)
                    }
                    else -> { /*-*/ }
                }
            }
        }
    }

    private fun mapMessage(event: RoomEvent): String {
        val eventDesc = when (event) {
            is RoomEvent.Disconnected -> {
                "Disconnected"
            }
            is RoomEvent.FailedToConnect -> {
                "FailedToConnect"
            }
            is RoomEvent.TrackSubscriptionFailed -> {
                "TrackSubscriptionFailed(Participant=${event.participant.asLog()})"
            }
            is RoomEvent.ActiveSpeakersChanged -> "ActiveSpeakersChanged"
            is RoomEvent.ConnectionQualityChanged -> {
                "ConnectionQualityChanged(Participant=${event.participant.asLog()}, Quality=${event.quality.name})"
            }
            is RoomEvent.DataReceived -> "DataReceived"
            is RoomEvent.ParticipantConnected -> {
                "ParticipantConnected(Participant=${event.participant.asLog()})"
            }
            is RoomEvent.ParticipantDisconnected -> {
                "ParticipantDisconnected(Participant=${event.participant.asLog()})"
            }
            is RoomEvent.ParticipantMetadataChanged -> "ParticipantMetadataChanged"
            is RoomEvent.ParticipantPermissionsChanged -> "ParticipantPermissionsChanged"
            is RoomEvent.Reconnected -> "Reconnected"
            is RoomEvent.Reconnecting -> "Reconnecting"
            is RoomEvent.RoomMetadataChanged -> "RoomMetadataChanged"
            is RoomEvent.TrackMuted -> {
                "TrackMuted(Participant=${event.participant.asLog()}, Publication=${event.publication.asLog()})"
            }
            is RoomEvent.TrackPublished -> {
                "TrackPublished(Participant=${event.participant.asLog()}, Publication=${event.publication.asLog()})"
            }
            is RoomEvent.TrackStreamStateChanged -> {
                "TrackStreamStateChanged(Publication=${event.trackPublication.asLog()}, StreamState=${event.streamState.name})"
            }
            is RoomEvent.TrackSubscribed -> {
                "TrackSubscribed(Participant=${event.participant.asLog()}, Publication=${event.publication.asLog()})"
            }
            is RoomEvent.TrackSubscriptionPermissionChanged -> "TrackSubscriptionPermissionChanged"
            is RoomEvent.TrackUnmuted -> {
                "TrackUnmuted(Participant=${event.participant.asLog()}, Publication=${event.publication.asLog()})"
            }
            is RoomEvent.TrackUnpublished -> {
                "TrackUnpublished(Participant=${event.participant.asLog()}, Publication=${event.publication.asLog()})"
            }
            is RoomEvent.TrackUnsubscribed -> {
                "TrackUnsubscribed(Participant=${event.participant.asLog()}, Track=${event.track.asLog()})"
            }
        }
        return "RoomEvent=$eventDesc"
    }

    private fun Participant.asLog(): String {
        return when (this) {
            is LocalParticipant -> "Self"
            is RemoteParticipant -> "Remote(id=$sid)"
            else -> "Unknown($sid)"
        }
    }
    private fun TrackPublication.asLog() = "TrackPublication(kind=$kind, id=$sid)"
    private fun Track.asLog(): String = "Track(kind=$kind, id=$sid)"
}

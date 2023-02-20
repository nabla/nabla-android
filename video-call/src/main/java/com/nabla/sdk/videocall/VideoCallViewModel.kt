package com.nabla.sdk.videocall

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.sdk.core.kotlin.runCatchingCancellable
import com.nabla.sdk.core.ui.helpers.LiveFlow
import com.nabla.sdk.core.ui.helpers.MutableLiveFlow
import com.nabla.sdk.core.ui.helpers.emitIn
import com.nabla.sdk.videocall.domain.CameraService
import io.livekit.android.events.RoomEvent
import io.livekit.android.events.collect
import io.livekit.android.room.Room
import io.livekit.android.room.participant.LocalParticipant
import io.livekit.android.room.participant.Participant
import io.livekit.android.room.participant.RemoteParticipant
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.LocalVideoTrack
import io.livekit.android.room.track.LocalVideoTrackOptions
import io.livekit.android.room.track.VideoTrack
import io.livekit.android.util.flow
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class VideoCallViewModel(
    private val videoCallPrivateClient: VideoCallPrivateClient,
    private val url: String,
    private val token: String,
    private val cameraService: CameraService,
) : ViewModel() {

    private val mutableLocalVideoTrackFlow = MutableStateFlow<LocalVideoTrack?>(null)
    private val mutableRemoteVideoTracksFlow = MutableStateFlow<List<VideoTrack>>(emptyList())
    private val mutableActiveSpeakersFlow = MutableStateFlow<List<Participant>>(emptyList())
    private val mutableRemoteParticipantsFlow = MutableStateFlow<Map<String, RemoteParticipant>>(emptyMap())
    private val mutableIsPictureInPictureFlow = MutableStateFlow(false)

    // this could have been avoided if tracks were observable, see https://github.com/livekit/client-sdk-android/issues/101
    private val mutableIsSelfMirrorFlow = MutableStateFlow(true)

    private val mutableFinishFlow = MutableLiveFlow<FinishReason>()
    internal val finishFlow: LiveFlow<FinishReason> = mutableFinishFlow

    private val connectRoomLock = Mutex()
    private val mutableConnectedRoomFlow = MutableStateFlow<Room?>(null)
    internal val connectedRoomFlow: StateFlow<Room?> = mutableConnectedRoomFlow

    private val mutableConnectionInfoFlow = MutableStateFlow<ConnectionInfoState?>(null)
    internal val connectionInfoFlow: StateFlow<ConnectionInfoState?> = mutableConnectionInfoFlow

    internal val participantsCountFlow: StateFlow<Int> = mutableRemoteParticipantsFlow
        .map { participants -> participants.size + 1 }
        .stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = 0)

    private val mutableControlsFlow = MutableStateFlow<ControlsState?>(null)
    internal val controlsFlow: Flow<ControlsState?> = mutableControlsFlow
        .combine(mutableIsPictureInPictureFlow) { controls, isPip ->
            if (isPip) controls?.copy(controlsVisible = false) else controls
        }

    private var flipJob: Job? = null
    private var toggleCameraJob: Job? = null

    private val mutableRemoteActiveSpeakerVideoTrackFlow = mutableRemoteVideoTracksFlow
        .combine(
            mutableActiveSpeakersFlow
                // We don't want empty lists (except at the very beginning) as we want to keep the last active speaker.
                .filter { it.isNotEmpty() }.onStart { emit(emptyList()) }
        ) { tracks, speakers ->
            tracks.lastOrNull { track ->
                // take last (in order of subscription) track belonging to any of the current speakers
                speakers.any { speaker -> speaker.videoTracks.any { (_, videoTrack) -> videoTrack != null && videoTrack.sid == track.sid } }
            }
                // we either:
                // 1. are at the very beginning with empty speakers list,
                // 2. we have some active speakers but they don't have video tracks anymore,
                // Then take the last subscribed track.
                ?: tracks.lastOrNull()
        }.distinctUntilChangedBy { it?.sid }

    internal val videoStateFlow = combine(
        mutableLocalVideoTrackFlow,
        mutableRemoteActiveSpeakerVideoTrackFlow,
        mutableIsSelfMirrorFlow,
        mutableIsPictureInPictureFlow,
        mutableControlsFlow.map { it?.cameraEnabled ?: true },
    ) { localTrack, activeRemoteTrack, isSelfMirror, pictureInPictureEnabled, cameraEnabled ->
        when (localTrack) {
            null -> when (activeRemoteTrack) {
                null -> VideoState.None
                else -> VideoState.RemoteOnly(activeRemoteTrack)
            }
            else -> when (activeRemoteTrack) {
                null -> if (cameraEnabled) {
                    VideoState.SelfOnly(localTrack, isSelfMirror)
                } else VideoState.None
                else -> if (cameraEnabled && !pictureInPictureEnabled) {
                    VideoState.Both(localTrack, activeRemoteTrack, isSelfMirror)
                } else VideoState.RemoteOnly(activeRemoteTrack)
            }
        }.also { videoCallPrivateClient.logger.debug("new video state: $it", domain = VIDEO_CALL_DOMAIN) }
    }
        .stateIn(viewModelScope, started = SharingStarted.Eagerly, initialValue = VideoState.None)

    fun onPermissionsGranted() {
        connectRoomIfNeededSynchronously()
    }

    private fun connectRoomIfNeededSynchronously() {
        viewModelScope.launch {
            connectRoomLock.withLock {
                if (mutableConnectedRoomFlow.value != null) return@launch
                connectRoom()
            }
        }
    }

    private suspend fun connectRoom() {
        runCatchingCancellable {
            val room = videoCallPrivateClient.createCurrentRoom()

            viewModelScope.launch {
                room::state.flow.collect { roomState ->
                    videoCallPrivateClient.logger.debug(
                        "new state for room ${room.name}: $roomState",
                        domain = VIDEO_CALL_DOMAIN
                    )
                    when (roomState) {
                        Room.State.CONNECTING -> {
                            mutableConnectionInfoFlow.value = ConnectionInfoState.Connecting
                        }
                        Room.State.CONNECTED -> {
                            mutableConnectionInfoFlow.value = ConnectionInfoState.Connected
                        }
                        Room.State.DISCONNECTED -> {
                            mutableFinishFlow.emitIn(viewModelScope, FinishReason.CallEnded)
                        }
                        Room.State.RECONNECTING -> {
                            mutableConnectionInfoFlow.value = ConnectionInfoState.ReConnecting
                        }
                    }
                }
            }

            viewModelScope.launch {
                room.events.collect { event ->
                    videoCallPrivateClient.logger.debug(
                        "new event in room ${room.name}: ${event.javaClass.simpleName}",
                        domain = VIDEO_CALL_DOMAIN
                    )
                    when (event) {
                        is RoomEvent.TrackSubscribed -> onTrackSubscribed(event)
                        is RoomEvent.TrackUnsubscribed -> onTrackUnsubscribed(event)
                        is RoomEvent.TrackMuted -> {
                            (event.participant as? LocalParticipant)?.updateControlsState()
                        }
                        is RoomEvent.TrackUnmuted -> {
                            (event.participant as? LocalParticipant)?.updateControlsState()
                        }
                        else -> {
                                /* Room events for state like reconnecting are buggy, prefer using room::state for handling state.
                                See https://github.com/livekit/client-sdk-android/issues/105#issuecomment-1250796210 */
                        }
                    }
                }
            }

            room.connect(url, token)

            val localParticipant = room.localParticipant

            localParticipant.setMicrophoneEnabled(true)
            localParticipant.setCameraEnabled(true)

            mutableConnectedRoomFlow.value = room

            mutableControlsFlow.value = ControlsState(
                micEnabled = localParticipant.isMicrophoneEnabled(),
                cameraEnabled = localParticipant.isCameraEnabled(),
            )

            viewModelScope.launch {
                room::remoteParticipants.flow.collect(mutableRemoteParticipantsFlow)
            }
            viewModelScope.launch {
                room::activeSpeakers.flow.collect(mutableActiveSpeakersFlow)
            }

            viewModelScope.launch {
                localParticipant::videoTracks.flow
                    .map { tracks ->
                        tracks.map { it.second }.firstNotNullOf { it as? LocalVideoTrack }
                    }
                    .collect(mutableLocalVideoTrackFlow)
            }
        }.onFailure {
            videoCallPrivateClient.logger.warn("Failed to join room", domain = VIDEO_CALL_DOMAIN)
            mutableFinishFlow.emit(FinishReason.UnableToConnect)
        }
    }

    private fun LocalParticipant.updateControlsState() {
        ControlsState(
            micEnabled = isMicrophoneEnabled(),
            cameraEnabled = isCameraEnabled(),
        )
    }

    fun onPermissionsRefused() {
        mutableFinishFlow.emitIn(viewModelScope, FinishReason.PermissionsRefused)
    }

    private fun onTrackSubscribed(trackSubscribed: RoomEvent.TrackSubscribed) {
        (trackSubscribed.track as? VideoTrack)?.let { videoTrack ->
            mutableRemoteVideoTracksFlow.value = mutableRemoteVideoTracksFlow.value + videoTrack
        }
    }

    private fun onTrackUnsubscribed(trackUnsubscribed: RoomEvent.TrackUnsubscribed) {
        (trackUnsubscribed.track as? VideoTrack)?.let { videoTrack ->
            mutableRemoteVideoTracksFlow.value = mutableRemoteVideoTracksFlow.value - videoTrack
        }
    }

    fun onMicClicked() {
        mutableConnectedRoomFlow.value?.localParticipant?.let {
            viewModelScope.launch {
                it.setMicrophoneEnabled(!it.isMicrophoneEnabled())
                mutableControlsFlow.value = mutableControlsFlow.value?.copy(micEnabled = it.isMicrophoneEnabled())
            }
        }
    }

    fun onCameraClicked() {
        val localParticipant = mutableConnectedRoomFlow.value?.localParticipant ?: return

        if (toggleCameraJob?.isCompleted == false) return

        toggleCameraJob = viewModelScope.launch {
            val shouldWaitCameras = localParticipant.isCameraEnabled()
            localParticipant.setCameraEnabled(!localParticipant.isCameraEnabled())
            // Workaround, see context https://github.com/livekit/client-sdk-android/issues/146
            if (shouldWaitCameras) cameraService.awaitAllCamerasAvailable()
            mutableControlsFlow.value = mutableControlsFlow.value?.copy(cameraEnabled = localParticipant.isCameraEnabled())
        }
    }

    fun onFlipCameraClicked() {
        val localParticipant = mutableConnectedRoomFlow.value?.localParticipant ?: return
        val oldVideoTrack = localParticipant.videoTracks.map { it.second }.first() as? LocalVideoTrack ?: return
        val newOptions = when (oldVideoTrack.options.position) {
            CameraPosition.FRONT -> LocalVideoTrackOptions(position = CameraPosition.BACK)
            CameraPosition.BACK -> LocalVideoTrackOptions(position = CameraPosition.FRONT)
            null -> LocalVideoTrackOptions()
        }
        if (flipJob?.isCompleted == false) return

        flipJob = viewModelScope.launch {
            // Workaround, see context https://github.com/livekit/client-sdk-android/issues/145
            oldVideoTrack.stopCapture()
            cameraService.awaitAllCamerasAvailable()
            oldVideoTrack.restartTrack(newOptions)
            mutableIsSelfMirrorFlow.value = newOptions.position == CameraPosition.FRONT
        }
    }

    fun onFullscreenClicked() {
        val controls = mutableControlsFlow.value
        mutableControlsFlow.value = controls?.copy(controlsVisible = !controls.controlsVisible)
    }

    fun onHangUpClicked() {
        mutableConnectedRoomFlow.value?.disconnect()
    }

    fun onPictureInPictureChanged(inPictureInPictureMode: Boolean) {
        mutableIsPictureInPictureFlow.value = inPictureInPictureMode
    }

    fun onPictureInPictureDismissed() {
        mutableConnectedRoomFlow.value?.disconnect()
    }

    override fun onCleared() {
        mutableConnectedRoomFlow.value?.disconnect()
        super.onCleared()
    }

    sealed interface VideoState {

        sealed interface SelfVideo : VideoState {
            val selfTrack: LocalVideoTrack
            val isSelfMirror: Boolean
        }

        sealed interface RemoteVideo : VideoState {
            val remoteTrack: VideoTrack
        }

        object None : VideoState

        class SelfOnly(
            override val selfTrack: LocalVideoTrack,
            override val isSelfMirror: Boolean,
        ) : VideoState, SelfVideo

        class RemoteOnly(
            override val remoteTrack: VideoTrack,
        ) : VideoState, RemoteVideo

        class Both(
            override val selfTrack: LocalVideoTrack,
            override val remoteTrack: VideoTrack,
            override val isSelfMirror: Boolean,
        ) : VideoState, SelfVideo, RemoteVideo
    }

    data class ControlsState(
        val micEnabled: Boolean,
        val cameraEnabled: Boolean,
        val controlsVisible: Boolean = true,
    )

    enum class ConnectionInfoState {
        Connecting,
        Connected,
        ReConnecting,
    }

    enum class FinishReason {
        CallEnded,
        PermissionsRefused,
        UnableToConnect,
    }

    companion object {
        internal const val VIDEO_CALL_DOMAIN = "VideoCall"
    }
}

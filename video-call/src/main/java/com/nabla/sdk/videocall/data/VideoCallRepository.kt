package com.nabla.sdk.videocall.data

import android.content.Context
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.boundary.VideoCall
import com.twilio.audioswitch.AudioDevice
import io.livekit.android.LiveKit
import io.livekit.android.LiveKitOverrides
import io.livekit.android.RoomOptions
import io.livekit.android.audio.AudioSwitchHandler
import io.livekit.android.room.Room
import io.livekit.android.room.track.CameraPosition
import io.livekit.android.room.track.LocalVideoTrackOptions
import io.livekit.android.util.flow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

internal class VideoCallRepository(
    private val applicationContext: Context,
    private val okHttpClient: OkHttpClient,
    private val repoScope: CoroutineScope
) {

    private val mutableCurrentRoomFlow = MutableStateFlow<Room?>(null)
    private val currentVideoCallFlow: Flow<VideoCall?> =
        createCurrentVideoCallFlow().stateIn(repoScope, SharingStarted.WhileSubscribed(), null)
    private val currentRoomMutex = Mutex()
    private var currentRoomDisconnectListenerJob: Job? = null

    suspend fun connectRoom(url: String, token: String): Room {
        // Only supporting one connected room (the current room) atm
        return currentRoomMutex.withLock {
            mutableCurrentRoomFlow.value?.disconnect()
            val newCurrentRoom = connectLiveKitRoom(url, token)
            mutableCurrentRoomFlow.value = newCurrentRoom
            clearCurrentRoomOnDisconnected(newCurrentRoom)
            newCurrentRoom
        }
    }

    fun watchCurrentVideoCall(): Flow<VideoCall?> {
        return currentVideoCallFlow
    }

    private fun clearCurrentRoomOnDisconnected(room: Room) {
        currentRoomDisconnectListenerJob?.cancel()
        repoScope.launch {
            room::state.flow.firstOrNull { roomState -> roomState == Room.State.DISCONNECTED }
            currentRoomMutex.withLock { mutableCurrentRoomFlow.value = null }
        }.also { currentRoomDisconnectListenerJob = it }
    }

    private suspend fun connectLiveKitRoom(url: String, token: String): Room {
        val room = LiveKit.create(
            applicationContext,
            overrides = LiveKitOverrides(
                okHttpClient,
                audioHandler = AudioSwitchHandler(applicationContext).apply {
                    preferredDeviceList = listOf(
                        AudioDevice.BluetoothHeadset::class.java,
                        AudioDevice.WiredHeadset::class.java,
                        AudioDevice.Speakerphone::class.java,
                        AudioDevice.Earpiece::class.java,
                    )
                }
            ),
            options = RoomOptions(
                adaptiveStream = true,
                dynacast = true,
                videoTrackCaptureDefaults = LocalVideoTrackOptions(position = CameraPosition.FRONT),
            ),
        )

        room.connect(
            url = url,
            token = token,
        )

        return room
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun createCurrentVideoCallFlow(): Flow<VideoCall?> {
        return mutableCurrentRoomFlow.flatMapLatest { room ->
            return@flatMapLatest if (room == null) {
                flowOf(null)
            } else {
                flowAsVideoCallWhileNotDisconnected(room)
            }
        }
    }

    private fun flowAsVideoCallWhileNotDisconnected(room: Room): Flow<VideoCall?> {
        return room::state.flow.map { roomState ->
            when (roomState) {
                Room.State.DISCONNECTED -> {
                    // Disconnected is a livekit terminal state after hangup.
                    return@map null
                }
                else -> return@map VideoCall(Uuid.fromString(requireNotNull(room.name)))
            }
        }
    }
}

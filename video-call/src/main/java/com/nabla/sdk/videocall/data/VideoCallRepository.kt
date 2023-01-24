package com.nabla.sdk.videocall.data

import android.content.Context
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.entity.VideoCall
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
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient

internal class VideoCallRepository(
    private val applicationContext: Context,
    private val okHttpClient: OkHttpClient,
    private val repoScope: CoroutineScope,
    private val videoCallRepositoryReporterHelper: VideoCallRepositoryReporterHelper
) {

    private val mutableCurrentRoomFlow = MutableStateFlow<Room?>(null)
    private val currentVideoCallFlow: Flow<VideoCall?> =
        createCurrentVideoCallFlow().stateIn(repoScope, SharingStarted.WhileSubscribed(), null)
    private val currentRoomMutex = Mutex()

    init {
        repoScope.launch {
            mutableCurrentRoomFlow.collectLatest { room ->
                if (room == null) return@collectLatest
                coroutineScope {
                    videoCallRepositoryReporterHelper.installRoomReporter(this, room)
                }
            }
        }
    }

    suspend fun createCurrentRoom(): Room {
        // Only supporting one connected room (the current room) atm
        return currentRoomMutex.withLock {
            mutableCurrentRoomFlow.value?.disconnect()
            val newCurrentRoom = createLiveKitRoom()
            mutableCurrentRoomFlow.value = newCurrentRoom
            newCurrentRoom
        }
    }

    fun watchCurrentVideoCall(): Flow<VideoCall?> {
        return currentVideoCallFlow
    }

    private fun createLiveKitRoom(): Room {
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
        return room::state.flow.transform { roomState ->
            when (roomState) {
                Room.State.DISCONNECTED -> {
                    // Disconnected is a livekit terminal state after hangup.
                    emit(null)
                }
                Room.State.CONNECTED -> {
                    emit(VideoCall(Uuid.fromString(room.name)))
                }
                else -> { /* no-op */
                }
            }
        }
    }
}

package com.nabla.sdk.tests.common

import android.content.Context
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.domain.boundary.VideoCallModule
import com.nabla.sdk.core.domain.entity.VideoCall
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeVideoCallModule : VideoCallModule {
    private val currentVideoCall = MutableStateFlow<VideoCall?>(null)

    override fun openVideoCall(context: Context, url: String, roomId: String, token: String) {
        currentVideoCall.value = VideoCall(Uuid.fromString(roomId))
    }

    override fun watchCurrentVideoCall(): Flow<VideoCall?> = currentVideoCall
}

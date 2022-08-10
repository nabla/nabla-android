package com.nabla.sdk.videocall

import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.VideoCallModule
import io.livekit.android.room.Room

internal interface NablaVideoCallClient : VideoCallModule {
    val logger: Logger

    suspend fun connectRoom(url: String, token: String): Room
}

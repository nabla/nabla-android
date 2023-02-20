package com.nabla.sdk.videocall

import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.videocall.domain.CameraService
import io.livekit.android.room.Room

internal interface VideoCallPrivateClient {
    val cameraService: CameraService
    val logger: Logger

    suspend fun createCurrentRoom(): Room
}

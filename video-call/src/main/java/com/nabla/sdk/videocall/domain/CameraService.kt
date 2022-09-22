package com.nabla.sdk.videocall.domain

internal interface CameraService {
    suspend fun awaitAllCamerasAvailable()
}

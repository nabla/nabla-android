package com.nabla.sdk.videocall

import com.nabla.sdk.core.domain.boundary.Logger
import io.livekit.android.room.Room

internal interface NablaVideoCallClient {
    val logger: Logger

    suspend fun createCurrentRoom(): Room
}

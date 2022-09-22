package com.nabla.sdk.videocall

import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.boundary.VideoCallModule
import com.nabla.sdk.core.domain.entity.ConfigurationException

public interface NablaVideoCallModule : VideoCallModule {
    public companion object Factory {
        public operator fun invoke(): VideoCallModule.Factory =
            VideoCallModule.Factory { NablaVideoCallClientImpl(coreContainer = it) }
    }
}

public val NablaClient.videoCallModule: VideoCallModule
    get() = coreContainer.videoCallModule
        ?: throw moduleNotInitialized()

internal val NablaClient.videoCallInternalModule: VideoCallInternalModule
    get() = coreContainer.videoCallModule as? VideoCallInternalModule
        ?: throw moduleNotInitialized()

internal val NablaClient.videoCallClient: NablaVideoCallClient
    get() = videoCallModule as? NablaVideoCallClient
        ?: throw moduleNotInitialized()

private fun moduleNotInitialized() =
    ConfigurationException.ModuleNotInitialized("NablaVideoCallModule")

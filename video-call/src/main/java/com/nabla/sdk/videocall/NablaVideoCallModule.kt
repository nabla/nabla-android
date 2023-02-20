package com.nabla.sdk.videocall

import com.nabla.sdk.core.NablaClient
import com.nabla.sdk.core.domain.boundary.VideoCallModule
import com.nabla.sdk.core.domain.entity.ConfigurationException

public interface NablaVideoCallModule {
    public companion object Factory {
        public operator fun invoke(): VideoCallModule.Factory =
            VideoCallModule.Factory { VideoCallModuleImpl(coreContainer = it) }
    }
}

internal val NablaClient.videoCallPrivateClient: VideoCallPrivateClient
    get() = coreContainer.videoCallModule as? VideoCallModuleImpl
        ?: throw ConfigurationException.ModuleNotInitialized("NablaVideoCallModule")

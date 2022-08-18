package com.nabla.sdk.core.domain.boundary

import android.content.Context
import com.nabla.sdk.core.domain.entity.ModuleType
import kotlinx.coroutines.flow.Flow

public interface VideoCallModule : Module {
    public fun openVideoCall(context: Context, url: String, roomId: String, token: String)
    public fun watchCurrentVideoCall(): Flow<VideoCall?>

    public fun interface Factory : Module.Factory<VideoCallModule> {
        override fun type(): ModuleType = ModuleType.VIDEO_CALL
    }
}

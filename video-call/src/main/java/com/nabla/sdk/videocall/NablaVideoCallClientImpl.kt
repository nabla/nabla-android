package com.nabla.sdk.videocall

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.VideoCall
import com.nabla.sdk.core.domain.boundary.VideoCallModule
import com.nabla.sdk.core.injection.CoreContainer
import com.nabla.sdk.videocall.domain.CameraService
import com.nabla.sdk.videocall.injection.VideoCallContainer
import io.livekit.android.room.Room
import kotlinx.coroutines.flow.Flow

internal class NablaVideoCallClientImpl internal constructor(
    coreContainer: CoreContainer,
) : NablaVideoCallClient, VideoCallModule, VideoCallInternalModule {

    private val name = coreContainer.name

    private val videoCallContainer = VideoCallContainer(
        baseOkHttpClient = coreContainer.okHttpClient,
        logger = coreContainer.logger,
        context = coreContainer.configuration.context,
        errorReporter = coreContainer.errorReporter,
    )

    override val logger: Logger = videoCallContainer.logger

    override fun openVideoCall(context: Context, url: String, roomId: String, token: String) {
        context.startActivity(
            VideoCallActivity.newIntent(
                context,
                url,
                roomId,
                token,
                name,
            ).apply {
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
        )
    }

    override fun watchCurrentVideoCall(): Flow<VideoCall?> {
        return videoCallContainer.videoCallRepository.watchCurrentVideoCall()
    }

    override suspend fun createCurrentRoom(): Room {
        return videoCallContainer.videoCallRepository.createCurrentRoom()
    }

    override val cameraService: CameraService
        get() = videoCallContainer.cameraService
}

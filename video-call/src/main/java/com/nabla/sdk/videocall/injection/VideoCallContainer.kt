package com.nabla.sdk.videocall.injection

import android.content.Context
import com.nabla.sdk.core.domain.boundary.ErrorReporter
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.videocall.data.CameraServiceImpl
import com.nabla.sdk.videocall.data.VideoCallRepository
import com.nabla.sdk.videocall.data.VideoCallRepositoryReporterHelper
import com.nabla.sdk.videocall.domain.CameraService
import kotlinx.coroutines.CoroutineScope
import okhttp3.OkHttpClient

internal class VideoCallContainer(
    baseOkHttpClient: OkHttpClient,
    val logger: Logger,
    val context: Context,
    val errorReporter: ErrorReporter,
    backgroundScope: CoroutineScope,
) {

    private val okHttpClient = baseOkHttpClient.newBuilder()
        .build()

    val videoCallRepository = VideoCallRepository(
        applicationContext = context,
        okHttpClient = okHttpClient,
        repoScope = backgroundScope,
        videoCallRepositoryReporterHelper = VideoCallRepositoryReporterHelper(errorReporter),
    )

    val cameraService: CameraService = CameraServiceImpl(logger, context.applicationContext)
}

package com.nabla.sdk.videocall.injection

import android.content.Context
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.videocall.data.VideoCallRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

internal class VideoCallContainer(
    baseOkHttpClient: OkHttpClient,
    val logger: Logger,
    val context: Context,
) {
    private val okHttpClient = baseOkHttpClient.newBuilder()
        .build()

    val videoCallRepository = VideoCallRepository(
        applicationContext = context,
        okHttpClient = okHttpClient,
        repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    )
}

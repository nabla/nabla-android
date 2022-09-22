package com.nabla.sdk.videocall.data

import android.content.Context
import android.hardware.camera2.CameraManager
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.videocall.VideoCallViewModel
import com.nabla.sdk.videocall.domain.CameraService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class CameraServiceImpl(
    private val logger: Logger,
    private val context: Context,
) : CameraService {

    override suspend fun awaitAllCamerasAvailable() {
        logger.debug("await all cameras available", domain = VideoCallViewModel.VIDEO_CALL_DOMAIN)
        val cameraManager = context.applicationContext.getSystemService(CameraManager::class.java)
        val cameraIdList = cameraManager.cameraIdList.toMutableList()
        if (cameraIdList.isEmpty()) {
            logger.debug("no camera, finishing await", domain = VideoCallViewModel.VIDEO_CALL_DOMAIN)
            return
        }
        suspendCancellableCoroutine<Unit> { cancellableContinuation ->
            val callback = object : CameraManager.AvailabilityCallback() {
                override fun onCameraAvailable(cameraId: String) {
                    super.onCameraAvailable(cameraId)
                    logger.debug("on camera available $cameraId", domain = VideoCallViewModel.VIDEO_CALL_DOMAIN)
                    cameraIdList.remove(cameraId)
                    logger.debug("still waiting cameras $cameraIdList", domain = VideoCallViewModel.VIDEO_CALL_DOMAIN)
                    if (cameraIdList.isEmpty()) {
                        logger.debug("all cameras available", domain = VideoCallViewModel.VIDEO_CALL_DOMAIN)
                        cameraManager.unregisterAvailabilityCallback(this)
                        cancellableContinuation.resume(Unit)
                    }
                }
            }

            cancellableContinuation.invokeOnCancellation {
                cameraManager.unregisterAvailabilityCallback(callback)
            }

            cameraManager.registerAvailabilityCallback(callback, null)
        }
    }
}

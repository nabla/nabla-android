package com.nabla.sdk.messaging.ui.helper

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

internal class CameraFileProvider : FileProvider() {

    companion object {
        private fun authority(context: Context) = "${context.packageName}.nabla.messaging.camerafileprovider"

        fun getUri(context: Context, file: File): Uri {
            return getUriForFile(
                context,
                authority(context),
                file
            )
        }

        fun createFile(context: Context): File {
            return File(context.cacheDir, "/nabla/capture/medias/capture_${System.currentTimeMillis()}")
        }
    }
}

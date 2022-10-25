package com.nabla.sdk.core.ui.providers

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

internal class CameraFileProvider : FileProvider() {

    companion object {
        private fun authority(context: Context) = "${context.packageName}.nabla.core.camerafileprovider"

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

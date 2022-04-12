package com.nabla.sdk.core.ui.helpers.mediapicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.messaging.ui.helper.CameraFileProvider
import java.io.File

class CaptureImageFromCameraActivityContract : ActivityResultContract<Unit, MediaPickingResult<LocalMedia.Image>>() {
    private var currentFile: File? = null

    override fun createIntent(context: Context, input: Unit): Intent {
        val file = CameraFileProvider.createFile(context)
        currentFile = file

        val takeVideoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takeVideoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        takeVideoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        file.parentFile?.mkdirs()
        file.createNewFile()

        val videoUri: Uri = CameraFileProvider.getUri(context, file)

        takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)

        return takeVideoIntent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): MediaPickingResult<LocalMedia.Image> {
        try {
            if (resultCode == Activity.RESULT_CANCELED) {
                return MediaPickingResult.Cancelled()
            }

            if (resultCode != Activity.RESULT_OK) {
                throw RuntimeException("Activity result: $resultCode")
            }

            val file = currentFile ?: throw IllegalStateException("No file available")

            val mimeType = MimeTypeHelper.getFileMediaMimeType(file) as? MimeType.Image
                ?: throw IllegalArgumentException("Mime type is not a supported image one")

            return MediaPickingResult.Success(
                when (mimeType) {
                    MimeType.Image.PNG -> LocalMedia.Image(
                        file.toURI(),
                        generateFileName("png"),
                        MimeType.Image.PNG,
                    )
                    MimeType.Image.JPEG -> LocalMedia.Image(
                        file.toURI(),
                        generateFileName("jpg"),
                        MimeType.Image.JPEG,
                    )
                }
            )
        } catch (error: Exception) {
            return MediaPickingResult.Failure(error)
        } finally {
            currentFile = null
        }
    }
}

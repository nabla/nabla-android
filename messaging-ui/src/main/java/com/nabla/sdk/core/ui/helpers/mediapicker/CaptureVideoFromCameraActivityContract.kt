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

internal class CaptureVideoFromCameraActivityContract : ActivityResultContract<Unit, MediaPickingResult<LocalMedia.Video>>() {
    private var currentFile: File? = null

    override fun createIntent(context: Context, input: Unit): Intent {
        val file = CameraFileProvider.createFile(context)
        currentFile = file

        val takePhotoIntent = Intent(MediaStore.ACTION_VIDEO_CAPTURE)
        takePhotoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        takePhotoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        file.parentFile?.mkdirs()
        file.createNewFile()

        val videoUri: Uri = CameraFileProvider.getUri(context, file)

        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, videoUri)

        return takePhotoIntent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): MediaPickingResult<LocalMedia.Video> {
        try {
            if (resultCode == Activity.RESULT_CANCELED) {
                return MediaPickingResult.Cancelled()
            }

            if (resultCode != Activity.RESULT_OK) {
                throw RuntimeException("Activity result: $resultCode")
            }

            val file = currentFile ?: throw IllegalStateException("No file available")

            val mimeType = MimeTypeHelper.getFileMediaMimeType(file) as? MimeType.Video
                ?: throw IllegalArgumentException("Mime type is not a supported video one")

            return MediaPickingResult.Success(
                LocalMedia.Video(
                    file.toURI(),
                    generateFileName(extension = mimeType.subtype),
                    mimeType,
                )
            )
        } catch (error: Exception) {
            return MediaPickingResult.Failure(error)
        } finally {
            currentFile = null
        }
    }
}

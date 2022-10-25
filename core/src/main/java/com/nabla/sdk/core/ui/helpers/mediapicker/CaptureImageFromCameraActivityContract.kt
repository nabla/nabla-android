package com.nabla.sdk.core.ui.helpers.mediapicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContract
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.MimeType
import com.nabla.sdk.core.ui.helpers.mediapicker.mimetypedetector.MimeTypeHelper
import com.nabla.sdk.core.ui.providers.CameraFileProvider
import java.io.File

@NablaInternal
public class CaptureImageFromCameraActivityContract(
    private val context: Context,
) : ActivityResultContract<Unit, MediaPickingResult<LocalMedia.Image>>() {
    private var currentFile: File? = null

    override fun createIntent(context: Context, input: Unit): Intent {
        val file = CameraFileProvider.createFile(context)
        currentFile = file

        val takePhotoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        takePhotoIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        takePhotoIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        file.parentFile?.mkdirs()
        file.createNewFile()

        val imageUri: Uri = CameraFileProvider.getUri(context, file)

        takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)

        return takePhotoIntent
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

            val mimeType = MimeTypeHelper.getInstance(context).getFileMediaMimeType(file) as? MimeType.Image
                ?: throw IllegalArgumentException("Mime type is not a supported image one")

            return MediaPickingResult.Success(
                LocalMedia.Image(
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

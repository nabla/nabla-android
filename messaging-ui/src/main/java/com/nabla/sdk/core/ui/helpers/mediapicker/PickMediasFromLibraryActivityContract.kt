package com.nabla.sdk.core.ui.helpers.mediapicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContract
import com.nabla.sdk.core.data.helper.toJvmURI
import com.nabla.sdk.core.domain.entity.MimeType

internal class PickMediasFromLibraryActivityContract(private val context: Context) :
    ActivityResultContract<Array<MimeType>, MediaPickingResult<List<LocalMedia>>>() {

    override fun createIntent(context: Context, mimeTypes: Array<MimeType>): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = mimeTypes.joinToString("|") { it.stringRepresentation }
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.map { it.stringRepresentation }.toTypedArray())
        }
    }

    override fun parseResult(resultCode: Int, result: Intent?): MediaPickingResult<List<LocalMedia>> {
        try {
            if (resultCode == Activity.RESULT_CANCELED) {
                return MediaPickingResult.Cancelled()
            }

            if (resultCode != Activity.RESULT_OK) {
                throw RuntimeException("Activity result: $resultCode")
            }

            result?.clipData?.let { clipData ->
                val uris = mutableListOf<Uri>()
                for (i in 0 until clipData.itemCount) {
                    uris.add(clipData.getItemAt(i).uri)
                }
                return MediaPickingResult.Success(createMediasOrThrow(uris))
            }

            result?.data?.let { uri ->
                return MediaPickingResult.Success(createMediasOrThrow(listOf(uri)))
            }

            throw RuntimeException("Unable to get uri from result")
        } catch (error: Exception) {
            return MediaPickingResult.Failure(error)
        }
    }

    private fun createMediasOrThrow(uris: List<Uri>): List<LocalMedia> {
        return uris.map { uri ->
            val mimeTypeRepresentation = context.contentResolver.getType(uri)
                ?: throw IllegalArgumentException("Unable to get mime type for uri: $uri")

            return@map LocalMedia.create(uri.toJvmURI(), mimeTypeRepresentation, getMediaName(uri))
        }
    }

    private fun getMediaName(uri: Uri): String? {
        try {
            context.contentResolver.query(uri, null, null, null, null).use { cursor ->
                if (cursor != null && cursor.moveToFirst()) {
                    val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (index >= 0) {
                        return cursor.getString(index)
                    }
                }
            }

            return null
        } catch (t: Throwable) {
            return null
        }
    }
}

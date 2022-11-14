package com.nabla.sdk.core.ui.helpers.mediapicker

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.result.contract.ActivityResultContract
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.MimeType
import java.net.URI

@NablaInternal
public class PickMediasFromLibraryActivityContract(private val context: Context) :
    ActivityResultContract<Array<MimeType>, MediaPickingResult<List<LocalMedia>>>() {

    override fun createIntent(context: Context, input: Array<MimeType>): Intent {
        return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = input.joinToString("|") { it.stringRepresentation }
            addCategory(Intent.CATEGORY_OPENABLE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            putExtra(Intent.EXTRA_MIME_TYPES, input.map { it.stringRepresentation }.toTypedArray())
        }
    }

    override fun parseResult(resultCode: Int, intent: Intent?): MediaPickingResult<List<LocalMedia>> {
        try {
            if (resultCode == Activity.RESULT_CANCELED) {
                return MediaPickingResult.Cancelled()
            }

            if (resultCode != Activity.RESULT_OK) {
                throw RuntimeException("Activity result: $resultCode")
            }

            val singleMimeTypeOrNull = if (intent?.clipData?.description?.mimeTypeCount == 1) {
                intent.clipData?.description?.getMimeType(0)
            } else null

            intent?.clipData?.let { clipData ->
                val urisWithMimeTypes = mutableListOf<Pair<Uri, String?>>()
                for (i in 0 until clipData.itemCount) {
                    urisWithMimeTypes.add(clipData.getItemAt(i).uri to singleMimeTypeOrNull)
                }
                return MediaPickingResult.Success(createMediasOrThrow(urisWithMimeTypes))
            }

            intent?.data?.let { uri ->
                return MediaPickingResult.Success(createMediasOrThrow(listOf(Pair(uri, singleMimeTypeOrNull))))
            }

            throw RuntimeException("Unable to get uri from result")
        } catch (error: Exception) {
            return MediaPickingResult.Failure(error)
        }
    }

    private fun createMediasOrThrow(uris: List<Pair<Uri, String?>>): List<LocalMedia> {
        return uris.map { (uri, mimeTypeString) ->
            val mimeTypeRepresentation = mimeTypeString
                ?: context.contentResolver.getType(uri)
                ?: throw IllegalArgumentException("Unable to get mime type for uri: $uri")

            return@map LocalMedia.create(URI.create(uri.toString()), mimeTypeRepresentation, getMediaName(uri))
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

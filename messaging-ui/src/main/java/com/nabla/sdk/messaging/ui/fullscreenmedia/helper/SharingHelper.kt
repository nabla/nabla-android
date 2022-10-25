package com.nabla.sdk.messaging.ui.fullscreenmedia.helper

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

// This need to be in sync with manifest declaration
private fun fileProviderAuthority(context: Context) = "${context.packageName}.nabla.messaging.ui.sharefileprovider"

internal fun Bitmap.createSharableJpegImage(
    id: String,
    context: Context,
): Uri {
    val tempFile = File(context.cacheDir, "/nabla/share/images/image_$id.jpg")
    tempFile.parentFile?.mkdirs()

    if (tempFile.createNewFile()) {
        val fileOutputStream = FileOutputStream(tempFile)

        fileOutputStream.use {
            compress(Bitmap.CompressFormat.JPEG, 80, fileOutputStream)
        }
    }

    return FileProvider.getUriForFile(
        context,
        fileProviderAuthority(context),
        tempFile
    )
}

internal fun createSharableDocument(
    path: String,
    context: Context,
): Uri {
    val file = File(path)

    val tempFile = File(context.cacheDir, "/nabla/share/documents/${file.nameWithoutExtension}_${path.hashCode()}.${file.extension}")
    tempFile.parentFile?.mkdirs()

    if (!tempFile.exists()) {
        file.copyTo(tempFile)
    }

    return FileProvider.getUriForFile(
        context,
        fileProviderAuthority(context),
        tempFile
    )
}

internal fun Uri.createSharingIntent(mimeType: String) = Intent().apply {
    action = Intent.ACTION_SEND
    clipData = ClipData(
        this@createSharingIntent.lastPathSegment,
        arrayOf(mimeType),
        ClipData.Item(this@createSharingIntent)
    )
    putExtra(Intent.EXTRA_STREAM, this@createSharingIntent)
    type = mimeType
    flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
}

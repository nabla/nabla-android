package com.nabla.sdk.docscanner.core.components.impl

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import coil.imageLoader
import coil.request.ImageRequest
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend fun Context.getBitmapFromUri(
    imageUri: Uri,
    allowHardware: Boolean = true,
    backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
): Bitmap {
    val imageRequest = ImageRequest.Builder(applicationContext)
        .data(imageUri)
        .allowHardware(allowHardware)
        .build()
    return withContext(backgroundDispatcher) {
        applicationContext.imageLoader.execute(imageRequest).drawable as BitmapDrawable
    }.bitmap
}

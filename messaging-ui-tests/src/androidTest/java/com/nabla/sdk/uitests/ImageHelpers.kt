package com.nabla.sdk.uitests

import android.app.Activity
import android.app.Instrumentation
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import com.nabla.sdk.core.domain.entity.MimeType
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import com.nabla.sdk.uitests.R as TestsR

// inspired from https://proandroiddev.com/testing-camera-and-galley-intents-with-espresso-218eb9f59da9
fun createImageGallerySetResultStub(context: Context): Instrumentation.ActivityResult {
    val bundle = Bundle()
    val parcels = ArrayList<Parcelable>()
    val resultData = Intent()
    val dir = context.externalCacheDir
    val file = File(dir?.path, "ui_test_image.jpg")
    val uri: Uri = Uri.fromFile(file)
    val parcelable1 = uri as Parcelable
    parcels.add(parcelable1)
    bundle.putParcelableArrayList(Intent.EXTRA_STREAM, parcels)
    resultData.putExtras(bundle)
    resultData.clipData = ClipData("label", arrayOf(MimeType.Image.Jpeg.stringRepresentation), ClipData.Item(uri))
    return Instrumentation.ActivityResult(Activity.RESULT_OK, resultData)
}

fun savePickedImage(context: Context) {
    val bm = BitmapFactory.decodeResource(context.resources, TestsR.mipmap.image)!!
    val dir = context.externalCacheDir
    val file = File(dir?.path, "ui_test_image.jpg")
    val outStream: FileOutputStream?
    try {
        outStream = FileOutputStream(file)
        bm.compress(Bitmap.CompressFormat.JPEG, 100, outStream)
        with(outStream) {
            flush()
            close()
        }
    } catch (e: FileNotFoundException) {
        e.printStackTrace()
    } catch (e: IOException) {
        e.printStackTrace()
    }
}

package com.nabla.sdk.docscanner.core.components.impl

import android.content.Context
import android.graphics.Bitmap
import com.nabla.sdk.core.data.helper.toAndroidUri
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.docscanner.core.components.DocumentDetector
import com.nabla.sdk.docscanner.core.models.NormalizedCorners
import com.nabla.sdk.docscanner.core.models.Point
import com.nabla.sdk.docscanner.ml.Myrtle2
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.nio.ByteBuffer
import java.nio.ByteOrder

internal class AndroidDocumentDetector(
    private val context: Context,
) : DocumentDetector {
    private val detectorModel by lazy { Myrtle2.newInstance(context) }
    private val inputFeature = TensorBuffer.createFixedSize(intArrayOf(1, INPUT_SIZE, INPUT_SIZE, 3), DataType.FLOAT32)

    private val pixels: IntArray = IntArray(INPUT_SIZE * INPUT_SIZE)
    private val imgData: ByteBuffer = ByteBuffer.allocateDirect(INPUT_SIZE * INPUT_SIZE * 3 * 4)

    init {
        imgData.order(ByteOrder.nativeOrder())
    }

    override suspend fun detectDocumentCorners(imageUri: Uri): NormalizedCorners? {
        val bitmap = context.getBitmapFromUri(
            imageUri = imageUri.toAndroidUri(),
            allowHardware = false,
        )

        val resizedBitmap = Bitmap.createScaledBitmap(
            bitmap,
            INPUT_SIZE,
            INPUT_SIZE,
            false
        )
        resizedBitmap.getPixels(
            pixels,
            0,
            INPUT_SIZE,
            0,
            0,
            INPUT_SIZE,
            INPUT_SIZE
        )
        val pixelsCount = INPUT_SIZE * INPUT_SIZE
        for (i in 0 until pixelsCount) {
            val c = pixels[i]
            val r = (c shr 16 and 0xff) / 255.0f
            val g = (c shr 8 and 0xff) / 255.0f
            val b = (c and 0xff) / 255.0f
            imgData.putFloat(r)
            imgData.putFloat(g)
            imgData.putFloat(b)
        }
        imgData.rewind()
        inputFeature.loadBuffer(imgData)
        val outputs = detectorModel.process(inputFeature)
        val corners = outputs.outputFeature0AsTensorBuffer.floatArray
            .map { feature -> feature / INPUT_SIZE.toFloat() }
            .toFloatArray()
        return if (outputs.outputFeature1AsTensorBuffer.floatArray[0] > 0) {
            NormalizedCorners(
                topLeft = Point(corners[0], corners[1]),
                topRight = Point(corners[2], corners[3]),
                bottomRight = Point(corners[4], corners[5]),
                bottomLeft = Point(corners[6], corners[7])
            )
        } else {
            null
        }
    }

    companion object {
        private const val INPUT_SIZE = 256
    }
}

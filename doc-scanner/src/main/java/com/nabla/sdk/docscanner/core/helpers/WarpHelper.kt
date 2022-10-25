package com.nabla.sdk.docscanner.core.helpers

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import com.nabla.sdk.docscanner.core.models.NormalizedCorners

internal fun getWarpedBitmap(srcBitmap: Bitmap, corners: NormalizedCorners, dstBitmapFactory: (width: Int, height: Int) -> Bitmap): Bitmap {
    val dstBitmap = dstBitmapFactory(
        (srcBitmap.width * corners.getWidthRatio()).toInt(),
        (srcBitmap.height * corners.getHeightRatio()).toInt()
    )
    val canvas = Canvas(dstBitmap)
    val warpMatrix = corners.getWarpMatrix(srcBitmap)
    canvas.drawBitmap(srcBitmap, warpMatrix, null)
    return dstBitmap
}

internal fun NormalizedCorners.getWarpMatrix(srcBitmap: Bitmap): Matrix {
    val dstWidth = srcBitmap.width * getWidthRatio()
    val dstHeight = srcBitmap.height * getHeightRatio()
    val src = floatArrayOf(
        topLeft.x * srcBitmap.width, topLeft.y * srcBitmap.height,
        topRight.x * srcBitmap.width, topRight.y * srcBitmap.height,
        bottomLeft.x * srcBitmap.width, bottomLeft.y * srcBitmap.height,
        bottomRight.x * srcBitmap.width, bottomRight.y * srcBitmap.height,
    )
    val dst = floatArrayOf(
        0f, 0f,
        dstWidth, 0f,
        0f, dstHeight,
        dstWidth, dstHeight
    )
    return Matrix().apply {
        setPolyToPoly(
            src,
            0,
            dst,
            0,
            4
        )
    }
}

package com.nabla.sdk.docscanner.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.Magnifier
import androidx.annotation.ColorInt
import androidx.appcompat.widget.AppCompatImageView
import com.google.android.material.R
import com.google.android.material.color.MaterialColors
import com.nabla.sdk.core.ui.helpers.dpToPx
import java.lang.Float.min
import kotlin.math.max
import kotlin.math.sqrt

internal class DocumentCropView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : AppCompatImageView(context, attrs, defStyleAttr) {

    @ColorInt
    private val paintColor: Int = MaterialColors.getColor(this, R.attr.colorSecondary)

    private val rectanglePaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        color = paintColor
        strokeWidth = 2f
    }
    private val cornerPaint: Paint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        color = paintColor
        strokeWidth = 4f
    }

    // point objects
    var normalizedCorners: Array<PointF>? = null
        set(value) {
            // make a copy of points as we will mutate them
            val newValue = if (value != null) {
                Array(4) { PointF(0F, 0F) }.apply {
                    forEachIndexed { index, pointF ->
                        pointF.set(value[index])
                    }
                }
            } else {
                null
            }
            field = newValue
            invalidate()
        }
    private var currentCorner: Int? = null

    private val cornerRadius = 20F

    private val magnifier: Magnifier? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val size = context.dpToPx(80f)
        Magnifier.Builder(this@DocumentCropView)
            .setSize(size, size)
            .setCornerRadius(size / 2f)
            .setDefaultSourceToMagnifierOffset(0, -size)
            .build()
    } else {
        null
    }

    private fun scaleCorners(): Array<PointF>? {
        val scaledCorners = normalizedCorners?.mapNotNull {
            getCornerImageViewPointF(it)
        }
        return if (scaledCorners?.size == 4) {
            scaledCorners.toTypedArray()
        } else {
            null
        }
    }

    private fun getCornerImageViewPointF(normalizedCorner: PointF): PointF? {
        val currentDrawable = drawable ?: return null
        val points = floatArrayOf(normalizedCorner.x * currentDrawable.intrinsicWidth, normalizedCorner.y * currentDrawable.intrinsicHeight)
        val currentImageMatrix = imageMatrix ?: return null
        currentImageMatrix.mapPoints(points)
        return PointF(points[0], points[1])
    }

    private fun getImageViewPointToNormalizedCorner(imageViewPoint: PointF): PointF? {
        val currentImageMatrix = imageMatrix ?: return null
        val inverse = Matrix()
        currentImageMatrix.invert(inverse)
        val points = floatArrayOf(imageViewPoint.x, imageViewPoint.y)
        inverse.mapPoints(points)
        val currentDrawable = drawable ?: return null
        return PointF(points[0] / currentDrawable.intrinsicWidth, points[1] / currentDrawable.intrinsicHeight)
    }

    private val path = Path()

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val points = scaleCorners() ?: return
        canvas.apply {
            path.reset()
            path.moveTo(points[0].x, points[0].y)
            path.lineTo(points[1].x, points[1].y)
            path.lineTo(points[2].x, points[2].y)
            path.lineTo(points[3].x, points[3].y)
            path.lineTo(points[0].x, points[0].y)
            path.close()
            drawPath(path, rectanglePaint)
        }
        canvas.drawCircle(points[0].x, points[0].y, cornerRadius, cornerPaint)
        canvas.drawCircle(points[1].x, points[1].y, cornerRadius, cornerPaint)
        canvas.drawCircle(points[2].x, points[2].y, cornerRadius, cornerPaint)
        canvas.drawCircle(points[3].x, points[3].y, cornerRadius, cornerPaint)
    }

    @SuppressLint("ClickableViewAccessibility") // no trivial way of making the crop accessible
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                currentCorner = getCloseCornerIndex(x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                currentCorner?.let { currentCorner ->
                    val imageRect = RectF(drawable.bounds)
                    imageMatrix.mapRect(imageRect)
                    val inBoundX = min(max(x, imageRect.left), imageRect.right)
                    val inBoundY = min(max(y, imageRect.top), imageRect.bottom)
                    val updatedNormalizedCorner = getImageViewPointToNormalizedCorner(PointF(inBoundX, inBoundY))
                    updatedNormalizedCorner?.let {
                        normalizedCorners?.get(currentCorner)?.x = updatedNormalizedCorner.x
                        normalizedCorners?.get(currentCorner)?.y = updatedNormalizedCorner.y
                    }
                    invalidate()
                }
            }
            MotionEvent.ACTION_UP -> {
                currentCorner = null
            }
        }
        updateMagnifier(x, y)
        return true
    }

    private fun updateMagnifier(x: Float, y: Float) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return
        if (currentCorner != null) {
            magnifier?.show(x, y)
        } else {
            magnifier?.dismiss()
        }
    }

    private fun getCloseCornerIndex(x: Float, y: Float): Int? {
        return normalizedCorners?.let {
            for (i in it.indices) {
                val cornerImageViewPoint = getCornerImageViewPointF(it[i])
                cornerImageViewPoint?.let {
                    val dx = (x - cornerImageViewPoint.x)
                    val dy = (y - cornerImageViewPoint.y)
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist <= resources.dpToPx(40f)) {
                        return i
                    }
                }
            }
            return null
        }
    }
}

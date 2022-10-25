package com.nabla.sdk.docscanner.ui.extensions

import android.graphics.PointF
import com.nabla.sdk.docscanner.core.models.Point

internal fun Point.toPointF() = PointF(x, y)

internal fun PointF.toPoint() = Point(x, y)

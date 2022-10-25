package com.nabla.sdk.docscanner.ui.extensions

import android.graphics.PointF
import com.nabla.sdk.docscanner.core.models.NormalizedCorners

internal fun Array<PointF>.toNormalizedCorners(): NormalizedCorners = NormalizedCorners.fromList(map { it.toPoint() })

internal fun NormalizedCorners.toAndroidArrayList(): ArrayList<PointF> = ArrayList(asList.map { it.toPointF() })

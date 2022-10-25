package com.nabla.sdk.docscanner.core.models

import kotlin.math.sqrt

internal data class Point(val x: Float, val y: Float)

internal fun getDistance(p1: Point, p2: Point): Float {
    val dx = p2.x - p1.x
    val dy = p2.y - p1.y
    return sqrt(dx * dx + dy * dy)
}

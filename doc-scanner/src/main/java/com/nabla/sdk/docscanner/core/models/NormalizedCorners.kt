package com.nabla.sdk.docscanner.core.models

internal data class NormalizedCorners(val topLeft: Point, val topRight: Point, val bottomRight: Point, val bottomLeft: Point) {
    val asList by lazy { listOf(topLeft, topRight, bottomRight, bottomLeft) }

    fun getWidthRatio(): Float {
        val topDistance = getDistance(topLeft, topRight)
        val bottomDistance = getDistance(bottomLeft, bottomRight)
        return (topDistance + bottomDistance) / 2f
    }

    fun getHeightRatio(): Float {
        val rightDistance = getDistance(bottomRight, topRight)
        val leftDistance = getDistance(bottomLeft, topLeft)
        return (rightDistance + leftDistance) / 2f
    }

    companion object {
        fun fromList(normalizedCorners: List<Point>): NormalizedCorners {
            check(normalizedCorners.size == 4)
            return NormalizedCorners(
                topLeft = normalizedCorners[0],
                topRight = normalizedCorners[1],
                bottomRight = normalizedCorners[2],
                bottomLeft = normalizedCorners[3],
            )
        }
    }
}

package com.nabla.sdk.messaging.ui.scene.messages

internal data class PlaybackProgress(
    val currentPositionMillis: Long,
    val totalDurationMillis: Long?,
) {
    companion object {
        internal val UNKNOWN = PlaybackProgress(currentPositionMillis = 0, totalDurationMillis = null)
    }
}

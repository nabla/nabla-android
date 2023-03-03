package com.nabla.sdk.core.domain.entity

import androidx.annotation.VisibleForTesting

public sealed class VideoCallRoomStatus {

    /**
     * Room is Open and can be joined using the [url] and [token].
     */
    public data class Open(
        val url: String,
        val token: String,
    ) : VideoCallRoomStatus()

    /**
     * Room is closed, can't be joined.
     */
    public object Closed : VideoCallRoomStatus()

    @VisibleForTesting
    public companion object
}

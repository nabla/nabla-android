package com.nabla.sdk.core.domain.entity

import androidx.annotation.VisibleForTesting

public sealed class VideoCallRoomStatus {

    public data class Open(
        val url: String,
        val token: String,
    ) : VideoCallRoomStatus()

    public object Closed : VideoCallRoomStatus()

    @VisibleForTesting
    public companion object
}

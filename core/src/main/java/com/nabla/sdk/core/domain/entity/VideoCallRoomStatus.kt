package com.nabla.sdk.core.domain.entity

public sealed class VideoCallRoomStatus {
    public data class Open(
        val url: String,
        val token: String,
    ) : VideoCallRoomStatus()

    public object Closed : VideoCallRoomStatus()

    public companion object
}

package com.nabla.sdk.core.domain.entity

public sealed class LivekitRoomStatus {
    public data class Open(
        val url: String,
        val token: String
    ) : LivekitRoomStatus()

    public object Closed : LivekitRoomStatus()
}

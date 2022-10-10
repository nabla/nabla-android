package com.nabla.sdk.core.domain.entity

import com.benasher44.uuid.Uuid

public data class VideoCallRoom(
    val id: Uuid,
    val status: VideoCallRoomStatus,
) {
    public companion object
}

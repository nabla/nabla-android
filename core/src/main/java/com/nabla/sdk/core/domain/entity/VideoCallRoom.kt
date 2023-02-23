package com.nabla.sdk.core.domain.entity

import androidx.annotation.VisibleForTesting
import com.benasher44.uuid.Uuid

public data class VideoCallRoom(
    val id: Uuid,
    val status: VideoCallRoomStatus,
) {

    @VisibleForTesting
    public companion object
}

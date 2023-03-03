package com.nabla.sdk.core.domain.entity

import androidx.annotation.VisibleForTesting
import com.benasher44.uuid.Uuid

/**
 * A video-call room on which a video-call would take place.
 *
 * Can be joined using the information provided in [VideoCallRoomStatus.Open].
 */
public data class VideoCallRoom(
    val id: Uuid,
    val status: VideoCallRoomStatus,
) {

    @VisibleForTesting
    public companion object
}

package com.nabla.sdk.core.domain.entity

import com.benasher44.uuid.Uuid

public data class LivekitRoom(
    val id: Uuid,
    val status: LivekitRoomStatus
)

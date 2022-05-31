package com.nabla.sdk.messaging.ui.scene.messages

import com.nabla.sdk.core.domain.entity.Uri

internal data class OngoingVoiceRecording(
    val targetUri: Uri,
    val secondsSoFar: Int,
    val isPaused: Boolean,
)

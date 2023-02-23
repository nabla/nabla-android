package com.nabla.sdk.core.domain.entity

import androidx.annotation.VisibleForTesting

public data class SystemUser(
    val name: String,
    val avatar: EphemeralUrl?,
) {
    @VisibleForTesting
    public companion object
}

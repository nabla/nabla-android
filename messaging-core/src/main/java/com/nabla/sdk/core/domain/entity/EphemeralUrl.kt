package com.nabla.sdk.core.domain.entity

import kotlinx.datetime.Instant

data class EphemeralUrl(
    val expiresAt: Instant,
    val url: Uri,
) {
    companion object
}

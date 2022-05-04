package com.nabla.sdk.core.domain.entity

import kotlinx.datetime.Instant

/**
 * [Uri] with an expiration date.
 * Make sure you request a new [Uri] if expired.
 */
public data class EphemeralUrl(
    val expiresAt: Instant,
    val url: Uri,
) {
    public companion object
}

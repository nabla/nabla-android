package com.nabla.sdk.core.domain.entity

import com.benasher44.uuid.Uuid

/**
 * @param prefix Honorific name prefix, e.g. 'Dr' or 'Mrs'.
 */
public data class Provider(
    val id: Uuid,
    val avatar: EphemeralUrl?,
    val firstName: String,
    val lastName: String,
    val prefix: String?,
) : MaybeProvider {
    public companion object
}

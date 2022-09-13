package com.nabla.sdk.core.domain.entity

import com.benasher44.uuid.Uuid

/**
 * @param prefix Honorific name prefix, e.g. 'Dr' or 'Mrs'.
 * @param title As in _job title_, typically the speciality. e.g. _Dermatologist_.
 */
public data class Provider(
    val id: Uuid,
    val avatar: EphemeralUrl?,
    val firstName: String,
    val lastName: String,
    val prefix: String?,
    val title: String?,
) : MaybeProvider {
    public companion object
}

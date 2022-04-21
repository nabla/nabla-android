package com.nabla.sdk.core.domain.entity

import com.benasher44.uuid.Uuid

sealed interface User {
    data class Provider(
        val id: Uuid,
        val avatar: EphemeralUrl?,
        val firstName: String,
        val lastName: String,
        val title: String?,
        val prefix: String?,
    ) : User {
        companion object
    }

    data class Patient(
        val id: Uuid,
        val avatar: Attachment?,
        val username: String,
        val labels: List<String>,
    ) : User {
        companion object
    }

    object Unknown : User
}

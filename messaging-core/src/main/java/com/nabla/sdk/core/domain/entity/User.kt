package com.nabla.sdk.core.domain.entity

sealed interface User {
    data class Provider(
        val id: Id,
        val avatar: Attachment?,
        val firstName: String,
        val lastName: String,
        val title: String?,
        val prefix: String?,
    ) : User {
        companion object
    }

    data class Patient(
        val id: Id,
        val avatar: Attachment?,
        val username: String,
        val labels: List<String>,
    ) : User {
        companion object
    }
}

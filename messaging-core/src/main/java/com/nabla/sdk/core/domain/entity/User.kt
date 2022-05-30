package com.nabla.sdk.core.domain.entity

import com.benasher44.uuid.Uuid

public sealed interface User {
    /**
     * @param prefix Honorific name prefix, e.g. 'Dr' or 'Mrs'.
     */
    public data class Provider(
        val id: Uuid,
        val avatar: EphemeralUrl?,
        val firstName: String,
        val lastName: String,
        val prefix: String?,
    ) : User, MaybeProvider {
        public companion object
    }

    public object DeletedProvider : User, MaybeProvider

    public data class Patient(
        val id: Uuid,
        val avatar: Attachment?,
        val username: String,
        val labels: List<String>,
    ) : User {
        public companion object
    }

    public data class System(
        val name: String,
        val avatar: EphemeralUrl?,
    ) : User {
        public companion object
    }

    public object Unknown : User
}

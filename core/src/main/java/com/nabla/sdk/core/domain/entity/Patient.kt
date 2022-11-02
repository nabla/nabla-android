package com.nabla.sdk.core.domain.entity

import com.benasher44.uuid.Uuid

public sealed interface Patient {
    /**
     * Current patient as authenticated from the NablaClient.
     */
    public object Current : Patient

    /**
     * @param displayName: server-made formatted name for the patient from what we know about them, e.g. "$firstName $lastName".
     */
    public data class Other(
        val id: Uuid,
        val displayName: String,
    ) : Patient {
        public companion object
    }

    public companion object
}

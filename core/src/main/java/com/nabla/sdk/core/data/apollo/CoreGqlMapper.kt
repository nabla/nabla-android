package com.nabla.sdk.core.data.apollo

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.EphemeralUrl
import com.nabla.sdk.core.domain.entity.Patient
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.core.domain.entity.VideoCallRoom
import com.nabla.sdk.core.domain.entity.VideoCallRoomStatus
import com.nabla.sdk.graphql.fragment.EphemeralUrlFragment
import com.nabla.sdk.graphql.fragment.LivekitRoomFragment
import com.nabla.sdk.graphql.fragment.LivekitRoomStatusFragment
import com.nabla.sdk.graphql.fragment.PatientFragment
import com.nabla.sdk.graphql.fragment.ProviderFragment

@NablaInternal
public class CoreGqlMapper(private val logger: Logger) {

    public fun mapToVideoCallRoom(fragment: LivekitRoomFragment): VideoCallRoom {
        return VideoCallRoom(
            id = fragment.uuid,
            status = mapToVideoCallRoomStatus(fragment.status.livekitRoomStatusFragment),
        )
    }

    public fun mapToVideoCallRoomStatus(fragment: LivekitRoomStatusFragment): VideoCallRoomStatus {
        fragment.onLivekitRoomOpenStatus?.let {
            return VideoCallRoomStatus.Open(
                url = it.url,
                token = it.token,
            )
        }
        fragment.onLivekitRoomClosedStatus?.let {
            return VideoCallRoomStatus.Closed
        }
        logger.error("Unknown video call room status mapping for $fragment - falling back to Closed")
        return VideoCallRoomStatus.Closed
    }

    public fun mapToProvider(providerFragment: ProviderFragment): Provider {
        val avatarUrl = providerFragment.avatarUrl?.ephemeralUrlFragment?.let { mapToEphemeralUrl(it) }
        return Provider(
            id = providerFragment.id,
            avatar = avatarUrl,
            firstName = providerFragment.firstName,
            lastName = providerFragment.lastName,
            prefix = providerFragment.prefix,
            title = providerFragment.title,
        )
    }

    public fun mapToPatient(patientFragment: PatientFragment): Patient {
        return if (patientFragment.isMe) {
            Patient.Current
        } else {
            Patient.Other(
                id = patientFragment.id,
                displayName = patientFragment.displayName,
            )
        }
    }

    public fun mapToEphemeralUrl(ephemeralUrlFragment: EphemeralUrlFragment): EphemeralUrl {
        return EphemeralUrl(
            expiresAt = ephemeralUrlFragment.expiresAt,
            url = Uri(ephemeralUrlFragment.url),
        )
    }
}

package com.nabla.sdk.core.data.apollo

import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.entity.EphemeralUrl
import com.nabla.sdk.core.domain.entity.LivekitRoom
import com.nabla.sdk.core.domain.entity.LivekitRoomStatus
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.domain.entity.Uri
import com.nabla.sdk.graphql.fragment.EphemeralUrlFragment
import com.nabla.sdk.graphql.fragment.LivekitRoomFragment
import com.nabla.sdk.graphql.fragment.LivekitRoomStatusFragment
import com.nabla.sdk.graphql.fragment.ProviderFragment

@NablaInternal
public class CoreGqlMapper(private val logger: Logger) {

    public fun mapToLivekitRoom(fragment: LivekitRoomFragment): LivekitRoom {
        return LivekitRoom(
            id = fragment.uuid,
            status = mapToLivekitRoomStatus(fragment.status.livekitRoomStatusFragment),
        )
    }

    public fun mapToLivekitRoomStatus(fragment: LivekitRoomStatusFragment): LivekitRoomStatus {
        fragment.onLivekitRoomOpenStatus?.let {
            return LivekitRoomStatus.Open(
                url = it.url,
                token = it.token
            )
        }
        fragment.onLivekitRoomClosedStatus?.let {
            return LivekitRoomStatus.Closed
        }
        logger.error("Unknown livekit room status mapping for $fragment - falling back to Closed")
        return LivekitRoomStatus.Closed
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

    public fun mapToEphemeralUrl(ephemeralUrlFragment: EphemeralUrlFragment): EphemeralUrl {
        return EphemeralUrl(
            expiresAt = ephemeralUrlFragment.expiresAt,
            url = Uri(ephemeralUrlFragment.url)
        )
    }
}

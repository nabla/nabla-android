package com.nabla.sdk.messaging.core.domain.entity

import android.os.Parcelable
import com.benasher44.uuid.Uuid
import com.nabla.sdk.core.annotation.NablaInternal
import com.nabla.sdk.core.domain.entity.InternalException
import kotlinx.datetime.Instant
import kotlinx.parcelize.Parcelize

public sealed interface ConversationId : Parcelable {
    public val clientId: Uuid?
    public val remoteId: Uuid?
    public val stableId: Uuid

    @Parcelize
    public data class Local(
        override val clientId: Uuid,
        override val remoteId: Uuid? = null,
        override val stableId: Uuid = clientId
    ) : ConversationId

    @Parcelize
    public data class Remote(
        override val clientId: Uuid? = null,
        override val remoteId: Uuid,
        override val stableId: Uuid = clientId ?: remoteId
    ) : ConversationId

    @NablaInternal
    public fun requireRemote(): Remote {
        return this as? Remote ?: throw InternalException(
            IllegalStateException("Require remote but $this is ${javaClass.simpleName}")
        )
    }
}

public data class Conversation(
    val id: ConversationId,
    val title: String?,
    val subtitle: String?,
    val inboxPreviewTitle: String,
    val lastMessagePreview: String?,
    val lastModified: Instant,
    val patientUnreadMessageCount: Int,
    val providersInConversation: List<ProviderInConversation>,
) {
    public companion object
}

package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.User
import kotlinx.datetime.Instant

data class ProviderInConversation(
    val provider: User.Provider,
    val isTyping: Boolean,
    val seenUntil: Instant?,
) {
    companion object
}

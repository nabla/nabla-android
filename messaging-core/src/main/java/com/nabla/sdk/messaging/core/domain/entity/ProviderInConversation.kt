package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.User
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.seconds

public data class ProviderInConversation(
    val provider: User.Provider,
    val typingAt: Instant?,
    val seenUntil: Instant?,
    val isTyping: Boolean = isTyping(typingAt),
) {

    internal fun isInactiveAt(): Instant? {
        return Companion.isInactiveAt(typingAt)
    }

    public companion object {
        private val TYPING_TIME_WINDOW = 20.seconds

        private fun isInactiveAt(typingAt: Instant?): Instant? {
            return if (typingAt != null) {
                typingAt + (TYPING_TIME_WINDOW)
            } else {
                null
            }
        }

        private fun isTyping(typingAt: Instant?): Boolean {
            val isInactiveAt = isInactiveAt(typingAt)
            return if (isInactiveAt != null) {
                isInactiveAt > Clock.System.now()
            } else {
                false
            }
        }
    }
}

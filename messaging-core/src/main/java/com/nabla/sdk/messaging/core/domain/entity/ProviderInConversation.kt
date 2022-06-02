package com.nabla.sdk.messaging.core.domain.entity

import androidx.annotation.VisibleForTesting
import com.nabla.sdk.core.domain.entity.Provider
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

public data class ProviderInConversation(
    val provider: Provider,
    val typingAt: Instant?,
    val seenUntil: Instant?,
) {

    public fun isTyping(clock: Clock = Clock.System): Boolean = Companion.isTyping(clock, typingAt)

    internal fun isInactiveAt(): Instant? {
        return Companion.isInactiveAt(typingAt)
    }

    public companion object {
        @VisibleForTesting public val TYPING_TIME_WINDOW: Duration = 20.seconds

        private fun isInactiveAt(typingAt: Instant?): Instant? {
            return if (typingAt != null) {
                typingAt + (TYPING_TIME_WINDOW)
            } else {
                null
            }
        }

        private fun isTyping(clock: Clock, typingAt: Instant?): Boolean {
            val isInactiveAt = isInactiveAt(typingAt)
            return if (isInactiveAt != null) {
                isInactiveAt > clock.now()
            } else {
                false
            }
        }
    }
}

package com.nabla.sdk.messaging.core.data.apollo

import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(ExperimentalCoroutinesApi::class)
internal fun <D> Flow<D>.notifyTypingUpdates(
    clock: Clock = Clock.System,
    context: CoroutineContext = EmptyCoroutineContext,
    providersSelector: (D) -> List<ProviderInConversation>,
): Flow<D> {
    return transformLatest { data ->
        emit(data)
        providersSelector(data)
            .mapNotNull { provider ->
                provider.isInactiveAt()
            }
            .minOrNull()
            ?.let {
                withContext(context) {
                    delay(it - clock.now())
                }
                emit(data)
            }
    }
}

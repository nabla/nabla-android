package com.nabla.sdk.messaging.core.domain.entity

import com.nabla.sdk.core.domain.entity.NablaException
import com.nabla.sdk.core.domain.entity.ServerException

public class MessageNotFoundException(conversationId: ConversationId, localMessageId: MessageId.Local) : NablaException(
    message = "Unable to find message in cache. localMessageId: $localMessageId, conversationId: $conversationId"
)

public class InvalidMessageException(message: String) : NablaException(message = message)

public object MissingConversationIdException :
    NablaException(message = "Missing conversationId parameter, make sure you follow the documentation to integrate ConversationFragment.")

public class InvalidAppThemeException(message: String) :
    NablaException(message = "$message. Please make sure you're using \"Theme.Material3\" on your app theme or at least for activities containing Nabla UI components.")

public open class ProviderNotFoundException internal constructor(cause: ServerException) : NablaException(cause = cause) {
    internal companion object {
        internal const val ERROR_CODE = 20_000
    }
}

public open class ProviderMissingPermissionException internal constructor(cause: ServerException) : NablaException(cause = cause) {
    internal companion object {
        internal const val ERROR_CODE = 10_001
    }
}

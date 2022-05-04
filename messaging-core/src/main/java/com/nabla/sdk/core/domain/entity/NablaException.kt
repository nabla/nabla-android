package com.nabla.sdk.core.domain.entity

import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.MessageId

public sealed class NablaException private constructor(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    internal open val wrapped: Throwable? = null

    public sealed class Configuration(message: String) : NablaException(message = message) {
        public object MissingInitialize : Configuration("Missing SDK initialize. Make sure you call \"NablaCore.initialize\".")
        public object MissingApiKey :
            Configuration("Missing API key. Make sure to add \"com.nabla.sdk.PUBLIC_API_KEY\" in your manifest or call \"NablaCoreBuilder.context(yourContext)\" during initialize call.")

        public object MissingContext : Configuration("Missing context. Make sure you follow the doc to integrate the SDK properly.")
    }

    public class Authentication internal constructor(override val wrapped: Throwable) : NablaException()

    public class Network internal constructor(override val wrapped: Throwable) : NablaException()
    public class Server internal constructor(override val wrapped: Throwable, code: Int, serverMessage: String, requestId: String?) :
        NablaException(message = "Nabla server error. Code: $code, message: $serverMessage, requestId: $requestId")

    public class Internal constructor(override val wrapped: Throwable) : NablaException(message = wrapped.message)

    public class InvalidMessage(message: String) : NablaException(message = message)
    public class MessageNotFound(conversationId: ConversationId, localMessageId: MessageId.Local) : NablaException(
        message = "Unable to find message in cache. localMessageId: $localMessageId, conversationId: $conversationId"
    )

    public object MissingConversationId :
        NablaException(message = "Missing conversationId parameter, make sure you follow the documentation to integrate ConversationFragment.")

    public class InvalidAppTheme(message: String) :
        NablaException(message = "$message. Please make sure you're using \"Theme.Material3\" on your app theme or at least for activities containing Nabla UI components.")

    public class Unknown internal constructor(override val wrapped: Throwable) : NablaException(message = wrapped.message, cause = wrapped)
}

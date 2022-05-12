package com.nabla.sdk.core.domain.entity

import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.MessageId

public sealed class NablaException private constructor(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    public sealed class Configuration(message: String) : NablaException(message = message) {
        public object MissingInitialize : Configuration("Missing SDK initialize. Make sure you call \"NablaCore.initialize\".")
        public object MissingApiKey :
            Configuration("Missing API key. Make sure to add \"com.nabla.sdk.PUBLIC_API_KEY\" in your manifest or call \"NablaCoreBuilder.context(yourContext)\" during initialize call.")

        public object MissingContext : Configuration("Missing context. Make sure you follow the doc to integrate the SDK properly.")
    }

    public sealed class Authentication constructor(cause: Throwable?, message: String) : NablaException(cause = cause, message = message) {
        public object NotAuthenticated : Authentication(cause = null, message = "You must call NablaClient.authenticate before using any authenticated API")
        public class UnableToGetFreshSessionToken(cause: Throwable) : Authentication(cause = cause, message = "Unable to get session token from the SessionTokenProvider")
    }

    public class Network internal constructor(cause: Throwable) : NablaException(cause = cause)
    public class Server internal constructor(cause: Throwable, code: Int, serverMessage: String, requestId: String?) :
        NablaException(cause = cause, message = "Nabla server error. Code: $code, message: $serverMessage, requestId: $requestId")

    public class Internal constructor(cause: Throwable) : NablaException(cause = cause, message = cause.message)

    public class InvalidMessage(message: String) : NablaException(message = message)
    public class MessageNotFound(conversationId: ConversationId, localMessageId: MessageId.Local) : NablaException(
        message = "Unable to find message in cache. localMessageId: $localMessageId, conversationId: $conversationId"
    )

    public object MissingConversationId :
        NablaException(message = "Missing conversationId parameter, make sure you follow the documentation to integrate ConversationFragment.")

    public class InvalidAppTheme(message: String) :
        NablaException(message = "$message. Please make sure you're using \"Theme.Material3\" on your app theme or at least for activities containing Nabla UI components.")

    public class Unknown internal constructor(cause: Throwable) : NablaException(message = cause.message, cause = cause)
}

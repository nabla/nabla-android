package com.nabla.sdk.core.domain.entity

sealed class NablaException private constructor(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    internal open val wrapped: Throwable? = null

    sealed class Configuration(message: String) : NablaException(message = message) {
        object MissingApiKey : Configuration("Missing API key. Make sure to add \"com.nabla.sdk.PUBLIC_API_KEY\" in your manifest.")
        object MissingContext : Configuration("Missing context. Make sure you follow the doc to integrate the SDK properly.")
        object MissingSessionTokenProvider : Configuration("Missing SessionTokenProvider. Make sure you call NablaCore.registerDefaultSessionTokenProvider.")
    }

    class Network internal constructor(override val wrapped: Throwable) : NablaException()
    class Server internal constructor(override val wrapped: Throwable, code: Int, serverMessage: String, requestId: String?) :
        NablaException(message = "Nabla server error. Code: $code, message: $serverMessage, requestId: $requestId")

    class Internal constructor(override val wrapped: Throwable) : NablaException(message = wrapped.message)

    class InvalidMessage(message: String) : NablaException(message = message)
    object MissingConversationId : NablaException(message = "Missing conversationId parameter, make sure you follow the documentation to integrate ConversationFragment.")
    class InvalidAppTheme(message: String) : NablaException(message = "$message. Please make sure you're using \"Theme.Material3\" on your app theme or at least for activities containing Nabla UI components.")

    class Unknown internal constructor(override val wrapped: Throwable) : NablaException(message = wrapped.message, cause = wrapped)
}

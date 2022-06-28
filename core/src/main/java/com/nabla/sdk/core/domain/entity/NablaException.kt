package com.nabla.sdk.core.domain.entity

public open class NablaException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

public sealed class ConfigurationException(message: String) : NablaException(message = message) {
    public object MissingInitialize : ConfigurationException("Missing SDK initialize. Make sure you call \"NablaClient.initialize\".")
    public object MissingApiKey :
        ConfigurationException("Missing API key. Make sure to add \"com.nabla.sdk.PUBLIC_API_KEY\" in your manifest or pass a Context to \"NablaClient.initialize\" in the \"Configuration\" argument.")

    public object MissingContext : ConfigurationException("Missing context. Make sure you follow the doc to integrate the SDK properly.")
}

public sealed class AuthenticationException constructor(cause: Throwable?, message: String) : NablaException(cause = cause, message = message) {
    public object NotAuthenticated :
        AuthenticationException(cause = null, message = "You must call NablaClient.authenticate before using any authenticated API")

    public class UnableToGetFreshSessionToken(cause: Throwable) :
        AuthenticationException(cause = cause, message = "Unable to get session token from the SessionTokenProvider")
}

public class NetworkException internal constructor(cause: Throwable) : NablaException(cause = cause)
public class ServerException internal constructor(cause: Throwable, public val code: Int, serverMessage: String, requestId: String?) :
    NablaException(cause = cause, message = "Nabla server error. Code: $code, message: $serverMessage, requestId: $requestId")

public class InternalException constructor(cause: Throwable) : NablaException(cause = cause, message = cause.message)

public class UnknownException internal constructor(cause: Throwable) : NablaException(message = cause.message, cause = cause)

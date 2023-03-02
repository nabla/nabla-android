package com.nabla.sdk.core.domain.entity

import com.nabla.sdk.core.annotation.NablaInternal

public open class NablaException(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

public abstract class ConfigurationException private constructor(message: String) : NablaException(message = message) {
    public class MissingInitialize : ConfigurationException("Missing SDK initialize. Make sure you call \"NablaClient.initialize\".")
    public class MissingApiKey :
        ConfigurationException("Missing API key. Make sure to add \"com.nabla.sdk.PUBLIC_API_KEY\" in your manifest or pass a Context to \"NablaClient.initialize\" in the \"Configuration\" argument.")

    public class MissingContext : ConfigurationException("Missing context. Make sure you follow the doc to integrate the SDK properly.")

    public class ModuleNotInitialized(moduleName: String) : ConfigurationException("The $moduleName module is not initialized properly. Please make sure you call \"NablaClient.initialize\" with it included as a module.")
}

public abstract class AuthenticationException private constructor(cause: Throwable?, message: String) : NablaException(cause = cause, message = message) {
    public class UserIdNotSet :
        AuthenticationException(cause = null, message = "You must call NablaClient.setCurrentUserOrThrow before using any authenticated API")

    public class UnableToGetFreshSessionToken(cause: Throwable) :
        AuthenticationException(cause = cause, message = "Unable to get session token from the SessionTokenProvider")

    public class AuthorizationDenied(cause: Throwable) :
        AuthenticationException(cause = cause, message = "Authorization denied")

    public class CurrentUserAlreadySet(currentUserId: String, newUserId: String) :
        AuthenticationException(
            cause = null,
            message = "You can't set a new current user when one is already set. " +
                "Current user id: $currentUserId, new user id: $newUserId. " +
                "You should call clearCurrentUser before calling setCurrentUserOrThrow again with another id."
        )
}

public class InvalidAppThemeException(message: String) :
    NablaException(message = "$message. Please make sure you're using \"Theme.Material3\" on your app theme or at least for activities containing Nabla UI components.")

public class NetworkException internal constructor(cause: Throwable) : NablaException(cause = cause)
public class ServerException internal constructor(cause: Throwable, public val code: Int, public val serverMessage: String, requestId: String?) :
    NablaException(cause = cause, message = "Nabla server error. Code: $code, message: $serverMessage, requestId: $requestId")

@NablaInternal
public class InternalException private constructor(cause: Throwable) : NablaException(cause = cause, message = cause.message) {

    @NablaInternal
    public companion object {
        @NablaInternal
        public fun Throwable.asNablaInternal(): InternalException = InternalException(this)

        @NablaInternal
        public fun throwNablaInternalException(message: String): Nothing = throw IllegalStateException(message).asNablaInternal()
    }
}

public class UnknownException internal constructor(cause: Throwable) : NablaException(message = cause.message, cause = cause)

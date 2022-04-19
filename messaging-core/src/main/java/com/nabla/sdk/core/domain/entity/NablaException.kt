package com.nabla.sdk.core.domain.entity

sealed class NablaException private constructor(
    message: String? = null,
    cause: Throwable? = null,
) : Exception(message, cause) {
    internal abstract val wrapped: Throwable

    class Network internal constructor(override val wrapped: Throwable) : NablaException()
    class Internal internal constructor(override val wrapped: Throwable, code: Int) : NablaException(message = "Internal error code: $code")
    class Server internal constructor(override val wrapped: Throwable, code: Int) : NablaException(message = "Nabla server error code: $code")

    class Unknown internal constructor(override val wrapped: Throwable) : NablaException(cause = wrapped)
}

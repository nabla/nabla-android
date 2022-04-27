package com.nabla.sdk.core.data.auth

import java.io.IOException

internal class AuthIoException(override val cause: Throwable) : IOException(cause)

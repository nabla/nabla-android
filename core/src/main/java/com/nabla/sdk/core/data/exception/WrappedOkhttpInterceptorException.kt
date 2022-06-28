package com.nabla.sdk.core.data.exception

import java.io.IOException

internal class WrappedOkhttpInterceptorException(override val cause: Throwable) : IOException(cause)

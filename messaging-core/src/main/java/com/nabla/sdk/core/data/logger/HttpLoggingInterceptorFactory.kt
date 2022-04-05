package com.nabla.sdk.core.data.logger

import com.nabla.sdk.core.domain.boundary.Logger
import okhttp3.logging.HttpLoggingInterceptor

object HttpLoggingInterceptorFactory {
    fun make(logger: Logger): HttpLoggingInterceptor {
        val logging = HttpLoggingInterceptor { message -> logger.debug(message, tag = "http") }
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)
        return logging
    }
}

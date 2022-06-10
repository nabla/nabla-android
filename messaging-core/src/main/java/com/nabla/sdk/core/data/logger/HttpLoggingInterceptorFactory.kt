package com.nabla.sdk.core.data.logger

import com.nabla.sdk.core.data.file.FileService
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.core.domain.boundary.Logger.Companion.HTTP_DOMAIN
import okhttp3.Interceptor
import okhttp3.logging.HttpLoggingInterceptor

internal object HttpLoggingInterceptorFactory {
    fun make(logger: Logger): Interceptor {
        val bodyLogger = HttpLoggingInterceptor { message ->
            logger.debug(domain = HTTP_DOMAIN, message = message)
        }.apply { setLevel(HttpLoggingInterceptor.Level.BODY) }
        val headerLogger = HttpLoggingInterceptor { message ->
            logger.debug(domain = HTTP_DOMAIN, message = message)
        }.apply { setLevel(HttpLoggingInterceptor.Level.HEADERS) }

        return Interceptor { chain ->
            return@Interceptor if (chain.request().url.toString().endsWith(FileService.UPLOAD_FILE_PATH)) {
                headerLogger.intercept(chain)
            } else {
                bodyLogger.intercept(chain)
            }
        }
    }
}

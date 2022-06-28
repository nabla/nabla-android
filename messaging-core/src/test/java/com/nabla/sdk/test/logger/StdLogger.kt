package com.nabla.sdk.test.logger

import com.nabla.sdk.core.domain.boundary.Logger

class StdLogger : Logger {
    override fun debug(message: String, error: Throwable?, domain: String) {
        logOp(message, error, domain)
    }

    override fun info(message: String, error: Throwable?, domain: String?) {
        logOp(message, error, domain)
    }

    override fun warn(message: String, error: Throwable?, domain: String?) {
        logOp(message, error, domain)
    }

    override fun error(message: String, error: Throwable?, domain: String?) {
        logOp(message, error, domain)
    }

    private fun logOp(message: String, error: Throwable?, domain: String?) {
        if (domain != null) {
            println("$domain:$message")
        } else {
            println(message)
        }

        error?.printStackTrace()
    }
}

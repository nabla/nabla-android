package com.nabla.sdk.core.data.logger

import com.nabla.sdk.core.domain.boundary.Logger

class StdLogger : Logger {
    override fun debug(message: String, error: Throwable?, tag: String) {
        logOp(message, error, tag)
    }

    override fun info(message: String, error: Throwable?, tag: String) {
        logOp(message, error, tag)
    }

    override fun warn(message: String, error: Throwable?, tag: String) {
        logOp(message, error, tag)
    }

    override fun error(message: String, error: Throwable?, tag: String) {
        logOp(message, error, tag)
    }

    private fun logOp(message: String, error: Throwable?, tag: String) {
        println("$tag:$message")
        error?.printStackTrace()
    }
}

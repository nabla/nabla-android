package com.nabla.sdk.messaging.core.data.stubs

import android.util.Log
import com.nabla.sdk.core.domain.boundary.Logger

object LoggerImpl : Logger {
    override fun debug(message: String, error: Throwable?, tag: String) {
        Log.d(tag, message, error)
    }

    override fun info(message: String, error: Throwable?, tag: String) {
        Log.i(tag, message, error)
    }

    override fun warn(message: String, error: Throwable?, tag: String) {
        Log.w(tag, message, error)
    }

    override fun error(message: String, error: Throwable?, tag: String) {
        Log.e(tag, message, error)
    }
}

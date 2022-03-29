package com.nabla.sdk.playground

import android.app.Application
import com.nabla.sdk.core.NablaCore
import com.nabla.sdk.core.NablaCoreConfig

class PlaygroundApp: Application() {
    override fun onCreate() {
        super.onCreate()
        NablaCore.instance.setConfig(NablaCoreConfig(isLoggingEnable = true))
        NablaCore.instance.init {
            // Call to authenticate the app on client server
            TODO()
        }
    }
}
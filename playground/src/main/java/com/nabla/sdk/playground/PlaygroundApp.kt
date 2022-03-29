package com.nabla.sdk.playground

import android.app.Application
import com.nabla.sdk.core.NablaCore

class PlaygroundApp: Application() {
    override fun onCreate() {
        super.onCreate()
        NablaCore.instance.init(this) {
            // Call to authenticate the app on client server
            TODO()
        }
    }
}
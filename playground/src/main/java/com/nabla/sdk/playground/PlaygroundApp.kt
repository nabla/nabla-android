package com.nabla.sdk.playground

import android.app.Application
import com.nabla.sdk.messaging.core.Nabla

class PlaygroundApp: Application() {
    override fun onCreate() {
        super.onCreate()
        Nabla.init(this) {
            // Call to authenticate the app on client server
            TODO()
        }
    }
}
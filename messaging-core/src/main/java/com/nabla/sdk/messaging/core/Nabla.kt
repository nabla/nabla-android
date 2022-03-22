package com.nabla.sdk.messaging.core

import android.content.Context
import com.nabla.sdk.auth.domain.boundary.SessionTokenProvider
import com.nabla.sdk.messaging.core.injection.MessagingContainer

class Nabla(context: Context, sessionTokenProvider: SessionTokenProvider) {
    val messagingContainer = MessagingContainer(context, sessionTokenProvider)

    companion object {
        @Volatile private var instance: Nabla? = null

        fun init(applicationContext: Context, sessionTokenProvider: SessionTokenProvider) {
            initSingleton(applicationContext, sessionTokenProvider)
        }

        fun getInstance(): Nabla {
            return requireNotNull(instance) {
                "Nabla SDK not initialized"
            }
        }

        private fun initSingleton(applicationContext: Context, sessionTokenProvider: SessionTokenProvider) {
            synchronized(this) {
                instance ?: Nabla(applicationContext, sessionTokenProvider).also { instance = it }
            }
        }
    }
}

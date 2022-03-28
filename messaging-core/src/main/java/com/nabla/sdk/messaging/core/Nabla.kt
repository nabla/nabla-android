package com.nabla.sdk.messaging.core

import android.content.Context
import com.nabla.sdk.auth.domain.boundary.SessionTokenProvider
import com.nabla.sdk.core.domain.entity.PatientId
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.injection.MessagingContainer
import kotlinx.coroutines.flow.Flow

class Nabla(context: Context, sessionTokenProvider: SessionTokenProvider) {

    val messagingContainer = MessagingContainer(context, sessionTokenProvider)

    suspend fun login(patientId: PatientId): Result<Unit> {
        return messagingContainer.loginInteractor().invoke(patientId)
    }

    fun getConversations(): Flow<List<Conversation>> {
        return messagingContainer.conversationRepository.getConversations()
    }

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

package com.nabla.health.sdk.messaging.ui.scene

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nabla.health.sdk.messaging.core.domain.boundary.ConversationRepository
import com.nabla.health.sdk.messaging.core.kotlin.runCatchingCancellable
import kotlinx.coroutines.launch

class ConversationListViewModel(
    private val conversationRepository: ConversationRepository
): ViewModel() {

    fun createConversation() {
        viewModelScope.launch {
            runCatchingCancellable {
                conversationRepository.createConversation()
            }
        }
    }
}

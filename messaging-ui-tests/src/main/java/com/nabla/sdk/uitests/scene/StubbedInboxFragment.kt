package com.nabla.sdk.uitests.scene

import android.view.ViewGroup
import com.nabla.sdk.messaging.core.NablaMessagingClient
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.ui.scene.conversations.InboxFragment
import com.nabla.sdk.messaging.ui.scene.messages.ConversationFragment

class StubbedInboxFragment : InboxFragment() {
    override val messagingClient: NablaMessagingClient
        get() = nablaMessagingClient

    override fun openConversationScreen(conversationId: ConversationId) {
        val containerId = (view?.parent as ViewGroup).id

        parentFragmentManager.beginTransaction()
            .replace(
                containerId,
                ConversationFragment.newInstance(conversationId) {
                    setFragment(StubbedConversationFragment())
                },
            )
            .addToBackStack(null)
            .commit()
    }
}

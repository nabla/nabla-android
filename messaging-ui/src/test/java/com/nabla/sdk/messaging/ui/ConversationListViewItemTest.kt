package com.nabla.sdk.messaging.ui

import androidx.constraintlayout.widget.ConstraintLayout
import com.nabla.sdk.messaging.ui.databinding.NablaConversationListViewItemBinding
import org.junit.Rule
import org.junit.Test

internal class ConversationListViewItemTest {

    @get:Rule
    val paparazzi = defaultPaparazzi()

    @Test
    fun `Display default state`() {
        val binding = NablaConversationListViewItemBinding.bind(
            paparazzi.inflateWithWithNablaMessagingThemeOverlays<ConstraintLayout>(R.layout.nabla_conversation_list_view_item)
        )

        binding.lastMessageDate.text = "last message date"
        binding.conversationAvatarView.displayUnicolorPlaceholder()
        binding.conversationInboxSubtitle.text = "conversation inbox subtitle"
        binding.conversationInboxTitle.text = "conversation inbox title"

        paparazzi.snapshot(binding.root)
    }
}

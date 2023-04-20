package com.nabla.sdk.messaging.ui.conversations

import androidx.constraintlayout.widget.ConstraintLayout
import com.nabla.sdk.messaging.ui.R
import com.nabla.sdk.messaging.ui.databinding.NablaConversationListViewItemBinding
import com.nabla.sdk.messaging.ui.inflateWithWithNablaMessagingThemeOverlays
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.DayNightPaparazziRule
import org.junit.Rule
import org.junit.Test

internal class ConversationListViewItemTest : BaseCoroutineTest() {
    @get:Rule
    val paparazzi = DayNightPaparazziRule()

    @Test
    fun `Display default state`() {
        paparazzi.snapshotDayNightMultiDevices { (_, layoutInflater) ->
            val binding = NablaConversationListViewItemBinding.bind(
                layoutInflater.inflateWithWithNablaMessagingThemeOverlays<ConstraintLayout>(R.layout.nabla_conversation_list_view_item),
            )

            binding.lastMessageDate.text = "last message date"
            binding.conversationAvatarView.displayUnicolorPlaceholder()
            binding.conversationInboxSubtitle.text = "conversation inbox subtitle"
            binding.conversationInboxTitle.text = "conversation inbox title"

            return@snapshotDayNightMultiDevices binding.root
        }
    }
}

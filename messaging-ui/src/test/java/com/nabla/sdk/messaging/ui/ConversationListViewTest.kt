package com.nabla.sdk.messaging.ui

import androidx.core.view.isVisible
import com.android.ide.common.rendering.api.SessionParams
import com.nabla.sdk.core.data.stubs.fake
import com.nabla.sdk.core.domain.entity.Provider
import com.nabla.sdk.core.ui.helpers.dpToPx
import com.nabla.sdk.messaging.core.data.stubs.fake
import com.nabla.sdk.messaging.core.domain.entity.Conversation
import com.nabla.sdk.messaging.core.domain.entity.ProviderInConversation
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListAdapter
import com.nabla.sdk.messaging.ui.scene.conversations.ConversationListView
import com.nabla.sdk.messaging.ui.scene.conversations.DefaultOffsetsItemDecoration
import com.nabla.sdk.messaging.ui.scene.conversations.toUiModel
import com.nabla.sdk.tests.common.BaseCoroutineTest
import com.nabla.sdk.tests.common.DayNightPaparazziRule
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.junit.Rule
import org.junit.Test
import java.util.UUID

internal class ConversationListViewTest : BaseCoroutineTest() {

    @get:Rule
    val paparazzi = DayNightPaparazziRule(
        renderingMode = SessionParams.RenderingMode.V_SCROLL,
    )

    @Test
    fun `Display default state`() {
        paparazzi.snapshotDayNightMultiDevices { (context, _) ->
            val view = ConversationListView(context)
            val adapter = ConversationListAdapter {}
            view.recyclerView.adapter = adapter
            view.recyclerView.addItemDecoration(
                DefaultOffsetsItemDecoration(
                    spacingBetweenItemsPx = view.context.dpToPx(12),
                    firstItemTopPaddingPx = view.context.dpToPx(12),
                    lastItemBottomPaddingPx = view.context.dpToPx(92),
                )
            )
            view.recyclerView.isVisible = true
            view.errorView.root.isVisible = false
            val providerInConversation = ProviderInConversation.fake(
                provider = Provider.fake(
                    id = UUID.fromString("6cf6bdbf-a0ea-437c-b668-2236e02f1a07"),
                    lastName = "Cayol",
                    avatar = null,
                )
            )
            adapter.submitList(
                List(20) { index ->
                    Conversation.fake(
                        inboxPreviewTitle = "Conversation $index",
                        lastMessagePreview = "last message preview $index",
                        providersInConversation = listOf(providerInConversation),
                        patientUnreadMessageCount = index % 2,
                        // Use distant past to make sure the date is displayed as full date
                        lastModified = LocalDate(2020, 1, 1).atStartOfDayIn(TimeZone.UTC)
                    ).toUiModel()
                }
            )

            return@snapshotDayNightMultiDevices view
        }
    }
}

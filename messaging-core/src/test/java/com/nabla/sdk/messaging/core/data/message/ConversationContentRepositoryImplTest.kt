package com.nabla.sdk.messaging.core.data.message

import app.cash.turbine.test
import com.benasher44.uuid.uuid4
import com.nabla.sdk.messaging.core.data.stubs.fake
import com.nabla.sdk.messaging.core.data.stubs.fakeImage
import com.nabla.sdk.messaging.core.data.stubs.fakeProviderJoined
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivity
import com.nabla.sdk.messaging.core.domain.entity.ConversationItems
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.messaging.core.domain.entity.toConversationId
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ConversationContentRepositoryImplTest {

    @Test
    fun `combineGqlAndLocalInfo is combining data sources`() = runTest {
        val conversationId = uuid4().toConversationId()

        val localMessageToKeep = Message.Text.Companion.fake(
            id = MessageId.Local(
                clientId = uuid4()
            )
        )
        val localMessageToReplace = Message.Text.Companion.fake(
            id = MessageId.Local(
                clientId = uuid4()
            )
        )
        val localMessageToMergeMediaSource = FileSource.Local.fakeImage()
        val localMessageToMerge = Message.Media.Image.Companion.fake(
            id = MessageId.Local(
                clientId = uuid4()
            ),
            mediaSource = localMessageToMergeMediaSource
        )
        val localMessages = listOf(localMessageToKeep, localMessageToReplace, localMessageToMerge)
        val gqlMessageToKeep = Message.Text.Companion.fake(
            id = MessageId.Remote(
                clientId = uuid4(),
                remoteId = uuid4()
            )
        )
        val gqlMessageReplacingLocalMessage = Message.Text.Companion.fake(
            id = MessageId.Remote(
                clientId = localMessageToReplace.id.clientId,
                remoteId = uuid4()
            )
        )
        val gqlMessageToMergeDataSource = FileSource.Uploaded.fakeImage()
        val gqlMessageToMerge = Message.Media.Image.Companion.fake(
            id = MessageId.Remote(
                clientId = localMessageToMerge.id.clientId,
                remoteId = uuid4()
            ),
            mediaSource = gqlMessageToMergeDataSource
        )
        val gqlConversationActivity = ConversationActivity.Companion.fakeProviderJoined()
        val conversationItems = listOf(
            gqlMessageToKeep,
            gqlMessageReplacingLocalMessage,
            gqlMessageToMerge,
            gqlConversationActivity
        )
        val paginatedConversationItems = PaginatedConversationItems(
            conversationItems = ConversationItems(
                conversationId = conversationId,
                items = conversationItems
            ),
            hasMore = false
        )

        val localMessageDataSource = mockk<LocalMessageDataSource> {
            every { watchLocalMessages(conversationId) } returns flowOf(localMessages)
        }
        val gqlConversationContentDataSource = mockk<GqlConversationContentDataSource> {
            every { watchConversationItems(conversationId) } returns flowOf(paginatedConversationItems)
        }
        val mergedMessage = gqlMessageToMerge.modify(
            mediaSource = FileSource.Uploaded(
                fileLocal = localMessageToMergeMediaSource.fileLocal,
                fileUpload = gqlMessageToMergeDataSource.fileUpload
            )
        )
        val repo = ConversationContentRepositoryImpl(
            mockk(),
            localMessageDataSource,
            gqlConversationContentDataSource,
            mockk(),
            mockk(),
            mockk()
        )
        repo.watchConversationItems(conversationId).test {
            val item = awaitItem()
            item.conversationItems.items.apply {
                assertTrue(contains(localMessageToKeep))
                assertFalse(contains(localMessageToReplace))
                assertFalse(contains(localMessageToMerge))
                assertTrue(contains(gqlMessageToKeep))
                assertTrue(contains(gqlMessageReplacingLocalMessage))
                assertTrue(contains(mergedMessage))
                assertTrue(contains(gqlConversationActivity))
                assertTrue(size == 5)
                val orderedCombinedMessages = sortedByDescending { it.createdAt }
                assertEquals(orderedCombinedMessages, this)
            }
            awaitComplete()
        }
    }
}

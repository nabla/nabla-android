package com.nabla.sdk.messaging.core.data.message

import app.cash.turbine.test
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.domain.entity.PaginatedList
import com.nabla.sdk.core.domain.entity.RefreshingState
import com.nabla.sdk.core.domain.entity.Response
import com.nabla.sdk.messaging.core.data.stubs.fake
import com.nabla.sdk.messaging.core.data.stubs.fakeImage
import com.nabla.sdk.messaging.core.data.stubs.fakeProviderJoined
import com.nabla.sdk.messaging.core.domain.entity.ConversationActivity
import com.nabla.sdk.messaging.core.domain.entity.ConversationId
import com.nabla.sdk.messaging.core.domain.entity.FileSource
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.MessageId
import com.nabla.sdk.tests.common.BaseCoroutineTest
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationContentRepositoryImplTest : BaseCoroutineTest() {

    @Test
    fun `combineGqlAndLocalInfo is combining data sources`() = runTest {
        val conversationId = ConversationId.Remote(remoteId = uuid4())

        val localMessageToKeep = Message.Text.Companion.fake(
            id = MessageId.Local(
                clientId = uuid4(),
            ),
        )
        val localMessageToReplace = Message.Text.Companion.fake(
            id = MessageId.Local(
                clientId = uuid4(),
            ),
        )
        val localMessageToMergeMediaSource = FileSource.Local.fakeImage()
        val localMessageToMerge = Message.Media.Image.Companion.fake(
            id = MessageId.Local(
                clientId = uuid4(),
            ),
            mediaSource = localMessageToMergeMediaSource,
        )
        val localMessages = listOf(localMessageToKeep, localMessageToReplace, localMessageToMerge)
        val gqlMessageToKeep = Message.Text.Companion.fake(
            id = MessageId.Remote(
                clientId = uuid4(),
                remoteId = uuid4(),
            ),
        )
        val gqlMessageReplacingLocalMessage = Message.Text.Companion.fake(
            id = MessageId.Remote(
                clientId = localMessageToReplace.id.clientId,
                remoteId = uuid4(),
            ),
        )
        val gqlMessageToMergeDataSource = FileSource.Uploaded.fakeImage()
        val gqlMessageToMerge = Message.Media.Image.Companion.fake(
            id = MessageId.Remote(
                clientId = localMessageToMerge.id.clientId,
                remoteId = uuid4(),
            ),
            mediaSource = gqlMessageToMergeDataSource,
        )
        val gqlConversationActivity = ConversationActivity.Companion.fakeProviderJoined()
        val conversationItems = listOf(
            gqlMessageToKeep,
            gqlMessageReplacingLocalMessage,
            gqlMessageToMerge,
            gqlConversationActivity,
        )
        val paginatedConversationItems = PaginatedList(
            items = conversationItems,
            hasMore = false,
        )

        val localMessageDataSource = mockk<LocalMessageDataSource> {
            every { watchLocalMessages(conversationId) } returns flowOf(localMessages)
        }
        val gqlConversationContentDataSource = mockk<GqlConversationContentDataSource> {
            every { watchConversationItems(conversationId) } returns flowOf(
                Response(
                    isDataFresh = true,
                    refreshingState = RefreshingState.Refreshed,
                    data = paginatedConversationItems,
                ),
            )
        }

        val mergedMessage = gqlMessageToMerge.modify(
            mediaSource = FileSource.Uploaded(
                fileLocal = localMessageToMergeMediaSource.fileLocal,
                fileUpload = gqlMessageToMergeDataSource.fileUpload,
            ),
        )
        val repo = ConversationContentRepositoryImpl(
            mockk(),
            localMessageDataSource,
            mockk(),
            gqlConversationContentDataSource,
            mockk(),
            mockk(),
            isVideoCallModuleActive = false,
        )
        repo.watchConversationItems(conversationId).test {
            val itemResponse = awaitItem()
            itemResponse.data.items.apply {
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

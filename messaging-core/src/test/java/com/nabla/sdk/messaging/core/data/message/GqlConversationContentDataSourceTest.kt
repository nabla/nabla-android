package com.nabla.sdk.messaging.core.data.message

import app.cash.turbine.test
import com.apollographql.apollo3.annotations.ApolloExperimental
import com.apollographql.apollo3.api.Optional
import com.apollographql.apollo3.cache.normalized.api.MemoryCacheFactory
import com.apollographql.apollo3.network.NetworkTransport
import com.benasher44.uuid.uuid4
import com.nabla.sdk.core.data.apollo.ApolloFactory
import com.nabla.sdk.core.data.stubs.apollo.FlowTestNetworkTransport
import com.nabla.sdk.core.domain.boundary.Logger
import com.nabla.sdk.graphql.ConversationEventsSubscription
import com.nabla.sdk.graphql.ConversationItemsQuery
import com.nabla.sdk.graphql.type.OpaqueCursorPage
import com.nabla.sdk.messaging.core.data.apollo.GqlMapper
import com.nabla.sdk.messaging.core.data.stubs.GqlData
import com.nabla.sdk.messaging.core.domain.entity.Message
import com.nabla.sdk.messaging.core.domain.entity.toConversationId
import io.mockk.mockk
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@ApolloExperimental
@OptIn(ExperimentalCoroutinesApi::class)
class GqlConversationContentDataSourceTest {

    @Test
    fun `created message event is notified to conversations watcher`() = runTest {
        val (testNetworkTransport, job, gqlConversationContentDataSource) = setupTest(this)

        val conversationId = uuid4().toConversationId()
        testNetworkTransport.register(
            gqlConversationContentDataSource.firstItemsPageQuery(conversationId),
            GqlData.ConversationItems.empty(conversationId)
        )
        val gqlEventEmitter = MutableStateFlow<ConversationEventsSubscription.Data?>(null)
        testNetworkTransport.register(
            ConversationEventsSubscription(conversationId.value),
            gqlEventEmitter.filterNotNull()
        )
        gqlConversationContentDataSource.watchConversationItems(conversationId).test {
            var paginatedConversations = awaitItem()
            assertTrue(paginatedConversations.conversationItems.items.isEmpty())
            gqlEventEmitter.value =
                GqlData.ConversationEvents.MessageCreated.patientTextMessage(conversationId)
            paginatedConversations = awaitItem()
            assertTrue(paginatedConversations.conversationItems.items.size == 1)
        }
        job.cancel()
    }

    @Test
    fun `load more message is notified to conversation watcher`() = runTest {
        val (testNetworkTransport, job, gqlConversationContentDataSource) = setupTest(this)

        val conversationId = uuid4().toConversationId()
        val cursor = uuid4().toString()
        testNetworkTransport.register(
            gqlConversationContentDataSource.firstItemsPageQuery(conversationId),
            GqlData.ConversationItems.single(conversationId) {
                nextCursor = cursor
                hasMore = true
            }
        )
        testNetworkTransport.register(
            ConversationItemsQuery(
                conversationId.value,
                OpaqueCursorPage(cursor = Optional.Present(cursor))
            ),
            GqlData.ConversationItems.single(conversationId) {
                nextCursor = null
                hasMore = false
            }
        )
        testNetworkTransport.register(
            ConversationEventsSubscription(conversationId.value),
            emptyFlow()
        )
        gqlConversationContentDataSource.watchConversationItems(conversationId).test {
            var paginatedItems = awaitItem()
            assertTrue(paginatedItems.conversationItems.items.size == 1)
            launch {
                gqlConversationContentDataSource.loadMoreConversationItemsInCache(
                    conversationId
                )
            }
            paginatedItems = awaitItem()
            assertTrue(paginatedItems.conversationItems.items.size == 2)
        }
        job.cancel()
    }

    @Test
    fun `deleted message is notified to conversation watcher`() = runTest {
        val (testNetworkTransport, job, gqlConversationContentDataSource) = setupTest(this)

        val conversationId = uuid4().toConversationId()
        testNetworkTransport.register(
            gqlConversationContentDataSource.firstItemsPageQuery(conversationId),
            GqlData.ConversationItems.single(conversationId) {
                nextCursor = uuid4().toString()
                hasMore = false
            }
        )
        val gqlEventEmitter = MutableStateFlow<ConversationEventsSubscription.Data?>(null)
        testNetworkTransport.register(
            ConversationEventsSubscription(conversationId.value),
            gqlEventEmitter.filterNotNull()
        )
        gqlConversationContentDataSource.watchConversationItems(conversationId).test {
            var paginatedConversationItems = awaitItem()
            var message = paginatedConversationItems.conversationItems.items.first()
            assertIs<Message.Text>(message)

            gqlEventEmitter.value = GqlData.ConversationEvents.MessageDeleted.deletedPatientMessage(conversationId, message.id)

            paginatedConversationItems = awaitItem()
            message = paginatedConversationItems.conversationItems.items.first()

            assertIs<Message.Deleted>(message)
        }
        job.cancel()
    }

    @Test
    fun `deleted message is notified in replier message conversation watcher`() = runTest {
        val (testNetworkTransport, job, gqlConversationContentDataSource) = setupTest(this)

        val conversationId = uuid4().toConversationId()
        val replyToMessageId = uuid4()
        testNetworkTransport.register(
            gqlConversationContentDataSource.firstItemsPageQuery(conversationId),
            GqlData.ConversationItems.single(conversationId) {
                nextCursor = uuid4().toString()
                hasMore = false
                val firstMessage = messageData {
                    id = replyToMessageId.toString()
                    messageContent = textMessageContentMessageContent { }
                    replyTo = null
                }
                data = listOf(
                    firstMessage,
                    messageData {
                        id = uuid4().toString()
                        messageContent = textMessageContentMessageContent { }
                        replyTo = firstMessage
                    },
                )
            }
        )
        val gqlEventEmitter = MutableStateFlow<ConversationEventsSubscription.Data?>(null)
        testNetworkTransport.register(
            ConversationEventsSubscription(conversationId.value),
            gqlEventEmitter.filterNotNull()
        )
        gqlConversationContentDataSource.watchConversationItems(conversationId).test {
            var paginatedConversationItems = awaitItem()
            var (firstMessage, secondMessage) = paginatedConversationItems.conversationItems.items
            assertIs<Message.Text>(firstMessage)
            assertIs<Message.Text>(secondMessage)
            assertEquals(firstMessage.id.remoteId, replyToMessageId)
            assertEquals(secondMessage.replyTo?.id?.remoteId, replyToMessageId)

            gqlEventEmitter.value = GqlData.ConversationEvents.MessageDeleted.deletedPatientMessage(conversationId, firstMessage.id)

            paginatedConversationItems = awaitItem()
            firstMessage = paginatedConversationItems.conversationItems.items[0]
            secondMessage = paginatedConversationItems.conversationItems.items[1]

            assertIs<Message.Deleted>(firstMessage)
            assertIs<Message.Text>(secondMessage)
            assertIs<Message.Deleted>(secondMessage.replyTo)
        }
        job.cancel()
    }

    @Test
    fun `receiving a duplicate new timeline item is idempotent`() = runTest {
        val (testNetworkTransport, job, gqlConversationContentDataSource) = setupTest(this)

        val conversationId = uuid4().toConversationId()
        testNetworkTransport.register(
            gqlConversationContentDataSource.firstItemsPageQuery(conversationId),
            GqlData.ConversationItems.single(conversationId) {
                nextCursor = uuid4().toString()
                hasMore = false
                data = emptyList()
            }
        )
        val gqlEventEmitter = MutableSharedFlow<ConversationEventsSubscription.Data>()

        testNetworkTransport.register(
            ConversationEventsSubscription(conversationId.value),
            gqlEventEmitter,
        )
        gqlConversationContentDataSource.watchConversationItems(conversationId).test {
            var items = awaitItem().conversationItems.items
            assertEquals(0, items.size)

            val event = GqlData.ConversationEvents.Activity.existingProviderJoinedActivity(conversationId)

            gqlEventEmitter.emit(event)

            items = awaitItem().conversationItems.items
            assertEquals(1, items.size)

            gqlEventEmitter.emit(event) // should be ignored by idempotency

            gqlEventEmitter.emit(GqlData.ConversationEvents.MessageCreated.patientTextMessage(conversationId))

            items = awaitItem().conversationItems.items

            assertEquals(2, items.size)
            assertIs<Message.Text>(items[0])
            assertIs<com.nabla.sdk.messaging.core.domain.entity.ConversationActivity>(items[1])
        }
        job.cancel()
    }

    private fun setupTest(testScope: TestScope): Triple<FlowTestNetworkTransport, CompletableJob, GqlConversationContentDataSource> {
        val testNetworkTransport = FlowTestNetworkTransport()
        val job = Job()
        val gqlConversationContentDataSource =
            createTestableGqlMessageDataSource(testNetworkTransport, testScope + job)
        return Triple(testNetworkTransport, job, gqlConversationContentDataSource)
    }

    private fun createTestableGqlMessageDataSource(
        testNetworkTransport: NetworkTransport,
        scope: CoroutineScope,
    ): GqlConversationContentDataSource {
        val apolloClient = ApolloFactory.configureBuilder(
            normalizedCacheFactory = MemoryCacheFactory()
        ).networkTransport(testNetworkTransport)
            .build()
        val logger: Logger = mockk(relaxed = true)
        return GqlConversationContentDataSource(
            logger = logger,
            coroutineScope = scope,
            apolloClient = apolloClient,
            mapper = GqlMapper(logger)
        )
    }
}
